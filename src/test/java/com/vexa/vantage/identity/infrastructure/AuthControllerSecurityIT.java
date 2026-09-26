package com.vexa.vantage.identity.infrastructure;

import com.vexa.vantage.identity.domain.TenantRole;
import com.vexa.vantage.identity.infrastructure.persistence.AppUserJpaEntity;
import com.vexa.vantage.identity.infrastructure.persistence.AppUserSpringDataRepository;
import com.vexa.vantage.identity.infrastructure.persistence.TenantJpaEntity;
import com.vexa.vantage.identity.infrastructure.persistence.TenantJpaRepository;
import com.vexa.vantage.support.PostgresTestContainerConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración (con PostgreSQL real vía Testcontainers) de la
 * configuración de seguridad del contexto delimitado de identidad:
 * {@link JwtAuthenticationFilter} y {@link SecurityConfig}.
 *
 * <p>El "endpoint protegido" usado como sonda es
 * {@code PATCH /api/v1/users/{id}/disabled} (de {@code UserAdminController},
 * fase 3.32): a los fines de esta prueba solo importa que Spring Security lo
 * rechace en la capa de filtros (antes de llegar al controlador) cuando el
 * token es ausente, inválido o de un tipo incorrecto; no importa si el
 * controlador ya existe.
 *
 * <p>También verifica la decisión de CSRF ya tomada en fases anteriores: las
 * rutas {@code /api/v1/auth/refresh} y {@code /api/v1/auth/logout} funcionan
 * sin token CSRF a propósito, porque dependen de la cookie
 * {@code refresh_token} (enviada automáticamente por el navegador) y no de un
 * bearer token explícito.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class AuthControllerSecurityIT extends PostgresTestContainerConfig {

    private static final String PROTECTED_ENDPOINT = "/api/v1/users/1/disabled";
    private static final String RAW_PASSWORD = "S3cret-Pass!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantJpaRepository tenantRepository;

    @Autowired
    private AppUserSpringDataRepository appUserRepository;

    @Value("${application.security.jwt.secret-key}")
    private String jwtSecret;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    private void seedTenantAndUser(String email) {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        String passwordHash = new BCryptPasswordEncoder().encode(RAW_PASSWORD);
        appUserRepository.save(new AppUserJpaEntity(tenantId, email, passwordHash, "Test User", TenantRole.MEMBER));
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("refresh_token").getValue();
    }

    private String buildExpiredAccessToken() {
        Instant past = Instant.now().minus(Duration.ofDays(1));
        return Jwts.builder()
                .subject("nobody@acme.com")
                .claim("uid", "user-1")
                .claim("tid", "tenant-1")
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    private String buildBadlySignedAccessToken() {
        byte[] rawKey = new byte[32];
        new SecureRandom().nextBytes(rawKey);
        SecretKey wrongKey = Keys.hmacShaKeyFor(rawKey);
        return Jwts.builder()
                .subject("nobody@acme.com")
                .claim("uid", "user-1")
                .claim("tid", "tenant-1")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(15))))
                .signWith(wrongKey, Jwts.SIG.HS256)
                .compact();
    }

    @Test
    void requestWithoutATokenIsRejectedWith401() throws Exception {
        mockMvc.perform(patch(PROTECTED_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabled\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithAnExpiredAccessTokenIsRejectedWith401() throws Exception {
        mockMvc.perform(patch(PROTECTED_ENDPOINT)
                        .header("Authorization", "Bearer " + buildExpiredAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabled\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithABadlySignedTokenIsRejectedWith401() throws Exception {
        mockMvc.perform(patch(PROTECTED_ENDPOINT)
                        .header("Authorization", "Bearer " + buildBadlySignedAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabled\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aRefreshTokenPresentedAsABearerAccessTokenIsRejectedWith401() throws Exception {
        seedTenantAndUser("bearer-refresh@acme.com");
        String refreshToken = login("bearer-refresh@acme.com");

        mockMvc.perform(patch(PROTECTED_ENDPOINT)
                        .header("Authorization", "Bearer " + refreshToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabled\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshEndpointWorksWithoutACsrfToken() throws Exception {
        seedTenantAndUser("csrf-refresh@acme.com");
        String refreshToken = login("csrf-refresh@acme.com");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk());
    }

    @Test
    void logoutEndpointWorksWithoutACsrfToken() throws Exception {
        seedTenantAndUser("csrf-logout@acme.com");
        String refreshToken = login("csrf-logout@acme.com");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isNoContent());
    }
}

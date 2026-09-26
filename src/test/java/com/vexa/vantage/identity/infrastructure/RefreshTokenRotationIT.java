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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración (con PostgreSQL real vía Testcontainers y JWTs reales
 * firmados por {@link com.vexa.vantage.identity.application.JwtService} y
 * {@link com.vexa.vantage.identity.application.RefreshTokenService}) del
 * flujo completo de rotación de refresh tokens a través de
 * {@link AuthController}.
 *
 * <p>Cubre dos escenarios de la Fase 3:
 * <ul>
 *     <li>Un refresh exitoso rota el {@code jti} de la cookie
 *     {@code refresh_token} (el token nuevo tiene una identidad distinta del
 *     token presentado).</li>
 *     <li>Reutilizar un {@code jti} ya superado (revocado por una rotación
 *     previa) revoca TODOS los refresh tokens vigentes de ese usuario: tanto
 *     el intento de reuso como cualquier intento posterior con otro token
 *     todavía "vigente" de ese mismo usuario deben fallar con 401.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class RefreshTokenRotationIT extends PostgresTestContainerConfig {

    private static final String RAW_PASSWORD = "S3cret-Pass!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantJpaRepository tenantRepository;

    @Autowired
    private AppUserSpringDataRepository appUserRepository;

    @Value("${application.security.jwt.secret-key}")
    private String jwtSecret;

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

    private MvcResult attemptRefresh(String refreshTokenCookieValue) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", refreshTokenCookieValue)))
                .andReturn();
    }

    private String jtiOf(String refreshToken) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(refreshToken).getPayload().getId();
    }

    @Test
    void refreshingRotatesTheJtiOfTheRefreshTokenCookie() throws Exception {
        seedTenantAndUser("rotate@acme.com");
        String originalRefreshToken = login("rotate@acme.com");

        MvcResult refreshResult = attemptRefresh(originalRefreshToken);
        assertThat(refreshResult.getResponse().getStatus()).isEqualTo(200);

        String rotatedRefreshToken = refreshResult.getResponse().getCookie("refresh_token").getValue();
        assertThat(jtiOf(rotatedRefreshToken)).isNotEqualTo(jtiOf(originalRefreshToken));
    }

    @Test
    void reusingASupersededJtiRevokesAllRefreshTokensOfThatUser() throws Exception {
        seedTenantAndUser("reuse@acme.com");
        String originalRefreshToken = login("reuse@acme.com");

        MvcResult firstRefresh = attemptRefresh(originalRefreshToken);
        assertThat(firstRefresh.getResponse().getStatus()).isEqualTo(200);
        String rotatedRefreshToken = firstRefresh.getResponse().getCookie("refresh_token").getValue();

        // Reusar el token original (ya superado por la rotación anterior) debe
        // ser rechazado y disparar la revocación de TODOS los tokens del usuario.
        MvcResult reuseAttempt = attemptRefresh(originalRefreshToken);
        assertThat(reuseAttempt.getResponse().getStatus()).isEqualTo(401);

        // El token rotado, aunque todavía no había sido presentado, también
        // debe quedar revocado como consecuencia de la detección de reuso.
        MvcResult attemptWithRotatedToken = attemptRefresh(rotatedRefreshToken);
        assertThat(attemptWithRotatedToken.getResponse().getStatus()).isEqualTo(401);
    }
}

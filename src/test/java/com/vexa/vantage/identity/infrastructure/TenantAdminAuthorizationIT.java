package com.vexa.vantage.identity.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vexa.vantage.identity.domain.TenantRole;
import com.vexa.vantage.identity.infrastructure.persistence.AppUserJpaEntity;
import com.vexa.vantage.identity.infrastructure.persistence.AppUserSpringDataRepository;
import com.vexa.vantage.identity.infrastructure.persistence.TenantJpaEntity;
import com.vexa.vantage.identity.infrastructure.persistence.TenantJpaRepository;
import com.vexa.vantage.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración (con PostgreSQL real vía Testcontainers) de la
 * autorización de {@link UserAdminController}: SOLO un usuario con
 * {@code tenant_role=TENANT_ADMIN} puede deshabilitar/reactivar usuarios o
 * cambiar su {@code tenant_role}; un {@code MEMBER} recibe 403 Forbidden.
 *
 * <p>El {@link ObjectMapper} usado aquí para leer el cuerpo JSON de la
 * respuesta de login se instancia localmente en lugar de inyectarse con
 * {@code @Autowired}: desde Spring Boot 4 la autoconfiguración de Jackson
 * expone por defecto el mapper de Jackson 3
 * ({@code tools.jackson.databind.json.JsonMapper}, bean
 * {@code jacksonJsonMapper}) y ya no registra un bean de
 * {@code com.fasterxml.jackson.databind.ObjectMapper} (Jackson 2) en el
 * contexto de la aplicación; como esta prueba solo necesita parsear un JSON
 * plano para extraer el {@code accessToken}, no depende de ningún
 * comportamiento configurado a nivel de aplicación y una instancia local es
 * suficiente y evita ese {@code NoSuchBeanDefinitionException}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class TenantAdminAuthorizationIT extends PostgresTestContainerConfig {

    private static final String RAW_PASSWORD = "S3cret-Pass!";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantJpaRepository tenantRepository;

    @Autowired
    private AppUserSpringDataRepository appUserRepository;

    private Long seedUser(Long tenantId, String email, TenantRole tenantRole) {
        String passwordHash = new BCryptPasswordEncoder().encode(RAW_PASSWORD);
        return appUserRepository.save(new AppUserJpaEntity(tenantId, email, passwordHash, "Test User", tenantRole))
                .getId();
    }

    private String loginAndGetAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    @Test
    void memberCannotDisableAnotherUser() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        seedUser(tenantId, "member1@acme.com", TenantRole.MEMBER);
        Long targetId = seedUser(tenantId, "target1@acme.com", TenantRole.MEMBER);

        String memberAccessToken = loginAndGetAccessToken("member1@acme.com");

        mockMvc.perform(patch("/api/v1/users/{id}/disabled", targetId)
                        .header("Authorization", "Bearer " + memberAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabled\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantAdminCanDisableAnotherUser() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        seedUser(tenantId, "admin1@acme.com", TenantRole.TENANT_ADMIN);
        Long targetId = seedUser(tenantId, "target2@acme.com", TenantRole.MEMBER);

        String adminAccessToken = loginAndGetAccessToken("admin1@acme.com");

        mockMvc.perform(patch("/api/v1/users/{id}/disabled", targetId)
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabled\":true}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void memberCannotChangeAnotherUsersTenantRole() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        seedUser(tenantId, "member2@acme.com", TenantRole.MEMBER);
        Long targetId = seedUser(tenantId, "target3@acme.com", TenantRole.MEMBER);

        String memberAccessToken = loginAndGetAccessToken("member2@acme.com");

        mockMvc.perform(patch("/api/v1/users/{id}/tenant-role", targetId)
                        .header("Authorization", "Bearer " + memberAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantRole\":\"TENANT_ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantAdminCanChangeAnotherUsersTenantRole() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        seedUser(tenantId, "admin2@acme.com", TenantRole.TENANT_ADMIN);
        Long targetId = seedUser(tenantId, "target4@acme.com", TenantRole.MEMBER);

        String adminAccessToken = loginAndGetAccessToken("admin2@acme.com");

        mockMvc.perform(patch("/api/v1/users/{id}/tenant-role", targetId)
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantRole\":\"TENANT_ADMIN\"}"))
                .andExpect(status().isNoContent());
    }
}

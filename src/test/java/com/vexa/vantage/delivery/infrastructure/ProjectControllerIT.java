package com.vexa.vantage.delivery.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.identity.domain.TenantRole;
import com.vexa.vantage.identity.infrastructure.persistence.AppUserJpaEntity;
import com.vexa.vantage.identity.infrastructure.persistence.AppUserSpringDataRepository;
import com.vexa.vantage.identity.infrastructure.persistence.ProjectMembershipJpaRepository;
import com.vexa.vantage.identity.infrastructure.persistence.ProjectMembershipMapper;
import com.vexa.vantage.identity.infrastructure.persistence.TenantJpaEntity;
import com.vexa.vantage.identity.infrastructure.persistence.TenantJpaRepository;
import com.vexa.vantage.identity.domain.ProjectMembership;
import com.vexa.vantage.shared.domain.UserId;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración (con PostgreSQL real vía Testcontainers, JWT real)
 * de {@link ProjectController} de extremo a extremo: creación de proyecto →
 * el creador recibe membresía {@code OWNER} automáticamente (requisito
 * "generalized-rbac", "Default Role Assignment") → puede leer su propia
 * {@code WorkflowDefinition} (esto TAMBIÉN demuestra que la búsqueda de la
 * plantilla built-in funciona con el filtro de tenant activo — carry-over de
 * batch 3a); archivar por ADMIN/OWNER es exitoso; archivar por MEMBER → 403;
 * un no-miembro recibe 403 al pedir la definición de workflow.
 *
 * <p>El {@link ObjectMapper} se instancia localmente (Jackson 3 no expone un
 * bean {@code com.fasterxml.jackson.databind.ObjectMapper}), mismo patrón
 * que {@code TenantAdminAuthorizationIT}/{@code ProjectMembershipAuthorizationIT}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class ProjectControllerIT extends PostgresTestContainerConfig {

    private static final String RAW_PASSWORD = "S3cret-Pass!";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantJpaRepository tenantRepository;

    @Autowired
    private AppUserSpringDataRepository appUserRepository;

    @Autowired
    private ProjectMembershipJpaRepository projectMembershipRepository;

    private Long seedUser(Long tenantId, String email) {
        String passwordHash = new BCryptPasswordEncoder().encode(RAW_PASSWORD);
        return appUserRepository
                .save(new AppUserJpaEntity(tenantId, email, passwordHash, "Test User", TenantRole.MEMBER))
                .getId();
    }

    private void seedMembership(Long projectId, Long userId, ProjectRole role) {
        ProjectMembership membership = new ProjectMembership(
                String.valueOf(projectId), UserId.of(String.valueOf(userId)), role, null);
        projectMembershipRepository.save(ProjectMembershipMapper.toEntity(membership));
    }

    private String loginAndGetAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void creatorBecomesOwnerAndCanReadTheirProvisionedWorkflowDefinition() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        seedUser(tenantId, "creator1@acme.com");
        String accessToken = loginAndGetAccessToken("creator1@acme.com");

        MvcResult createResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Platform\",\"description\":\"Main product\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String projectId = created.get("id").asText();

        assertThat(projectMembershipRepository.findAll()).anyMatch(
                m -> String.valueOf(m.getProjectId()).equals(projectId) && m.getRole() == ProjectRole.OWNER);

        mockMvc.perform(get("/api/v1/projects/{id}/workflow-definition", projectId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Scrum"));
    }

    @Test
    void ownerCanArchiveTheirOwnProject() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        seedUser(tenantId, "owner1@acme.com");
        String accessToken = loginAndGetAccessToken("owner1@acme.com");

        MvcResult createResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Platform\",\"description\":\"Main product\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/v1/projects/{id}/status", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void memberCannotArchiveAProject() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        seedUser(tenantId, "creator2@acme.com");
        String creatorToken = loginAndGetAccessToken("creator2@acme.com");
        MvcResult createResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Platform\",\"description\":\"Main product\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        Long memberId = seedUser(tenantId, "pc-member1@acme.com");
        seedMembership(Long.valueOf(projectId), memberId, ProjectRole.MEMBER);
        String memberToken = loginAndGetAccessToken("pc-member1@acme.com");

        mockMvc.perform(patch("/api/v1/projects/{id}/status", projectId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonMemberCannotReadTheWorkflowDefinition() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        seedUser(tenantId, "creator3@acme.com");
        String creatorToken = loginAndGetAccessToken("creator3@acme.com");
        MvcResult createResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Platform\",\"description\":\"Main product\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        seedUser(tenantId, "outsider2@acme.com");
        String outsiderToken = loginAndGetAccessToken("outsider2@acme.com");

        mockMvc.perform(get("/api/v1/projects/{id}/workflow-definition", projectId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }
}

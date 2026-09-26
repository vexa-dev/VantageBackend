package com.vexa.vantage.identity.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vexa.vantage.identity.domain.ProjectMembership;
import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.identity.domain.TenantRole;
import com.vexa.vantage.identity.infrastructure.persistence.AppUserJpaEntity;
import com.vexa.vantage.identity.infrastructure.persistence.AppUserSpringDataRepository;
import com.vexa.vantage.identity.infrastructure.persistence.ProjectMembershipJpaRepository;
import com.vexa.vantage.identity.infrastructure.persistence.ProjectMembershipMapper;
import com.vexa.vantage.identity.infrastructure.persistence.TenantJpaEntity;
import com.vexa.vantage.identity.infrastructure.persistence.TenantJpaRepository;
import com.vexa.vantage.shared.domain.UserId;
import com.vexa.vantage.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración (con PostgreSQL real vía Testcontainers) de la
 * autorización de {@link ProjectMembershipController}:
 * {@code GET /api/v1/projects/{id}/memberships} exige que el actor sea al
 * menos {@link ProjectRole#VIEWER} del proyecto (un usuario ajeno al
 * proyecto recibe 403), y {@code POST /api/v1/projects/{id}/memberships}
 * exige al menos {@link ProjectRole#ADMIN} (un {@code MEMBER} o
 * {@code VIEWER} del proyecto recibe 403).
 *
 * <p>Los contextos delimitados de delivery/colaboración (dueños de las
 * tablas {@code project}/{@code workflow_definition}) todavía no existen
 * como código de aplicación, pero sus tablas ya están creadas por
 * {@code V1__baseline_schema.sql} y {@code project_membership.project_id}
 * las referencia con clave foránea; por eso esta prueba siembra esas dos
 * filas por SQL directo ({@link JdbcTemplate}) en lugar de un repositorio de
 * dominio, únicamente para satisfacer esa restricción de integridad.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class ProjectMembershipAuthorizationIT extends PostgresTestContainerConfig {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long seedUser(Long tenantId, String email) {
        String passwordHash = new BCryptPasswordEncoder().encode(RAW_PASSWORD);
        return appUserRepository
                .save(new AppUserJpaEntity(tenantId, email, passwordHash, "Test User", TenantRole.MEMBER))
                .getId();
    }

    private Long seedProject(Long tenantId) {
        Long workflowId = jdbcTemplate.queryForObject(
                "INSERT INTO workflow_definition (tenant_id, name, states_and_transitions) "
                        + "VALUES (?, ?, '{}'::jsonb) RETURNING id",
                Long.class, tenantId, "Default Workflow");
        return jdbcTemplate.queryForObject(
                "INSERT INTO project (tenant_id, name, workflow_definition_id) VALUES (?, ?, ?) RETURNING id",
                Long.class, tenantId, "Test Project", workflowId);
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
    void viewerCanListMembershipsOfTheirProject() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        Long projectId = seedProject(tenantId);
        Long viewerId = seedUser(tenantId, "viewer1@acme.com");
        seedMembership(projectId, viewerId, ProjectRole.VIEWER);

        String accessToken = loginAndGetAccessToken("viewer1@acme.com");

        mockMvc.perform(get("/api/v1/projects/{id}/memberships", projectId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("VIEWER"));
    }

    @Test
    void nonMemberCannotListMemberships() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        Long projectId = seedProject(tenantId);
        seedUser(tenantId, "outsider1@acme.com");

        String accessToken = loginAndGetAccessToken("outsider1@acme.com");

        mockMvc.perform(get("/api/v1/projects/{id}/memberships", projectId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCannotAddMembership() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        Long projectId = seedProject(tenantId);
        Long memberId = seedUser(tenantId, "projmember1@acme.com");
        seedMembership(projectId, memberId, ProjectRole.MEMBER);
        Long targetId = seedUser(tenantId, "newcomer1@acme.com");

        String accessToken = loginAndGetAccessToken("projmember1@acme.com");

        mockMvc.perform(post("/api/v1/projects/{id}/memberships", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%d\",\"role\":\"VIEWER\"}".formatted(targetId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAddMembership() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        Long projectId = seedProject(tenantId);
        Long adminId = seedUser(tenantId, "projadmin1@acme.com");
        seedMembership(projectId, adminId, ProjectRole.ADMIN);
        Long targetId = seedUser(tenantId, "newcomer2@acme.com");

        String accessToken = loginAndGetAccessToken("projadmin1@acme.com");

        mockMvc.perform(post("/api/v1/projects/{id}/memberships", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%d\",\"role\":\"VIEWER\"}".formatted(targetId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.userId").value(String.valueOf(targetId)));

        assertThat(projectMembershipRepository.findAll()).anyMatch(
                m -> m.getUserId().equals(targetId) && m.getRole() == ProjectRole.VIEWER);
    }
}

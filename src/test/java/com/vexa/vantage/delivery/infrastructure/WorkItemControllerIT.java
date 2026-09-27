package com.vexa.vantage.delivery.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración (con PostgreSQL real vía Testcontainers, JWT real)
 * de {@link WorkItemController} de extremo a extremo: creación, transición
 * de estado válida e inválida (acotada por {@code appliesTo}, per la
 * {@code WorkflowDefinition} sembrada), reparenting, round-trip de
 * {@code tshirtSize}, y que un {@code VIEWER} intentando crear reciba 403.
 *
 * <p>Siembra una {@code WorkflowDefinition} propia (no la plantilla built-in
 * "Scrum") con un conjunto de transiciones deliberadamente pequeño y
 * determinístico ({@code TODO -> DOING} es la ÚNICA transición declarada
 * para {@code STORY}), para poder afirmar sin ambigüedad qué transición es
 * válida y cuál no — mismo patrón de siembra por SQL directo que
 * {@code WorkItemJpaRepositoryIT}/{@code ProjectMembershipAuthorizationIT}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class WorkItemControllerIT extends PostgresTestContainerConfig {

    private static final String RAW_PASSWORD = "S3cret-Pass!";
    private static final String WORKFLOW_JSON = """
            {"version":1,"states":[
                {"key":"TODO","name":"To Do","category":"TODO","order":0,"appliesTo":["STORY"]},
                {"key":"DOING","name":"Doing","category":"IN_PROGRESS","order":1,"appliesTo":["STORY"]},
                {"key":"DONE","name":"Done","category":"DONE","order":2,"appliesTo":["STORY"]}
            ],"transitions":[
                {"from":"TODO","to":"DOING","appliesTo":["STORY"]}
            ],"initialStateByType":{"STORY":"TODO"}}
            """;

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
                        + "VALUES (?, ?, ?::jsonb) RETURNING id",
                Long.class, tenantId, "Test Workflow", WORKFLOW_JSON);
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

    private String createWorkItem(String accessToken, Long projectId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/projects/{id}/work-items", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"STORY\",\"state\":\"TODO\",\"title\":\"%s\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void memberCanCreateAWorkItem() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        Long projectId = seedProject(tenantId);
        Long memberId = seedUser(tenantId, "wi-member2@acme.com");
        seedMembership(projectId, memberId, ProjectRole.MEMBER);
        String accessToken = loginAndGetAccessToken("wi-member2@acme.com");

        mockMvc.perform(post("/api/v1/projects/{id}/work-items", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"STORY\",\"state\":\"TODO\",\"title\":\"Design the schema\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("STORY"))
                .andExpect(jsonPath("$.state").value("TODO"));
    }

    @Test
    void viewerCannotCreateAWorkItem() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        Long projectId = seedProject(tenantId);
        Long viewerId = seedUser(tenantId, "viewer2@acme.com");
        seedMembership(projectId, viewerId, ProjectRole.VIEWER);
        String accessToken = loginAndGetAccessToken("viewer2@acme.com");

        mockMvc.perform(post("/api/v1/projects/{id}/work-items", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"STORY\",\"state\":\"TODO\",\"title\":\"Design the schema\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchAppliesAValidStateTransition() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        Long projectId = seedProject(tenantId);
        Long memberId = seedUser(tenantId, "member3@acme.com");
        seedMembership(projectId, memberId, ProjectRole.MEMBER);
        String accessToken = loginAndGetAccessToken("member3@acme.com");
        String workItemId = createWorkItem(accessToken, projectId, "Design the schema");

        mockMvc.perform(patch("/api/v1/work-items/{id}", workItemId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"DOING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("DOING"));
    }

    @Test
    void patchRejectsAnUndeclaredStateTransition() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        Long projectId = seedProject(tenantId);
        Long memberId = seedUser(tenantId, "member4@acme.com");
        seedMembership(projectId, memberId, ProjectRole.MEMBER);
        String accessToken = loginAndGetAccessToken("member4@acme.com");
        String workItemId = createWorkItem(accessToken, projectId, "Design the schema");

        mockMvc.perform(patch("/api/v1/work-items/{id}", workItemId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"DONE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchReparentsAWorkItem() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        Long projectId = seedProject(tenantId);
        Long memberId = seedUser(tenantId, "member5@acme.com");
        seedMembership(projectId, memberId, ProjectRole.MEMBER);
        String accessToken = loginAndGetAccessToken("member5@acme.com");
        String parentId = createWorkItem(accessToken, projectId, "Epic parent");
        String childId = createWorkItem(accessToken, projectId, "Story child");

        mockMvc.perform(patch("/api/v1/work-items/{id}", childId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentWorkItemId\":\"%s\"}".formatted(parentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentWorkItemId").value(parentId));
    }

    @Test
    void patchRoundTripsTshirtSize() throws Exception {
        Long tenantId = tenantRepository.save(new TenantJpaEntity("Acme Corp", "FREE")).getId();
        Long projectId = seedProject(tenantId);
        Long memberId = seedUser(tenantId, "member6@acme.com");
        seedMembership(projectId, memberId, ProjectRole.MEMBER);
        String accessToken = loginAndGetAccessToken("member6@acme.com");
        String workItemId = createWorkItem(accessToken, projectId, "Design the schema");

        mockMvc.perform(patch("/api/v1/work-items/{id}", workItemId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tshirtSize\":\"L\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tshirtSize").value("L"));
    }
}

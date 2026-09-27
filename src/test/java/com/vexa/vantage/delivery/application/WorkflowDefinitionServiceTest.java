package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.Project;
import com.vexa.vantage.delivery.domain.WorkflowDefinition;
import com.vexa.vantage.delivery.domain.WorkflowDefinitionId;
import com.vexa.vantage.identity.domain.InsufficientPermissionException;
import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Prueba unitaria de {@link WorkflowDefinitionService} usando Mockito sobre
 * {@link ProjectRepositoryPort}/{@link WorkflowDefinitionRepositoryPort}:
 * recuperación de la {@link WorkflowDefinition} activa de un proyecto.
 */
class WorkflowDefinitionServiceTest {

    private static final ProjectId PROJECT_ID = ProjectId.of("project-1");
    private static final WorkflowDefinitionId DEFINITION_ID = WorkflowDefinitionId.of("wfd-1");
    private static final UserId ACTOR_ID = UserId.of("user-1");

    private ProjectRepositoryPort projectRepository;
    private WorkflowDefinitionRepositoryPort workflowDefinitionRepository;
    private ProjectRoleResolverPort projectRoleResolver;
    private WorkflowDefinitionService service;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepositoryPort.class);
        workflowDefinitionRepository = mock(WorkflowDefinitionRepositoryPort.class);
        projectRoleResolver = mock(ProjectRoleResolverPort.class);
        service = new WorkflowDefinitionService(projectRepository, workflowDefinitionRepository, projectRoleResolver);
    }

    private Project project() {
        return new Project(PROJECT_ID, TenantId.of("tenant-1"), "Platform", "desc", DEFINITION_ID);
    }

    private WorkflowDefinition definition() {
        Set<String> story = Set.of("STORY");
        return new WorkflowDefinition(
                DEFINITION_ID, "Scrum",
                List.of(new com.vexa.vantage.delivery.domain.WorkflowState("BACKLOG", "Backlog", "TODO", 0, story)),
                List.of(), Map.of("STORY", "BACKLOG"));
    }

    @Test
    void getByProjectReturnsTheActiveWorkflowDefinitionOfTheProject() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.VIEWER));
        when(workflowDefinitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(definition()));

        WorkflowDefinition result = service.getByProject(PROJECT_ID, ACTOR_ID);

        assertThat(result.id()).isEqualTo(DEFINITION_ID);
        assertThat(result.name()).isEqualTo("Scrum");
    }

    @Test
    void getByProjectThrowsWhenTheProjectDoesNotExist() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByProject(PROJECT_ID, ACTOR_ID))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void getByProjectThrowsWhenTheWorkflowDefinitionNoLongerExists() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.VIEWER));
        when(workflowDefinitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByProject(PROJECT_ID, ACTOR_ID))
                .isInstanceOf(WorkflowDefinitionNotFoundException.class);
    }

    @Test
    void getByProjectThrowsWhenActorLacksAtLeastViewerRole() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByProject(PROJECT_ID, ACTOR_ID))
                .isInstanceOf(InsufficientPermissionException.class);
    }
}

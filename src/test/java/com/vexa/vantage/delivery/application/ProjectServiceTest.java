package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.Project;
import com.vexa.vantage.delivery.domain.ProjectStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba unitaria de {@link ProjectService} usando Mockito sobre
 * {@link ProjectRepositoryPort}/{@link WorkflowDefinitionRepositoryPort}:
 * creación de proyecto (aprovisionando una copia de la plantilla built-in
 * "Scrum" del seed V2) y archivado de proyecto.
 */
class ProjectServiceTest {

    private static final TenantId TENANT_ID = TenantId.of("tenant-1");
    private static final UserId ACTOR_ID = UserId.of("user-1");

    private ProjectRepositoryPort projectRepository;
    private WorkflowDefinitionRepositoryPort workflowDefinitionRepository;
    private ProjectRoleResolverPort projectRoleResolver;
    private ProjectMembershipProvisioningPort membershipProvisioning;
    private ProjectService service;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepositoryPort.class);
        workflowDefinitionRepository = mock(WorkflowDefinitionRepositoryPort.class);
        projectRoleResolver = mock(ProjectRoleResolverPort.class);
        membershipProvisioning = mock(ProjectMembershipProvisioningPort.class);
        service = new ProjectService(
                projectRepository, workflowDefinitionRepository, projectRoleResolver, membershipProvisioning);
    }

    private WorkflowDefinition builtInScrumTemplate() {
        return new WorkflowDefinition(
                WorkflowDefinitionId.of("wfd-template"), "Scrum", List.of(templateState()), List.of(), Map.of("STORY", "BACKLOG"));
    }

    private com.vexa.vantage.delivery.domain.WorkflowState templateState() {
        return new com.vexa.vantage.delivery.domain.WorkflowState(
                "BACKLOG", "Backlog", "TODO", 0, java.util.Set.of("STORY"));
    }

    @Test
    void createProjectProvisionsTheBuiltInScrumWorkflowDefinitionAndPersistsTheProject() {
        WorkflowDefinition template = builtInScrumTemplate();
        WorkflowDefinition provisioned = template.withId(WorkflowDefinitionId.of("wfd-project-1"));
        when(workflowDefinitionRepository.findBuiltInTemplate()).thenReturn(Optional.of(template));
        when(workflowDefinitionRepository.save(any(WorkflowDefinition.class))).thenReturn(provisioned);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Project created = service.createProject(TENANT_ID, ACTOR_ID, "Platform", "The main product");

        assertThat(created.tenantId()).isEqualTo(TENANT_ID);
        assertThat(created.name()).isEqualTo("Platform");
        assertThat(created.status()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(created.workflowDefinitionId()).isEqualTo(WorkflowDefinitionId.of("wfd-project-1"));
        verify(projectRepository).save(created);
    }

    @Test
    void createProjectAssignsTheCreatorAsProjectOwner() {
        WorkflowDefinition template = builtInScrumTemplate();
        WorkflowDefinition provisioned = template.withId(WorkflowDefinitionId.of("wfd-project-1"));
        when(workflowDefinitionRepository.findBuiltInTemplate()).thenReturn(Optional.of(template));
        when(workflowDefinitionRepository.save(any(WorkflowDefinition.class))).thenReturn(provisioned);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project argument = invocation.getArgument(0);
            return new Project(
                    ProjectId.of("project-1"), argument.tenantId(), argument.name(), argument.description(),
                    argument.workflowDefinitionId(), argument.status());
        });

        Project created = service.createProject(TENANT_ID, ACTOR_ID, "Platform", "The main product");

        verify(membershipProvisioning).provisionOwner(created.id(), ACTOR_ID);
    }

    @Test
    void createProjectThrowsWhenNoBuiltInTemplateExists() {
        when(workflowDefinitionRepository.findBuiltInTemplate()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProject(TENANT_ID, ACTOR_ID, "Platform", "The main product"))
                .isInstanceOf(WorkflowDefinitionNotFoundException.class);

        verify(projectRepository, never()).save(any());
        verify(membershipProvisioning, never()).provisionOwner(any(), any());
    }

    @Test
    void archiveProjectMarksTheProjectArchivedAndPersists() {
        ProjectId projectId = ProjectId.of("project-1");
        Project project = new Project(projectId, TENANT_ID, "Platform", "desc", WorkflowDefinitionId.of("wfd-1"));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRoleResolver.findRole(projectId, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.ADMIN));
        when(projectRepository.save(project)).thenReturn(project);

        Project archived = service.archiveProject(projectId, ACTOR_ID);

        assertThat(archived.isArchived()).isTrue();
        verify(projectRepository).save(project);
    }

    @Test
    void archiveProjectThrowsWhenTheProjectDoesNotExist() {
        ProjectId projectId = ProjectId.of("missing");
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archiveProject(projectId, ACTOR_ID))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void archiveProjectThrowsWhenActorLacksAtLeastAdminRole() {
        ProjectId projectId = ProjectId.of("project-1");
        Project project = new Project(projectId, TENANT_ID, "Platform", "desc", WorkflowDefinitionId.of("wfd-1"));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRoleResolver.findRole(projectId, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.MEMBER));

        assertThatThrownBy(() -> service.archiveProject(projectId, ACTOR_ID))
                .isInstanceOf(InsufficientPermissionException.class);

        assertThat(project.isArchived()).isFalse();
        verify(projectRepository, never()).save(any());
    }

    @Test
    void restoreProjectMarksTheProjectActiveAndPersists() {
        ProjectId projectId = ProjectId.of("project-1");
        Project project = new Project(
                projectId, TENANT_ID, "Platform", "desc", WorkflowDefinitionId.of("wfd-1"), ProjectStatus.ARCHIVED);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRoleResolver.findRole(projectId, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.ADMIN));
        when(projectRepository.save(project)).thenReturn(project);

        Project restored = service.restoreProject(projectId, ACTOR_ID);

        assertThat(restored.isArchived()).isFalse();
        verify(projectRepository).save(project);
    }

    @Test
    void restoreProjectThrowsWhenActorLacksAtLeastAdminRole() {
        ProjectId projectId = ProjectId.of("project-1");
        Project project = new Project(
                projectId, TENANT_ID, "Platform", "desc", WorkflowDefinitionId.of("wfd-1"), ProjectStatus.ARCHIVED);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRoleResolver.findRole(projectId, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.MEMBER));

        assertThatThrownBy(() -> service.restoreProject(projectId, ACTOR_ID))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void listProjectsReturnsOnlyProjectsWhereTheActorHasMembershipExcludingArchivedByDefault() {
        ProjectId activeId = ProjectId.of("project-1");
        ProjectId archivedId = ProjectId.of("project-2");
        Project active = new Project(activeId, TENANT_ID, "Active", "desc", WorkflowDefinitionId.of("wfd-1"));
        Project archived = new Project(
                archivedId, TENANT_ID, "Archived", "desc", WorkflowDefinitionId.of("wfd-1"), ProjectStatus.ARCHIVED);
        when(projectRoleResolver.findProjectIdsForActor(ACTOR_ID)).thenReturn(List.of(activeId, archivedId));
        when(projectRepository.findByIds(List.of(activeId, archivedId))).thenReturn(List.of(active, archived));

        List<Project> result = service.listProjects(ACTOR_ID, false);

        assertThat(result).containsExactly(active);
    }

    @Test
    void listProjectsIncludesArchivedWhenRequested() {
        ProjectId activeId = ProjectId.of("project-1");
        ProjectId archivedId = ProjectId.of("project-2");
        Project active = new Project(activeId, TENANT_ID, "Active", "desc", WorkflowDefinitionId.of("wfd-1"));
        Project archived = new Project(
                archivedId, TENANT_ID, "Archived", "desc", WorkflowDefinitionId.of("wfd-1"), ProjectStatus.ARCHIVED);
        when(projectRoleResolver.findProjectIdsForActor(ACTOR_ID)).thenReturn(List.of(activeId, archivedId));
        when(projectRepository.findByIds(List.of(activeId, archivedId))).thenReturn(List.of(active, archived));

        List<Project> result = service.listProjects(ACTOR_ID, true);

        assertThat(result).containsExactlyInAnyOrder(active, archived);
    }
}

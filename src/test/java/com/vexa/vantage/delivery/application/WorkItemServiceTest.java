package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.IllegalTransitionException;
import com.vexa.vantage.delivery.domain.Project;
import com.vexa.vantage.delivery.domain.StateTransition;
import com.vexa.vantage.delivery.domain.TShirtSize;
import com.vexa.vantage.delivery.domain.WorkItem;
import com.vexa.vantage.delivery.domain.WorkflowDefinition;
import com.vexa.vantage.delivery.domain.WorkflowDefinitionId;
import com.vexa.vantage.delivery.domain.WorkflowState;
import com.vexa.vantage.identity.domain.InsufficientPermissionException;
import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.shared.domain.DomainValidationException;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.domain.UserId;
import com.vexa.vantage.shared.domain.WorkItemId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba unitaria de {@link WorkItemService} usando Mockito sobre
 * {@link WorkItemRepositoryPort}/{@link ProjectRepositoryPort}/
 * {@link WorkflowDefinitionRepositoryPort}: creación, cambio de estado
 * (delegando la validación a {@link WorkflowDefinition}) y reparenting vía
 * {@code parent_work_item_id}.
 *
 * <p>No hay validación de tipo de hijo (p. ej. {@code allowedChildTypes})
 * en el reparenting: ni el spec ni el diseño la exigen en esta capa (ver
 * informe de aplicación de esta fase) — decisión deliberada, no un olvido.
 */
class WorkItemServiceTest {

    private static final ProjectId PROJECT_ID = ProjectId.of("project-1");
    private static final WorkflowDefinitionId DEFINITION_ID = WorkflowDefinitionId.of("wfd-1");
    private static final UserId ACTOR_ID = UserId.of("user-1");

    private WorkItemRepositoryPort workItemRepository;
    private ProjectRepositoryPort projectRepository;
    private WorkflowDefinitionRepositoryPort workflowDefinitionRepository;
    private ProjectRoleResolverPort projectRoleResolver;
    private WorkItemService service;

    @BeforeEach
    void setUp() {
        workItemRepository = mock(WorkItemRepositoryPort.class);
        projectRepository = mock(ProjectRepositoryPort.class);
        workflowDefinitionRepository = mock(WorkflowDefinitionRepositoryPort.class);
        projectRoleResolver = mock(ProjectRoleResolverPort.class);
        service = new WorkItemService(workItemRepository, projectRepository, workflowDefinitionRepository, projectRoleResolver);
    }

    private Project activeProject() {
        return new Project(PROJECT_ID, TenantId.of("tenant-1"), "Platform", "desc", DEFINITION_ID);
    }

    private WorkflowDefinition scrumDefinition() {
        Set<String> story = Set.of("STORY");
        List<WorkflowState> states = List.of(
                new WorkflowState("TODO", "To Do", "TODO", 0, story),
                new WorkflowState("DOING", "Doing", "IN_PROGRESS", 1, story),
                new WorkflowState("DONE", "Done", "DONE", 2, story));
        List<StateTransition> transitions = List.of(new StateTransition("TODO", "DOING", story));
        return new WorkflowDefinition(DEFINITION_ID, "Scrum", states, transitions, Map.of("STORY", "TODO"));
    }

    @Test
    void createWorkItemPersistsANewWorkItemAssociatedWithTheProject() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(activeProject()));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.MEMBER));
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkItem created = service.createWorkItem(
                PROJECT_ID, ACTOR_ID, "STORY", "TODO", "Design the schema", null, null, 50, 30, 5);

        assertThat(created.projectId()).isEqualTo(PROJECT_ID);
        assertThat(created.type()).isEqualTo("STORY");
        assertThat(created.state()).isEqualTo("TODO");
        assertThat(created.title()).isEqualTo("Design the schema");
        verify(workItemRepository).save(created);
    }

    @Test
    void createWorkItemAppliesDescriptionAndTshirtSizeWhenProvided() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(activeProject()));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.MEMBER));
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkItem created = service.createWorkItem(
                PROJECT_ID, ACTOR_ID, "STORY", "TODO", "Design the schema", "Full description", TShirtSize.M,
                50, 30, 5);

        assertThat(created.description()).isEqualTo("Full description");
        assertThat(created.tshirtSize()).contains(TShirtSize.M);
    }

    @Test
    void createWorkItemThrowsWhenTheProjectDoesNotExist() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createWorkItem(
                PROJECT_ID, ACTOR_ID, "STORY", "TODO", "Design the schema", null, null, 50, 30, 5))
                .isInstanceOf(ProjectNotFoundException.class);

        verify(workItemRepository, never()).save(any());
    }

    @Test
    void createWorkItemThrowsWhenActorLacksAtLeastMemberRole() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(activeProject()));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.VIEWER));

        assertThatThrownBy(() -> service.createWorkItem(
                PROJECT_ID, ACTOR_ID, "STORY", "TODO", "Design the schema", null, null, 50, 30, 5))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(workItemRepository, never()).save(any());
    }

    @Test
    void transitionStateDelegatesValidationToWorkflowDefinitionAndPersistsOnSuccess() {
        WorkItemId itemId = WorkItemId.of("item-1");
        WorkItem item = new WorkItem(itemId, PROJECT_ID, "STORY", "TODO", "Design the schema", 50, 30, 5);
        when(workItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(activeProject()));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.MEMBER));
        when(workflowDefinitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(scrumDefinition()));
        when(workItemRepository.save(item)).thenReturn(item);

        WorkItem transitioned = service.transitionState(itemId, ACTOR_ID, "DOING");

        assertThat(transitioned.state()).isEqualTo("DOING");
        verify(workItemRepository).save(item);
    }

    @Test
    void transitionStateRejectsAnUndeclaredTransitionAndLeavesStateUnchanged() {
        WorkItemId itemId = WorkItemId.of("item-1");
        WorkItem item = new WorkItem(itemId, PROJECT_ID, "STORY", "TODO", "Design the schema", 50, 30, 5);
        when(workItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(activeProject()));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.MEMBER));
        when(workflowDefinitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(scrumDefinition()));

        assertThatThrownBy(() -> service.transitionState(itemId, ACTOR_ID, "DONE"))
                .isInstanceOf(IllegalTransitionException.class);

        assertThat(item.state()).isEqualTo("TODO");
        verify(workItemRepository, never()).save(any());
    }

    @Test
    void transitionStateThrowsWhenTheWorkItemDoesNotExist() {
        WorkItemId itemId = WorkItemId.of("missing");
        when(workItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transitionState(itemId, ACTOR_ID, "DOING"))
                .isInstanceOf(WorkItemNotFoundException.class);
    }

    @Test
    void transitionStateThrowsWhenActorLacksAtLeastMemberRole() {
        WorkItemId itemId = WorkItemId.of("item-1");
        WorkItem item = new WorkItem(itemId, PROJECT_ID, "STORY", "TODO", "Design the schema", 50, 30, 5);
        when(workItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(activeProject()));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.VIEWER));

        assertThatThrownBy(() -> service.transitionState(itemId, ACTOR_ID, "DOING"))
                .isInstanceOf(InsufficientPermissionException.class);

        assertThat(item.state()).isEqualTo("TODO");
        verify(workItemRepository, never()).save(any());
    }

    @Test
    void reparentAssignsTheGivenParentAndPersists() {
        WorkItemId itemId = WorkItemId.of("item-1");
        WorkItemId parentId = WorkItemId.of("epic-1");
        WorkItem item = new WorkItem(itemId, PROJECT_ID, "STORY", "TODO", "Design the schema", 50, 30, 5);
        when(workItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.MEMBER));
        when(workItemRepository.save(item)).thenReturn(item);

        WorkItem reparented = service.reparent(itemId, ACTOR_ID, parentId);

        assertThat(reparented.parentWorkItemId()).contains(parentId);
        verify(workItemRepository).save(item);
    }

    @Test
    void reparentRejectsSettingAnItemAsItsOwnParent() {
        WorkItemId itemId = WorkItemId.of("item-1");
        WorkItem item = new WorkItem(itemId, PROJECT_ID, "STORY", "TODO", "Design the schema", 50, 30, 5);
        when(workItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.MEMBER));

        assertThatThrownBy(() -> service.reparent(itemId, ACTOR_ID, itemId))
                .isInstanceOf(DomainValidationException.class);

        verify(workItemRepository, never()).save(any());
    }

    @Test
    void reparentThrowsWhenActorLacksAtLeastMemberRole() {
        WorkItemId itemId = WorkItemId.of("item-1");
        WorkItemId parentId = WorkItemId.of("epic-1");
        WorkItem item = new WorkItem(itemId, PROJECT_ID, "STORY", "TODO", "Design the schema", 50, 30, 5);
        when(workItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.VIEWER));

        assertThatThrownBy(() -> service.reparent(itemId, ACTOR_ID, parentId))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(workItemRepository, never()).save(any());
    }

    @Test
    void listByProjectReturnsTheProjectsWorkItems() {
        WorkItem item = new WorkItem(WorkItemId.of("item-1"), PROJECT_ID, "STORY", "TODO", "Design the schema", 50, 30, 5);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(activeProject()));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.VIEWER));
        when(workItemRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(item));

        List<WorkItem> result = service.listByProject(PROJECT_ID, ACTOR_ID);

        assertThat(result).containsExactly(item);
    }

    @Test
    void listByProjectThrowsWhenActorHasNoMembership() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(activeProject()));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listByProject(PROJECT_ID, ACTOR_ID))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void patchTransitionsStateWhenStateIsProvided() {
        WorkItemId itemId = WorkItemId.of("item-1");
        WorkItem item = new WorkItem(itemId, PROJECT_ID, "STORY", "TODO", "Design the schema", 50, 30, 5);
        when(workItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(activeProject()));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.MEMBER));
        when(workflowDefinitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(scrumDefinition()));
        when(workItemRepository.save(item)).thenReturn(item);

        WorkItem patched = service.patch(itemId, ACTOR_ID, WorkItemPatchCommand.empty().withState("DOING"));

        assertThat(patched.state()).isEqualTo("DOING");
    }

    @Test
    void patchUpdatesTitleDescriptionPrioritizationAndTshirtSizeWhenProvided() {
        WorkItemId itemId = WorkItemId.of("item-1");
        WorkItem item = new WorkItem(itemId, PROJECT_ID, "STORY", "TODO", "Design the schema", 50, 30, 5);
        when(workItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.MEMBER));
        when(workItemRepository.save(item)).thenReturn(item);

        WorkItemPatchCommand command = WorkItemPatchCommand.empty()
                .withTitle("Design the new schema")
                .withDescription("Updated description")
                .withPrioritization(90, 40, 8)
                .withTshirtSize(TShirtSize.L);

        WorkItem patched = service.patch(itemId, ACTOR_ID, command);

        assertThat(patched.title()).isEqualTo("Design the new schema");
        assertThat(patched.description()).isEqualTo("Updated description");
        assertThat(patched.businessValue()).isEqualTo(90);
        assertThat(patched.urgency()).isEqualTo(40);
        assertThat(patched.storyPoints()).isEqualTo(8);
        assertThat(patched.tshirtSize()).contains(TShirtSize.L);
    }

    @Test
    void patchReparentsAndReassignsSprintWhenProvided() {
        WorkItemId itemId = WorkItemId.of("item-1");
        WorkItemId parentId = WorkItemId.of("epic-1");
        WorkItemId sprintId = WorkItemId.of("sprint-1");
        WorkItem item = new WorkItem(itemId, PROJECT_ID, "STORY", "TODO", "Design the schema", 50, 30, 5);
        when(workItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.MEMBER));
        when(workItemRepository.save(item)).thenReturn(item);

        WorkItemPatchCommand command = WorkItemPatchCommand.empty().withParent(parentId).withSprint(sprintId);
        WorkItem patched = service.patch(itemId, ACTOR_ID, command);

        assertThat(patched.parentWorkItemId()).contains(parentId);
        assertThat(patched.sprintWorkItemId()).contains(sprintId);
    }

    @Test
    void patchClearsAssigneeWhenAssigneeProvidedWithNullValue() {
        WorkItemId itemId = WorkItemId.of("item-1");
        WorkItem item = new WorkItem(itemId, PROJECT_ID, "STORY", "TODO", "Design the schema", 50, 30, 5);
        item.assignTo(UserId.of("previous-assignee"));
        when(workItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.MEMBER));
        when(workItemRepository.save(item)).thenReturn(item);

        WorkItem patched = service.patch(itemId, ACTOR_ID, WorkItemPatchCommand.empty().withAssignee(null));

        assertThat(patched.assigneeId()).isEmpty();
    }

    @Test
    void patchThrowsWhenActorLacksAtLeastMemberRole() {
        WorkItemId itemId = WorkItemId.of("item-1");
        WorkItem item = new WorkItem(itemId, PROJECT_ID, "STORY", "TODO", "Design the schema", 50, 30, 5);
        when(workItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.VIEWER));

        assertThatThrownBy(() -> service.patch(itemId, ACTOR_ID, WorkItemPatchCommand.empty().withTitle("New title")))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(workItemRepository, never()).save(any());
    }
}

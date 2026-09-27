package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.Board;
import com.vexa.vantage.delivery.domain.BoardId;
import com.vexa.vantage.delivery.domain.Project;
import com.vexa.vantage.delivery.domain.WorkItem;
import com.vexa.vantage.delivery.domain.WorkflowDefinition;
import com.vexa.vantage.delivery.domain.WorkflowDefinitionId;
import com.vexa.vantage.delivery.domain.WorkflowState;
import com.vexa.vantage.identity.domain.InsufficientPermissionException;
import com.vexa.vantage.identity.domain.ProjectRole;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Prueba unitaria de {@link BoardService} usando Mockito sobre
 * {@link BoardRepositoryPort}/{@link ProjectRepositoryPort}/
 * {@link WorkflowDefinitionRepositoryPort}/{@link WorkItemRepositoryPort}:
 * recuperación del tablero de un proyecto, agrupada por estado de workflow.
 */
class BoardServiceTest {

    private static final ProjectId PROJECT_ID = ProjectId.of("project-1");
    private static final WorkflowDefinitionId DEFINITION_ID = WorkflowDefinitionId.of("wfd-1");
    private static final UserId ACTOR_ID = UserId.of("user-1");

    private BoardRepositoryPort boardRepository;
    private ProjectRepositoryPort projectRepository;
    private WorkflowDefinitionRepositoryPort workflowDefinitionRepository;
    private WorkItemRepositoryPort workItemRepository;
    private ProjectRoleResolverPort projectRoleResolver;
    private BoardService service;

    @BeforeEach
    void setUp() {
        boardRepository = mock(BoardRepositoryPort.class);
        projectRepository = mock(ProjectRepositoryPort.class);
        workflowDefinitionRepository = mock(WorkflowDefinitionRepositoryPort.class);
        workItemRepository = mock(WorkItemRepositoryPort.class);
        projectRoleResolver = mock(ProjectRoleResolverPort.class);
        service = new BoardService(boardRepository, projectRepository, workflowDefinitionRepository, workItemRepository, projectRoleResolver);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(
                new Project(PROJECT_ID, TenantId.of("tenant-1"), "Platform", "desc", DEFINITION_ID)));
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.of(ProjectRole.VIEWER));
        when(workflowDefinitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(taskWorkflow()));
    }

    private WorkflowDefinition taskWorkflow() {
        Set<String> task = Set.of("TASK");
        List<WorkflowState> states = List.of(
                new WorkflowState("TO_DO", "To Do", "TODO", 0, task),
                new WorkflowState("IN_PROGRESS", "In Progress", "IN_PROGRESS", 1, task),
                new WorkflowState("DONE", "Done", "DONE", 2, task));
        return new WorkflowDefinition(DEFINITION_ID, "Scrum", states, List.of(), Map.of("TASK", "TO_DO"));
    }

    private WorkItem taskItem(String id, String state) {
        return new WorkItem(WorkItemId.of(id), PROJECT_ID, "TASK", state, "Item " + id, 10, 10, 3);
    }

    @Test
    void viewBoardGroupsWorkItemsByWorkflowStateForTheGivenItemType() {
        WorkItem inProgress = taskItem("item-1", "IN_PROGRESS");
        WorkItem otherTypeIgnored = new WorkItem(WorkItemId.of("epic-1"), PROJECT_ID, "EPIC", "TO_DO", "Epic", 10, 10, null);
        when(workItemRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(inProgress, otherTypeIgnored));
        when(boardRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.empty());

        BoardView view = service.viewBoard(PROJECT_ID, ACTOR_ID, "TASK");

        assertThat(view.columns()).extracting(column -> column.state().key())
                .containsExactly("TO_DO", "IN_PROGRESS", "DONE");
        assertThat(view.columns().get(0).items()).isEmpty();
        assertThat(view.columns().get(1).items()).containsExactly(inProgress);
        assertThat(view.columns().get(2).items()).isEmpty();
    }

    @Test
    void viewBoardUsesTheSavedBoardNameWhenABoardRowExists() {
        when(workItemRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of());
        when(boardRepository.findByProjectId(PROJECT_ID))
                .thenReturn(Optional.of(new Board(BoardId.of("board-1"), PROJECT_ID, "Sprint Board")));

        BoardView view = service.viewBoard(PROJECT_ID, ACTOR_ID, "TASK");

        assertThat(view.boardId()).contains(BoardId.of("board-1"));
        assertThat(view.boardName()).isEqualTo("Sprint Board");
    }

    @Test
    void viewBoardDefaultsToADefaultNameWhenNoBoardRowExists() {
        when(workItemRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of());
        when(boardRepository.findByProjectId(PROJECT_ID)).thenReturn(Optional.empty());

        BoardView view = service.viewBoard(PROJECT_ID, ACTOR_ID, "TASK");

        assertThat(view.boardId()).isEmpty();
        assertThat(view.boardName()).isEqualTo("Board");
    }

    @Test
    void viewBoardThrowsWhenTheProjectDoesNotExist() {
        ProjectId missing = ProjectId.of("missing");
        when(projectRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.viewBoard(missing, ACTOR_ID, "TASK"))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void viewBoardThrowsWhenActorLacksAtLeastViewerRole() {
        when(projectRoleResolver.findRole(PROJECT_ID, ACTOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.viewBoard(PROJECT_ID, ACTOR_ID, "TASK"))
                .isInstanceOf(InsufficientPermissionException.class);
    }
}

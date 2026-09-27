package com.vexa.vantage.delivery.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.WorkItemId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica {@link WorkItem}: las auto-referencias {@code parentWorkItemId}/
 * {@code sprintWorkItemId}, la delegación de las transiciones de estado a
 * {@link WorkflowDefinition} (dominio puro, sin persistencia), y que
 * {@link WsjfScore} se calcula bajo demanda a partir de los campos actuales
 * (nunca se guarda un resultado previamente calculado).
 */
class WorkItemTest {

    private static final ProjectId PROJECT_ID = ProjectId.of("project-1");
    private static final WorkItemId ITEM_ID = WorkItemId.of("item-1");

    private WorkflowDefinition storyWorkflow() {
        Set<String> story = Set.of("STORY");
        List<WorkflowState> states = List.of(
                new WorkflowState("BACKLOG", "Backlog", "TODO", 0, story),
                new WorkflowState("TODO", "To Do", "TODO", 1, story),
                new WorkflowState("DOING", "Doing", "IN_PROGRESS", 2, story),
                new WorkflowState("DONE", "Done", "DONE", 3, story)
        );
        List<StateTransition> transitions = List.of(
                new StateTransition("BACKLOG", "TODO", story),
                new StateTransition("TODO", "DOING", story),
                new StateTransition("DOING", "DONE", story)
        );
        return new WorkflowDefinition(
                WorkflowDefinitionId.of("wfd-1"), "Scrum", states, transitions, Map.of("STORY", "BACKLOG"));
    }

    private WorkItem newStory(String initialState) {
        return new WorkItem(ITEM_ID, PROJECT_ID, "STORY", initialState, "Implement login", 80, 60, 5);
    }

    @Test
    void createsWorkItemWithoutParentOrSprintByDefault() {
        WorkItem item = newStory("TODO");

        assertThat(item.parentWorkItemId()).isEmpty();
        assertThat(item.sprintWorkItemId()).isEmpty();
        assertThat(item.id()).isEqualTo(ITEM_ID);
        assertThat(item.projectId()).isEqualTo(PROJECT_ID);
        assertThat(item.type()).isEqualTo("STORY");
        assertThat(item.state()).isEqualTo("TODO");
    }

    @Test
    void changeParentSetsTheParentWorkItemReference() {
        WorkItem item = newStory("TODO");
        WorkItemId parentId = WorkItemId.of("epic-1");

        item.changeParent(parentId);

        assertThat(item.parentWorkItemId()).contains(parentId);
    }

    @Test
    void changeParentToNullClearsTheParentReference() {
        WorkItem item = newStory("TODO");
        item.changeParent(WorkItemId.of("epic-1"));

        item.changeParent(null);

        assertThat(item.parentWorkItemId()).isEmpty();
    }

    @Test
    void rejectsSettingItselfAsItsOwnParent() {
        WorkItem item = newStory("TODO");

        assertThatThrownBy(() -> item.changeParent(ITEM_ID))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("parent");
    }

    @Test
    void assignToSprintSetsTheSprintWorkItemReference() {
        WorkItem item = newStory("TODO");
        WorkItemId sprintId = WorkItemId.of("sprint-1");

        item.assignToSprint(sprintId);

        assertThat(item.sprintWorkItemId()).contains(sprintId);
    }

    @Test
    void rejectsAssigningItselfAsItsOwnSprint() {
        WorkItem item = newStory("TODO");

        assertThatThrownBy(() -> item.assignToSprint(ITEM_ID))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("sprint");
    }

    @Test
    void transitionToDelegatesValidationToWorkflowDefinitionAndUpdatesState() {
        WorkItem item = newStory("TODO");

        item.transitionTo("DOING", storyWorkflow());

        assertThat(item.state()).isEqualTo("DOING");
    }

    @Test
    void transitionToRejectsAnUndefinedTransitionAndLeavesStateUnchanged() {
        WorkItem item = newStory("TODO");

        assertThatThrownBy(() -> item.transitionTo("DONE", storyWorkflow()))
                .isInstanceOf(IllegalTransitionException.class);
        assertThat(item.state()).isEqualTo("TODO");
    }

    @Test
    void wsjfScoreIsComputedOnDemandFromCurrentFields() {
        WorkItem item = newStory("TODO");

        assertThat(item.wsjfScore()).isEqualTo(WsjfScore.compute(80, 60, 5));
    }

    @Test
    void wsjfScoreReflectsFieldChangesRatherThanACachedValue() {
        WorkItem item = newStory("TODO");
        WsjfScore before = item.wsjfScore();

        item.updatePrioritization(20, 10, 2);

        assertThat(item.wsjfScore()).isNotEqualTo(before);
        assertThat(item.wsjfScore()).isEqualTo(WsjfScore.compute(20, 10, 2));
    }

    @Test
    void changeTitleUpdatesTheTitle() {
        WorkItem item = newStory("TODO");

        item.changeTitle("Implement OAuth login");

        assertThat(item.title()).isEqualTo("Implement OAuth login");
    }

    @Test
    void changeTitleRejectsBlank() {
        WorkItem item = newStory("TODO");

        assertThatThrownBy(() -> item.changeTitle("  "))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void tshirtSizeIsEmptyByDefault() {
        WorkItem item = newStory("TODO");

        assertThat(item.tshirtSize()).isEmpty();
    }

    @Test
    void changeTshirtSizeSetsTheTshirtSizeEstimate() {
        WorkItem item = newStory("TODO");

        item.changeTshirtSize(TShirtSize.L);

        assertThat(item.tshirtSize()).contains(TShirtSize.L);
    }

    @Test
    void changeTshirtSizeToNullClearsTheEstimate() {
        WorkItem item = newStory("TODO");
        item.changeTshirtSize(TShirtSize.L);

        item.changeTshirtSize(null);

        assertThat(item.tshirtSize()).isEmpty();
    }
}

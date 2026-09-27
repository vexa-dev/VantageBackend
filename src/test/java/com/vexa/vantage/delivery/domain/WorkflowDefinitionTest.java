package com.vexa.vantage.delivery.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica {@link WorkflowDefinition}: la representación en memoria de la
 * configuración JSONB de {@code workflow_definition.states_and_transitions}
 * (la decodificación JSON en sí corre en infraestructura, vía
 * {@code WorkflowDefinitionJsonCodec} — este test construye el objeto de
 * valor directamente, sin Jackson, preservando la pureza de dominio), la
 * búsqueda de estados acotada por {@code appliesTo}/tipo de item, y el
 * rechazo de transiciones no declaradas para el estado actual.
 *
 * <p>Los datos de prueba reproducen la plantilla built-in "Scrum" de
 * {@code V2__builtin_workflow_templates.sql}: EPIC/STORY comparten un
 * conjunto de estados, TASK tiene el suyo propio (incluyendo BLOCKED).
 */
class WorkflowDefinitionTest {

    private static final Set<String> EPIC_STORY = Set.of("EPIC", "STORY");
    private static final Set<String> TASK_ONLY = Set.of("TASK");

    private WorkflowDefinition scrumTemplate() {
        List<WorkflowState> states = List.of(
                new WorkflowState("BACKLOG", "Backlog", "TODO", 0, EPIC_STORY),
                new WorkflowState("TODO", "To Do", "TODO", 1, EPIC_STORY),
                new WorkflowState("DOING", "Doing", "IN_PROGRESS", 2, EPIC_STORY),
                new WorkflowState("TESTING", "Testing", "IN_PROGRESS", 3, EPIC_STORY),
                new WorkflowState("DONE", "Done", "DONE", 4, Set.of("EPIC", "STORY", "TASK")),
                new WorkflowState("TO_DO", "To Do", "TODO", 0, TASK_ONLY),
                new WorkflowState("IN_PROGRESS", "In Progress", "IN_PROGRESS", 1, TASK_ONLY),
                new WorkflowState("BLOCKED", "Blocked", "BLOCKED", 4, TASK_ONLY)
        );
        List<StateTransition> transitions = List.of(
                new StateTransition("BACKLOG", "TODO", EPIC_STORY),
                new StateTransition("TODO", "DOING", EPIC_STORY),
                new StateTransition("DOING", "TESTING", EPIC_STORY),
                new StateTransition("TESTING", "DONE", EPIC_STORY),
                new StateTransition("TO_DO", "IN_PROGRESS", TASK_ONLY),
                new StateTransition("IN_PROGRESS", "BLOCKED", TASK_ONLY),
                new StateTransition("BLOCKED", "IN_PROGRESS", TASK_ONLY)
        );
        Map<String, String> initialStateByType = Map.of("EPIC", "BACKLOG", "STORY", "BACKLOG", "TASK", "TO_DO");
        return new WorkflowDefinition(WorkflowDefinitionId.of("wfd-1"), "Scrum", states, transitions, initialStateByType);
    }

    @Test
    void exposesStatesAndTransitionsAfterConstruction() {
        WorkflowDefinition definition = scrumTemplate();

        assertThat(definition.id()).isEqualTo(WorkflowDefinitionId.of("wfd-1"));
        assertThat(definition.name()).isEqualTo("Scrum");
    }

    @Test
    void statesExposesEveryDeclaredStateAcrossAllItemTypesUnfiltered() {
        WorkflowDefinition definition = scrumTemplate();

        assertThat(definition.states()).hasSize(8);
    }

    @Test
    void transitionsExposesEveryDeclaredTransitionAcrossAllItemTypesUnfiltered() {
        WorkflowDefinition definition = scrumTemplate();

        assertThat(definition.transitions()).hasSize(7);
    }

    @Test
    void initialStateByTypeExposesTheFullDeclaredMap() {
        WorkflowDefinition definition = scrumTemplate();

        assertThat(definition.initialStateByType())
                .containsExactlyInAnyOrderEntriesOf(Map.of("EPIC", "BACKLOG", "STORY", "BACKLOG", "TASK", "TO_DO"));
    }

    @Test
    void statesForTypeReturnsOnlyStatesScopedToThatItemType() {
        WorkflowDefinition definition = scrumTemplate();

        List<WorkflowState> epicStates = definition.statesForType("EPIC");

        assertThat(epicStates).extracting(WorkflowState::key)
                .containsExactlyInAnyOrder("BACKLOG", "TODO", "DOING", "TESTING", "DONE");
    }

    @Test
    void statesForTypeIsScopedDifferentlyForADifferentItemType() {
        WorkflowDefinition definition = scrumTemplate();

        List<WorkflowState> taskStates = definition.statesForType("TASK");

        assertThat(taskStates).extracting(WorkflowState::key)
                .containsExactlyInAnyOrder("TO_DO", "IN_PROGRESS", "BLOCKED", "DONE");
    }

    @Test
    void initialStateForReturnsTheDeclaredInitialStateForThatType() {
        WorkflowDefinition definition = scrumTemplate();

        assertThat(definition.initialStateFor("TASK")).isEqualTo("TO_DO");
        assertThat(definition.initialStateFor("EPIC")).isEqualTo("BACKLOG");
    }

    @Test
    void requireTransitionAllowedAcceptsADeclaredTransitionForThatItemType() {
        WorkflowDefinition definition = scrumTemplate();

        assertThatCode(definition, "STORY", "TODO", "DOING");
    }

    @Test
    void requireTransitionAllowedRejectsAnUndeclaredTransition() {
        WorkflowDefinition definition = scrumTemplate();

        assertThatThrownBy(() -> definition.requireTransitionAllowed("STORY", "TODO", "DONE"))
                .isInstanceOf(IllegalTransitionException.class)
                .hasMessageContaining("TODO")
                .hasMessageContaining("DONE");
    }

    @Test
    void requireTransitionAllowedRejectsATransitionNotScopedToTheGivenItemTypeEvenIfKeysMatch() {
        WorkflowDefinition definition = scrumTemplate();

        // "TO_DO -> IN_PROGRESS" is declared, but only for TASK, not STORY.
        assertThatThrownBy(() -> definition.requireTransitionAllowed("STORY", "TO_DO", "IN_PROGRESS"))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void withIdReturnsACopyWithADifferentIdButSameStatesTransitionsAndInitialStates() {
        WorkflowDefinition template = scrumTemplate();

        WorkflowDefinition provisioned = template.withId(WorkflowDefinitionId.of("wfd-provisioned-1"));

        assertThat(provisioned.id()).isEqualTo(WorkflowDefinitionId.of("wfd-provisioned-1"));
        assertThat(provisioned.name()).isEqualTo(template.name());
        assertThat(provisioned.statesForType("TASK")).extracting(WorkflowState::key)
                .containsExactlyInAnyOrder("TO_DO", "IN_PROGRESS", "BLOCKED", "DONE");
        assertThat(provisioned.initialStateFor("EPIC")).isEqualTo("BACKLOG");
        assertThatCode(provisioned, "STORY", "TODO", "DOING");
    }

    @Test
    void withIdDoesNotMutateTheOriginalDefinition() {
        WorkflowDefinition template = scrumTemplate();

        template.withId(WorkflowDefinitionId.of("wfd-provisioned-2"));

        assertThat(template.id()).isEqualTo(WorkflowDefinitionId.of("wfd-1"));
    }

    private static void assertThatCode(WorkflowDefinition definition, String itemType, String from, String to) {
        definition.requireTransitionAllowed(itemType, from, to);
    }
}

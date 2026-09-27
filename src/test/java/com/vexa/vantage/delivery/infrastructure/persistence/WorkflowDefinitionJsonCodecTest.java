package com.vexa.vantage.delivery.infrastructure.persistence;

import com.vexa.vantage.delivery.domain.StateTransition;
import com.vexa.vantage.delivery.domain.WorkflowDefinition;
import com.vexa.vantage.delivery.domain.WorkflowDefinitionId;
import com.vexa.vantage.delivery.domain.WorkflowState;
import com.vexa.vantage.shared.domain.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba unitaria (sin Spring, sin base de datos) de
 * {@link WorkflowDefinitionJsonCodec}: decodificación del JSONB de
 * {@code workflow_definition.states_and_transitions} (forma real de
 * {@code V2__builtin_workflow_templates.sql}) hacia {@link WorkflowDefinition},
 * y el round-trip inverso (codificación) usado al persistir la copia
 * aprovisionada de una plantilla built-in.
 *
 * <p>{@code itemTypes}/{@code allowedChildTypes} del JSON NO se decodifican
 * (gap ya señalado en el reporte de la fase de aplicación de Phase 4 batch 1:
 * {@link WorkflowDefinition} todavía no modela tipos de item ni hijos
 * permitidos) — se preservan en el JSON crudo de origen, pero se pierden al
 * volver a codificar una copia; documentado como gap conocido, no una
 * regresión nueva de esta fase.
 */
class WorkflowDefinitionJsonCodecTest {

    private static final String SCRUM_TEMPLATE_JSON = """
            {
              "version": 1,
              "itemTypes": [
                { "key": "EPIC", "name": "Epic", "allowedChildTypes": ["STORY"] }
              ],
              "states": [
                { "key": "BACKLOG", "name": "Backlog", "category": "TODO", "order": 0, "appliesTo": ["EPIC", "STORY"] },
                { "key": "TO_DO", "name": "To Do", "category": "TODO", "order": 0, "appliesTo": ["TASK"] }
              ],
              "transitions": [
                { "from": "BACKLOG", "to": "TO_DO", "appliesTo": ["EPIC"] }
              ],
              "initialStateByType": { "EPIC": "BACKLOG", "TASK": "TO_DO" }
            }
            """;

    @Test
    void decodeParsesStatesTransitionsAndInitialStateByTypeFromJsonb() {
        WorkflowDefinition definition = WorkflowDefinitionJsonCodec.decode(
                WorkflowDefinitionId.of("wfd-1"), "Scrum", SCRUM_TEMPLATE_JSON);

        assertThat(definition.id()).isEqualTo(WorkflowDefinitionId.of("wfd-1"));
        assertThat(definition.name()).isEqualTo("Scrum");
        assertThat(definition.states()).containsExactlyInAnyOrder(
                new WorkflowState("BACKLOG", "Backlog", "TODO", 0, Set.of("EPIC", "STORY")),
                new WorkflowState("TO_DO", "To Do", "TODO", 0, Set.of("TASK")));
        assertThat(definition.transitions()).containsExactly(
                new StateTransition("BACKLOG", "TO_DO", Set.of("EPIC")));
        assertThat(definition.initialStateByType()).containsExactlyInAnyOrderEntriesOf(
                Map.of("EPIC", "BACKLOG", "TASK", "TO_DO"));
    }

    @Test
    void encodeThenDecodeRoundTripsStatesTransitionsAndInitialStateByType() {
        WorkflowDefinition original = new WorkflowDefinition(
                WorkflowDefinitionId.of("wfd-1"), "Scrum",
                List.of(new WorkflowState("BACKLOG", "Backlog", "TODO", 0, Set.of("EPIC"))),
                List.of(new StateTransition("BACKLOG", "TODO", Set.of("EPIC"))),
                Map.of("EPIC", "BACKLOG"));

        String json = WorkflowDefinitionJsonCodec.encode(original);
        WorkflowDefinition decoded = WorkflowDefinitionJsonCodec.decode(original.id(), original.name(), json);

        assertThat(decoded.states()).containsExactlyElementsOf(original.states());
        assertThat(decoded.transitions()).containsExactlyElementsOf(original.transitions());
        assertThat(decoded.initialStateByType()).isEqualTo(original.initialStateByType());
    }

    @Test
    void decodeThrowsDomainValidationExceptionOnMalformedJson() {
        assertThatThrownBy(() -> WorkflowDefinitionJsonCodec.decode(WorkflowDefinitionId.of("wfd-1"), "Scrum", "{not-json"))
                .isInstanceOf(DomainValidationException.class);
    }
}

package com.vexa.vantage.delivery.infrastructure.dto;

import com.vexa.vantage.delivery.domain.StateTransition;
import com.vexa.vantage.delivery.domain.WorkflowDefinition;
import com.vexa.vantage.delivery.domain.WorkflowState;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Representación de una {@link WorkflowDefinition} expuesta por
 * {@code ProjectController#getWorkflowDefinition}. Se mapea manualmente
 * desde el dominio puro (sin dependencia de Jackson) en lugar de
 * serializarlo directamente.
 */
public record WorkflowDefinitionResponse(
        String id,
        String name,
        List<StateResponse> states,
        List<TransitionResponse> transitions,
        Map<String, String> initialStateByType) {

    public static WorkflowDefinitionResponse from(WorkflowDefinition definition) {
        return new WorkflowDefinitionResponse(
                definition.id().value(),
                definition.name(),
                definition.states().stream().map(StateResponse::from).toList(),
                definition.transitions().stream().map(TransitionResponse::from).toList(),
                definition.initialStateByType());
    }

    public record StateResponse(String key, String name, String category, int order, Set<String> appliesTo) {

        public static StateResponse from(WorkflowState state) {
            return new StateResponse(state.key(), state.name(), state.category(), state.order(), state.appliesTo());
        }
    }

    public record TransitionResponse(String from, String to, Set<String> appliesTo) {

        public static TransitionResponse from(StateTransition transition) {
            return new TransitionResponse(transition.from(), transition.to(), transition.appliesTo());
        }
    }
}

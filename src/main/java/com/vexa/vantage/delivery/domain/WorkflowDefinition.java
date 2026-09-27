package com.vexa.vantage.delivery.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;

import java.util.List;
import java.util.Map;

/**
 * Representación de dominio, en memoria, de la configuración de workflow de
 * un proyecto (columna JSONB {@code workflow_definition.states_and_transitions}).
 *
 * <p>La decodificación del JSON crudo ocurre en infraestructura, vía
 * {@code WorkflowDefinitionJsonCodec} (fase posterior) — este objeto ya
 * recibe {@link WorkflowState}/{@link StateTransition} construidos, y no
 * conoce Jackson ni ningún formato de serialización.
 *
 * <p>Los estados y transiciones están acotados por tipo de item
 * ({@code appliesTo}), lo que permite que un solo documento de workflow
 * exprese vocabularios de estado distintos para distintos tipos de item
 * (por ejemplo EPIC/STORY vs. TASK) — adenda de reconciliación de diseño
 * "unified state model".
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}).
 */
public final class WorkflowDefinition {

    private final WorkflowDefinitionId id;
    private final String name;
    private final List<WorkflowState> states;
    private final List<StateTransition> transitions;
    private final Map<String, String> initialStateByType;

    public WorkflowDefinition(
            WorkflowDefinitionId id,
            String name,
            List<WorkflowState> states,
            List<StateTransition> transitions,
            Map<String, String> initialStateByType) {
        this.id = id;
        this.name = DomainValidationException.requireNonBlank(name, "WorkflowDefinition.name");
        if (states == null || states.isEmpty()) {
            throw new DomainValidationException("WorkflowDefinition.states must not be null or empty");
        }
        this.states = List.copyOf(states);
        this.transitions = transitions == null ? List.of() : List.copyOf(transitions);
        this.initialStateByType = initialStateByType == null ? Map.of() : Map.copyOf(initialStateByType);
    }

    public WorkflowDefinitionId id() {
        return id;
    }

    public String name() {
        return name;
    }

    /**
     * Devuelve todos los estados declarados, sin acotar por tipo de item —
     * usado por {@code WorkflowDefinitionJsonCodec} (infraestructura) para
     * volver a serializar esta definición completa a JSON, por ejemplo al
     * persistir la copia aprovisionada de una plantilla built-in.
     */
    public List<WorkflowState> states() {
        return states;
    }

    /**
     * Devuelve todas las transiciones declaradas, sin acotar por tipo de
     * item — mismo propósito que {@link #states()}.
     */
    public List<StateTransition> transitions() {
        return transitions;
    }

    /**
     * Devuelve el mapa completo de estado inicial por tipo de item — mismo
     * propósito que {@link #states()}.
     */
    public Map<String, String> initialStateByType() {
        return initialStateByType;
    }

    /**
     * Devuelve los estados declarados cuyo {@code appliesTo} incluye
     * {@code itemType}, en el orden en que fueron declarados.
     */
    public List<WorkflowState> statesForType(String itemType) {
        return states.stream()
                .filter(state -> state.appliesTo().contains(itemType))
                .toList();
    }

    /**
     * Devuelve el estado inicial declarado para {@code itemType}.
     *
     * @throws DomainValidationException si {@code itemType} no tiene un estado inicial declarado
     */
    public String initialStateFor(String itemType) {
        String initial = initialStateByType.get(itemType);
        if (initial == null) {
            throw new DomainValidationException(
                    "WorkflowDefinition has no initialStateByType entry for item type '" + itemType + "'");
        }
        return initial;
    }

    /**
     * Exige que exista una transición declarada de {@code fromState} a
     * {@code toState}, acotada al {@code itemType} dado — una transición
     * declarada para otro tipo de item con las mismas claves de estado no
     * cuenta.
     *
     * @throws IllegalTransitionException si no existe tal transición
     */
    public void requireTransitionAllowed(String itemType, String fromState, String toState) {
        boolean allowed = transitions.stream().anyMatch(transition -> transition.matches(itemType, fromState, toState));
        if (!allowed) {
            throw new IllegalTransitionException(
                    "Transition from '" + fromState + "' to '" + toState + "' is not defined for item type '"
                            + itemType + "'");
        }
    }

    /**
     * Devuelve una copia de esta definición con {@code newId}, conservando
     * nombre, estados, transiciones y estados iniciales por tipo.
     *
     * <p>Usado por {@code ProjectService} al aprovisionar la plantilla
     * built-in "Scrum" para un proyecto nuevo: la plantilla canónica se
     * localiza vía {@code WorkflowDefinitionRepositoryPort#findBuiltInTemplate()}
     * y se copia con un id propio antes de persistirse, en lugar de que
     * múltiples proyectos compartan la misma fila mutable.
     */
    public WorkflowDefinition withId(WorkflowDefinitionId newId) {
        return new WorkflowDefinition(newId, this.name, this.states, this.transitions, this.initialStateByType);
    }
}

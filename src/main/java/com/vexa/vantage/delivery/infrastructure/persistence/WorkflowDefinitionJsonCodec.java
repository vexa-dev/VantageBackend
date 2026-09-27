package com.vexa.vantage.delivery.infrastructure.persistence;

import com.vexa.vantage.delivery.domain.StateTransition;
import com.vexa.vantage.delivery.domain.WorkflowDefinition;
import com.vexa.vantage.delivery.domain.WorkflowDefinitionId;
import com.vexa.vantage.delivery.domain.WorkflowState;
import com.vexa.vantage.shared.domain.DomainValidationException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * Único lugar que conoce la forma real del JSON de
 * {@code workflow_definition.states_and_transitions} (JSONB), tal como lo
 * inserta {@code V2__builtin_workflow_templates.sql}: decodifica ese texto
 * hacia {@link WorkflowDefinition} (dominio puro) y lo codifica de vuelta al
 * persistir.
 *
 * <p>Usa una instancia local de Jackson 3
 * ({@code tools.jackson.databind.json.JsonMapper}), no un bean de Spring —
 * Spring Boot 4.1 expone Jackson 3 ({@code tools.jackson.databind}) como su
 * stack JSON por defecto (ver {@code design-reconciliation-part2}), sin bean
 * {@code ObjectMapper} de Jackson 2 (ese paquete {@code com.fasterxml.jackson}
 * solo sigue en el classpath de forma transitiva vía {@code jjwt-jackson},
 * sin estar wireado al stack JSON de Spring); esta clase no necesita ningún
 * bean de Spring porque no serializa/deserializa nada que pase por Spring
 * MVC, así que una instancia local, igual que en pruebas, es válida y evita
 * acoplar el codec a la configuración de Jackson de Spring.
 *
 * <p><b>Gap conocido (ver informe de esta fase):</b> {@code itemTypes}/
 * {@code allowedChildTypes} del JSON NO se decodifican ni se vuelven a
 * codificar — {@link WorkflowDefinition} todavía no los modela (Phase 4
 * batch 1). Codificar una copia de una plantilla pierde esa sección; no es
 * una regresión nueva, ya que hasta esta fase no existía persistencia en
 * absoluto para {@code WorkflowDefinition}.
 */
public final class WorkflowDefinitionJsonCodec {

    private static final JsonMapper MAPPER = new JsonMapper();

    private WorkflowDefinitionJsonCodec() {
    }

    /**
     * Decodifica el JSON crudo de {@code states_and_transitions} hacia un
     * {@link WorkflowDefinition}, combinado con {@code id}/{@code name}
     * (columnas relacionales propias, fuera del JSONB).
     *
     * @throws DomainValidationException si {@code json} no es un JSON válido
     */
    public static WorkflowDefinition decode(WorkflowDefinitionId id, String name, String json) {
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new DomainValidationException("Invalid workflow_definition JSON: " + e.getMessage());
        }

        List<WorkflowState> states = new ArrayList<>();
        for (JsonNode stateNode : root.path("states")) {
            states.add(new WorkflowState(
                    stateNode.path("key").asText(),
                    stateNode.path("name").asText(),
                    stateNode.path("category").asText(),
                    stateNode.path("order").asInt(),
                    appliesTo(stateNode)));
        }

        List<StateTransition> transitions = new ArrayList<>();
        for (JsonNode transitionNode : root.path("transitions")) {
            transitions.add(new StateTransition(
                    transitionNode.path("from").asText(),
                    transitionNode.path("to").asText(),
                    appliesTo(transitionNode)));
        }

        Map<String, String> initialStateByType = new LinkedHashMap<>();
        root.path("initialStateByType").properties()
                .forEach(entry -> initialStateByType.put(entry.getKey(), entry.getValue().asText()));

        return new WorkflowDefinition(id, name, states, transitions, initialStateByType);
    }

    /**
     * Codifica {@code definition} de vuelta a JSON, en la misma forma que
     * {@link #decode} espera.
     */
    public static String encode(WorkflowDefinition definition) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("version", 1);

        ArrayNode statesNode = root.putArray("states");
        for (WorkflowState state : definition.states()) {
            ObjectNode stateNode = statesNode.addObject();
            stateNode.put("key", state.key());
            stateNode.put("name", state.name());
            stateNode.put("category", state.category());
            stateNode.put("order", state.order());
            ArrayNode appliesTo = stateNode.putArray("appliesTo");
            state.appliesTo().forEach(appliesTo::add);
        }

        ArrayNode transitionsNode = root.putArray("transitions");
        for (StateTransition transition : definition.transitions()) {
            ObjectNode transitionNode = transitionsNode.addObject();
            transitionNode.put("from", transition.from());
            transitionNode.put("to", transition.to());
            ArrayNode appliesTo = transitionNode.putArray("appliesTo");
            transition.appliesTo().forEach(appliesTo::add);
        }

        ObjectNode initialStateByTypeNode = root.putObject("initialStateByType");
        definition.initialStateByType().forEach(initialStateByTypeNode::put);

        return root.toString();
    }

    private static Set<String> appliesTo(JsonNode node) {
        return StreamSupport.stream(node.path("appliesTo").spliterator(), false)
                .map(JsonNode::asText)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}

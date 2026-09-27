package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.WorkflowDefinition;
import com.vexa.vantage.delivery.domain.WorkflowDefinitionId;

import java.util.Optional;

/**
 * Puerto (arquitectura hexagonal) que la capa de aplicación usa para
 * consultar y persistir {@link WorkflowDefinition}, sin conocer el mecanismo
 * de persistencia concreto (JPA, memoria, etc.), implementado en la capa de
 * infraestructura.
 */
public interface WorkflowDefinitionRepositoryPort {

    /**
     * Busca la definición de workflow indicada por id, si existe.
     */
    Optional<WorkflowDefinition> findById(WorkflowDefinitionId id);

    /**
     * Busca la plantilla built-in "Scrum" canónica (fila propiedad del
     * tenant de sistema insertada por {@code V2__builtin_workflow_templates.sql}),
     * usada como origen para aprovisionar una copia propia por proyecto vía
     * {@link com.vexa.vantage.delivery.domain.WorkflowDefinition#withId}.
     */
    Optional<WorkflowDefinition> findBuiltInTemplate();

    /**
     * Guarda (inserta o actualiza) una definición de workflow y devuelve la
     * instancia persistida.
     */
    WorkflowDefinition save(WorkflowDefinition definition);
}

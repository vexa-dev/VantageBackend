package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.WorkItem;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.WorkItemId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto (arquitectura hexagonal) que la capa de aplicación usa para
 * consultar y persistir {@link WorkItem}, sin conocer el mecanismo de
 * persistencia concreto (JPA, memoria, etc.), implementado en la capa de
 * infraestructura.
 *
 * <p>Ninguna firma recibe un {@code TenantId}: el tenant es ambiente vía
 * {@code TenantContext} y lo aplica el adaptador, de modo que quien llama no
 * pueda pasar uno incorrecto (decisión de diseño ya adoptada para
 * {@code ProjectMembershipRepositoryPort} en identity).
 */
public interface WorkItemRepositoryPort {

    /**
     * Busca el work item indicado por id, si existe.
     */
    Optional<WorkItem> findById(WorkItemId id);

    /**
     * Lista los work items del proyecto indicado.
     */
    List<WorkItem> findByProjectId(ProjectId projectId);

    /**
     * Guarda (inserta o actualiza) un work item y devuelve la instancia
     * persistida.
     */
    WorkItem save(WorkItem workItem);
}

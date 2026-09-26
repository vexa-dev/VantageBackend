package com.vexa.vantage.identity.application;

import com.vexa.vantage.identity.domain.ProjectMembership;
import com.vexa.vantage.shared.domain.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto (arquitectura hexagonal) que la capa de aplicación usa para
 * consultar y persistir {@link ProjectMembership}, sin conocer el mecanismo
 * de persistencia concreto (JPA, memoria, etc.), que se implementa en la
 * capa de infraestructura.
 */
public interface ProjectMembershipRepositoryPort {

    /**
     * Lista todas las membresías del proyecto indicado.
     */
    List<ProjectMembership> findByProjectId(String projectId);

    /**
     * Busca la membresía del usuario indicado en el proyecto indicado, si
     * existe.
     */
    Optional<ProjectMembership> findByProjectIdAndUserId(String projectId, UserId userId);

    /**
     * Guarda (inserta o actualiza) una membresía de proyecto.
     */
    void save(ProjectMembership membership);
}

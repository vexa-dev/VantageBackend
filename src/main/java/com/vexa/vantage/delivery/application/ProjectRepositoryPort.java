package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.Project;
import com.vexa.vantage.shared.domain.ProjectId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto (arquitectura hexagonal) que la capa de aplicación usa para
 * consultar y persistir {@link Project}, sin conocer el mecanismo de
 * persistencia concreto (JPA, memoria, etc.), implementado en la capa de
 * infraestructura.
 *
 * <p>Introducido como dependencia de {@link WorkItemService} (para resolver
 * la {@code WorkflowDefinition} activa de un proyecto antes de validar una
 * transición) — {@link ProjectService}, {@link BoardService} y
 * {@link WorkflowDefinitionService} reutilizan el mismo puerto, no uno
 * distinto por servicio.
 */
public interface ProjectRepositoryPort {

    /**
     * Busca el proyecto indicado por id, si existe.
     */
    Optional<Project> findById(ProjectId id);

    /**
     * Busca los proyectos indicados por id — usado por
     * {@code ProjectService#listProjects} para resolver en bloque los
     * proyectos donde el actor tiene membresía, en lugar de una consulta por
     * id (evita N+1).
     */
    List<Project> findByIds(List<ProjectId> ids);

    /**
     * Guarda (inserta o actualiza) un proyecto y devuelve la instancia
     * persistida.
     */
    Project save(Project project);
}

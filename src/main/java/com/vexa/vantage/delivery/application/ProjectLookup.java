package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.Project;
import com.vexa.vantage.shared.domain.ProjectId;

/**
 * Ayudante compartido que centraliza la búsqueda de un {@link Project} por
 * id, lanzando {@link ProjectNotFoundException} cuando no existe.
 *
 * <p>Extraído (refactor 4.20) de la lógica idéntica que
 * {@link WorkItemService}, {@link BoardService} y
 * {@link WorkflowDefinitionService} implementaban cada uno por separado —
 * mismo espíritu que {@code AuthenticatedActorResolver} en
 * {@code identity/infrastructure} (Fase 3, refactor 3.36).
 */
class ProjectLookup {

    private final ProjectRepositoryPort projectRepository;

    ProjectLookup(ProjectRepositoryPort projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * Busca el proyecto indicado por id, o lanza si no existe.
     *
     * @throws ProjectNotFoundException si {@code id} no existe
     */
    Project loadOrThrow(ProjectId id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException("Project '" + id.value() + "' not found"));
    }
}

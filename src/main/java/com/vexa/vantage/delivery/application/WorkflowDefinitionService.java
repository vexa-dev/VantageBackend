package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.Project;
import com.vexa.vantage.delivery.domain.WorkflowDefinition;
import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.UserId;

/**
 * Servicio de aplicación responsable de los casos de uso de solo lectura
 * sobre {@link WorkflowDefinition}: obtener la definición activa de un
 * proyecto.
 *
 * <p>Obtener la definición de workflow exige al menos
 * {@link ProjectRole#VIEWER} en el proyecto (decisión de usuario 2026-09-27).
 */
public class WorkflowDefinitionService {

    private final ProjectLookup projectLookup;
    private final WorkflowDefinitionRepositoryPort workflowDefinitionRepository;
    private final ProjectAuthorization authorization;

    public WorkflowDefinitionService(
            ProjectRepositoryPort projectRepository,
            WorkflowDefinitionRepositoryPort workflowDefinitionRepository,
            ProjectRoleResolverPort projectRoleResolver) {
        this.projectLookup = new ProjectLookup(projectRepository);
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.authorization = new ProjectAuthorization(projectRoleResolver);
    }

    /**
     * Obtiene la {@link WorkflowDefinition} activa del proyecto indicado.
     *
     * @throws ProjectNotFoundException             si {@code projectId} no existe
     * @throws com.vexa.vantage.identity.domain.InsufficientPermissionException
     *                                                si {@code actorId} no tiene al menos rol VIEWER en el proyecto
     * @throws WorkflowDefinitionNotFoundException  si la definición de workflow del proyecto ya no existe
     */
    public WorkflowDefinition getByProject(ProjectId projectId, UserId actorId) {
        Project project = projectLookup.loadOrThrow(projectId);
        authorization.requireAtLeast(projectId, actorId, ProjectRole.VIEWER);
        return workflowDefinitionRepository.findById(project.workflowDefinitionId())
                .orElseThrow(() -> new WorkflowDefinitionNotFoundException(
                        "WorkflowDefinition '" + project.workflowDefinitionId().value() + "' not found"));
    }
}

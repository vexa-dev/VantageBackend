package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.Project;
import com.vexa.vantage.delivery.domain.TShirtSize;
import com.vexa.vantage.delivery.domain.WorkItem;
import com.vexa.vantage.delivery.domain.WorkflowDefinition;
import com.vexa.vantage.delivery.domain.WorkflowDefinitionId;
import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.UserId;
import com.vexa.vantage.shared.domain.WorkItemId;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicación responsable de los casos de uso sobre
 * {@link WorkItem}: creación, cambio de estado (delegando la validación de la
 * transición a {@link WorkflowDefinition}) y reparenting vía
 * {@code parent_work_item_id}.
 *
 * <p>Reparenting NO valida tipos de hijo permitidos
 * ({@code allowedChildTypes} del JSON de plantilla de workflow): ni el spec
 * ({@code workflow-engine-domain}, requisito "Work Item Hierarchy") ni el
 * diseño exigen esa validación en esta capa — se documenta como decisión
 * deliberada, no como un olvido, en el reporte de esta fase de aplicación.
 *
 * <p>Los tres casos de uso exigen al menos {@link ProjectRole#MEMBER} en el
 * proyecto dueño del work item (decisión de usuario 2026-09-27: la
 * autorización de generalized-rbac se aplica en la capa de aplicación de
 * delivery, no en HTTP), verificado vía {@link ProjectAuthorization}
 * DESPUÉS de resolver la existencia del work item/proyecto — un work item o
 * proyecto inexistente sigue respondiendo 404, no 403, exactamente como antes
 * de esta fase.
 *
 * <p>Ningún método recibe {@code TenantId}: el tenant es ambiente vía
 * {@code TenantContext} y lo aplican los adaptadores de infraestructura
 * (fase posterior), igual que en {@code identity/application}.
 */
public class WorkItemService {

    private final WorkItemRepositoryPort workItemRepository;
    private final ProjectLookup projectLookup;
    private final WorkflowDefinitionRepositoryPort workflowDefinitionRepository;
    private final ProjectAuthorization authorization;

    public WorkItemService(
            WorkItemRepositoryPort workItemRepository,
            ProjectRepositoryPort projectRepository,
            WorkflowDefinitionRepositoryPort workflowDefinitionRepository,
            ProjectRoleResolverPort projectRoleResolver) {
        this.workItemRepository = workItemRepository;
        this.projectLookup = new ProjectLookup(projectRepository);
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.authorization = new ProjectAuthorization(projectRoleResolver);
    }

    /**
     * Crea un nuevo work item asociado al proyecto indicado.
     *
     * @throws ProjectNotFoundException si {@code projectId} no existe
     * @throws com.vexa.vantage.identity.domain.InsufficientPermissionException
     *                                   si {@code actorId} no tiene al menos rol MEMBER en el proyecto
     */
    public WorkItem createWorkItem(
            ProjectId projectId,
            UserId actorId,
            String type,
            String initialState,
            String title,
            String description,
            TShirtSize tshirtSize,
            Integer businessValue,
            Integer urgency,
            Integer storyPoints) {
        projectLookup.loadOrThrow(projectId);
        authorization.requireAtLeast(projectId, actorId, ProjectRole.MEMBER);

        WorkItem workItem = new WorkItem(null, projectId, type, initialState, title, businessValue, urgency, storyPoints);
        if (description != null) {
            workItem.changeDescription(description);
        }
        if (tshirtSize != null) {
            workItem.changeTshirtSize(tshirtSize);
        }
        return workItemRepository.save(workItem);
    }

    /**
     * Intenta mover el work item indicado a {@code newState}, delegando la
     * validación de la transición a la {@link WorkflowDefinition} activa del
     * proyecto dueño del work item.
     *
     * @throws WorkItemNotFoundException           si {@code workItemId} no existe
     * @throws ProjectNotFoundException             si el proyecto dueño ya no existe
     * @throws com.vexa.vantage.identity.domain.InsufficientPermissionException
     *                                               si {@code actorId} no tiene al menos rol MEMBER en el proyecto
     * @throws WorkflowDefinitionNotFoundException  si la definición de workflow del proyecto ya no existe
     * @throws com.vexa.vantage.delivery.domain.IllegalTransitionException
     *                                               si la transición no está definida
     */
    public WorkItem transitionState(WorkItemId workItemId, UserId actorId, String newState) {
        WorkItem workItem = loadWorkItemOrThrow(workItemId);
        Project project = projectLookup.loadOrThrow(workItem.projectId());
        authorization.requireAtLeast(project.id(), actorId, ProjectRole.MEMBER);
        WorkflowDefinition definition = loadWorkflowDefinitionOrThrow(project.workflowDefinitionId());

        workItem.transitionTo(newState, definition);
        return workItemRepository.save(workItem);
    }

    /**
     * Reasigna (o limpia, con {@code null}) el padre jerárquico del work item
     * indicado.
     *
     * @throws WorkItemNotFoundException                                     si {@code workItemId} no existe
     * @throws com.vexa.vantage.identity.domain.InsufficientPermissionException
     *                                                                        si {@code actorId} no tiene al menos rol MEMBER en el proyecto
     * @throws com.vexa.vantage.shared.domain.DomainValidationException si {@code newParentId} es el mismo work item
     */
    public WorkItem reparent(WorkItemId workItemId, UserId actorId, WorkItemId newParentId) {
        WorkItem workItem = loadWorkItemOrThrow(workItemId);
        authorization.requireAtLeast(workItem.projectId(), actorId, ProjectRole.MEMBER);

        workItem.changeParent(newParentId);
        return workItemRepository.save(workItem);
    }

    /**
     * Lista los work items del proyecto indicado — exige al menos
     * {@link ProjectRole#VIEWER}, el rol de proyecto más bajo, ya que es un
     * caso de uso de solo lectura (mismo criterio que {@code BoardService#viewBoard}).
     *
     * @throws ProjectNotFoundException si {@code projectId} no existe
     * @throws com.vexa.vantage.identity.domain.InsufficientPermissionException
     *                                   si {@code actorId} no tiene al menos rol VIEWER en el proyecto
     */
    public List<WorkItem> listByProject(ProjectId projectId, UserId actorId) {
        projectLookup.loadOrThrow(projectId);
        authorization.requireAtLeast(projectId, actorId, ProjectRole.VIEWER);
        return workItemRepository.findByProjectId(projectId);
    }

    /**
     * Aplica, en una sola operación autorizada una sola vez, todos los
     * cambios presentes en {@code command} sobre el work item indicado:
     * transición de estado (delegada a la {@link WorkflowDefinition} activa
     * del proyecto), reparenting, reasignación de sprint, edición de
     * título/descripción/priorización/talla de camiseta y reasignación de
     * responsable — respaldo de {@code PATCH /api/v1/work-items/{id}}.
     *
     * @throws WorkItemNotFoundException            si {@code workItemId} no existe
     * @throws ProjectNotFoundException              si el proyecto dueño ya no existe
     * @throws com.vexa.vantage.identity.domain.InsufficientPermissionException
     *                                                si {@code actorId} no tiene al menos rol MEMBER en el proyecto
     * @throws WorkflowDefinitionNotFoundException   si {@code command.state()} está presente y la definición de workflow del proyecto ya no existe
     * @throws com.vexa.vantage.delivery.domain.IllegalTransitionException
     *                                                si {@code command.state()} está presente y la transición no está definida
     */
    @Transactional
    public WorkItem patch(WorkItemId workItemId, UserId actorId, WorkItemPatchCommand command) {
        WorkItem workItem = loadWorkItemOrThrow(workItemId);
        authorization.requireAtLeast(workItem.projectId(), actorId, ProjectRole.MEMBER);

        if (command.state() != null) {
            Project project = projectLookup.loadOrThrow(workItem.projectId());
            WorkflowDefinition definition = loadWorkflowDefinitionOrThrow(project.workflowDefinitionId());
            workItem.transitionTo(command.state(), definition);
        }
        if (command.title() != null) {
            workItem.changeTitle(command.title());
        }
        if (command.description() != null) {
            workItem.changeDescription(command.description());
        }
        if (command.businessValue() != null || command.urgency() != null || command.storyPoints() != null) {
            workItem.updatePrioritization(
                    command.businessValue() != null ? command.businessValue() : workItem.businessValue(),
                    command.urgency() != null ? command.urgency() : workItem.urgency(),
                    command.storyPoints() != null ? command.storyPoints() : workItem.storyPoints());
        }
        if (command.tshirtSize() != null) {
            workItem.changeTshirtSize(command.tshirtSize());
        }
        if (command.parentProvided()) {
            workItem.changeParent(command.parentWorkItemId());
        }
        if (command.sprintProvided()) {
            workItem.assignToSprint(command.sprintWorkItemId());
        }
        if (command.assigneeProvided()) {
            if (command.assigneeId() != null) {
                workItem.assignTo(command.assigneeId());
            } else {
                workItem.clearAssignee();
            }
        }

        return workItemRepository.save(workItem);
    }

    private WorkItem loadWorkItemOrThrow(WorkItemId id) {
        return workItemRepository.findById(id)
                .orElseThrow(() -> new WorkItemNotFoundException("WorkItem '" + id.value() + "' not found"));
    }

    private WorkflowDefinition loadWorkflowDefinitionOrThrow(WorkflowDefinitionId id) {
        return workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new WorkflowDefinitionNotFoundException(
                        "WorkflowDefinition '" + id.value() + "' not found"));
    }
}

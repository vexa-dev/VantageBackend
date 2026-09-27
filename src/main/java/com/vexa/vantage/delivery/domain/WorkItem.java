package com.vexa.vantage.delivery.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.UserId;
import com.vexa.vantage.shared.domain.WorkItemId;

import java.util.Optional;

/**
 * Entidad de dominio que representa un elemento de trabajo genérico (Epic,
 * Story, Task, Sprint, Issue, ...), mapeada a la tabla {@code work_item}.
 *
 * <p>Reemplaza las antiguas entidades fijas {@code Story}/{@code Issue}:
 * expone un conjunto de campos consistente sin importar la plantilla de
 * workflow del proyecto dueño. Dos auto-referencias opcionales conviven sin
 * confundirse: {@code parentWorkItemId} lleva la jerarquía de descomposición
 * (Epic→Story→Task); {@code sprintWorkItemId} lleva, de forma independiente,
 * la pertenencia a un sprint.
 *
 * <p>Las transiciones de estado se delegan a {@link WorkflowDefinition}
 * (dominio puro, sin persistencia) — este objeto nunca decide por sí mismo
 * si una transición es válida. El puntaje WSJF ({@link #wsjfScore()}) se
 * calcula bajo demanda a partir de los campos actuales; nunca se guarda un
 * resultado previamente calculado.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}).
 */
public final class WorkItem {

    private final WorkItemId id;
    private final ProjectId projectId;
    private final String type;
    private String state;
    private String title;
    private String description;
    private Integer businessValue;
    private Integer urgency;
    private Integer storyPoints;
    private WorkItemId parentWorkItemId;
    private WorkItemId sprintWorkItemId;
    private UserId assigneeId;
    private TShirtSize tshirtSize;

    public WorkItem(
            WorkItemId id,
            ProjectId projectId,
            String type,
            String state,
            String title,
            Integer businessValue,
            Integer urgency,
            Integer storyPoints) {
        this.id = id;
        this.projectId = projectId;
        this.type = DomainValidationException.requireNonBlank(type, "WorkItem.type");
        this.state = DomainValidationException.requireNonBlank(state, "WorkItem.state");
        this.title = DomainValidationException.requireNonBlank(title, "WorkItem.title");
        this.businessValue = businessValue;
        this.urgency = urgency;
        this.storyPoints = storyPoints;
    }

    public WorkItemId id() {
        return id;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public String type() {
        return type;
    }

    public String state() {
        return state;
    }

    public String title() {
        return title;
    }

    /**
     * Actualiza el título de este work item.
     *
     * @throws DomainValidationException si {@code title} es {@code null} o está en blanco
     */
    public void changeTitle(String title) {
        this.title = DomainValidationException.requireNonBlank(title, "WorkItem.title");
    }

    public String description() {
        return description;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public Integer businessValue() {
        return businessValue;
    }

    public Integer urgency() {
        return urgency;
    }

    public Integer storyPoints() {
        return storyPoints;
    }

    public Optional<WorkItemId> parentWorkItemId() {
        return Optional.ofNullable(parentWorkItemId);
    }

    public Optional<WorkItemId> sprintWorkItemId() {
        return Optional.ofNullable(sprintWorkItemId);
    }

    public Optional<UserId> assigneeId() {
        return Optional.ofNullable(assigneeId);
    }

    public Optional<TShirtSize> tshirtSize() {
        return Optional.ofNullable(tshirtSize);
    }

    /**
     * Asigna (o limpia, con {@code null}) la estimación en talla de camiseta
     * de este work item.
     */
    public void changeTshirtSize(TShirtSize tshirtSize) {
        this.tshirtSize = tshirtSize;
    }

    public void assignTo(UserId assigneeId) {
        this.assigneeId = assigneeId;
    }

    public void clearAssignee() {
        this.assigneeId = null;
    }

    /**
     * Reasigna (o limpia, con {@code null}) el padre jerárquico de este work
     * item.
     *
     * @throws DomainValidationException si {@code newParentId} es este mismo
     *                                    work item (refleja el
     *                                    {@code CHECK ck_work_item_not_self_parent})
     */
    public void changeParent(WorkItemId newParentId) {
        if (newParentId != null && newParentId.equals(this.id)) {
            throw new DomainValidationException("WorkItem cannot be its own parent");
        }
        this.parentWorkItemId = newParentId;
    }

    /**
     * Asigna (o limpia, con {@code null}) el sprint al que pertenece este
     * work item, de forma independiente de {@link #changeParent(WorkItemId)}.
     *
     * @throws DomainValidationException si {@code sprintId} es este mismo
     *                                    work item (refleja el
     *                                    {@code CHECK ck_work_item_not_self_sprint})
     */
    public void assignToSprint(WorkItemId sprintId) {
        if (sprintId != null && sprintId.equals(this.id)) {
            throw new DomainValidationException("WorkItem cannot be its own sprint");
        }
        this.sprintWorkItemId = sprintId;
    }

    /**
     * Actualiza los tres insumos de priorización WSJF a la vez.
     */
    public void updatePrioritization(Integer businessValue, Integer urgency, Integer storyPoints) {
        this.businessValue = businessValue;
        this.urgency = urgency;
        this.storyPoints = storyPoints;
    }

    /**
     * Intenta mover este work item a {@code newState}, delegando la
     * validación a {@code definition} acotada por el {@code type} de este
     * work item. Si la transición no está definida, el estado permanece sin
     * cambios (la excepción se lanza antes de mutar {@code this.state}).
     *
     * @throws IllegalTransitionException si {@code definition} no declara una
     *                                     transición de {@code this.state} a
     *                                     {@code newState} para {@code this.type}
     */
    public void transitionTo(String newState, WorkflowDefinition definition) {
        definition.requireTransitionAllowed(this.type, this.state, newState);
        this.state = newState;
    }

    /**
     * Calcula el puntaje WSJF bajo demanda a partir de los campos actuales
     * de priorización. Nunca se persiste ni se cachea entre llamadas.
     */
    public WsjfScore wsjfScore() {
        return WsjfScore.compute(businessValue, urgency, storyPoints);
    }
}

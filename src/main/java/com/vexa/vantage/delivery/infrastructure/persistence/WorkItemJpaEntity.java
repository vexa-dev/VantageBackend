package com.vexa.vantage.delivery.infrastructure.persistence;

import com.vexa.vantage.delivery.domain.TShirtSize;
import com.vexa.vantage.shared.infrastructure.TenantFilterAspect;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.Instant;

/**
 * Entidad JPA que mapea la tabla {@code work_item}, separada del objeto de
 * dominio puro {@code WorkItem}.
 *
 * <p>{@code parentWorkItemId}/{@code sprintWorkItemId}/{@code assigneeId} son
 * columnas {@code BIGINT} nulas — auto-referencias (las dos primeras) y
 * referencia a {@code app_user} (la tercera), mapeadas como {@code Long} en
 * lugar de una asociación JPA {@code @ManyToOne}: el dominio ya modela estos
 * campos como identificadores planos ({@code Optional<WorkItemId>}/
 * {@code Optional<UserId>}), no como referencias a objetos cargados, y una
 * asociación real introduciría acoplamiento y riesgo de carga perezosa que el
 * diseño de esta re-arquitectura explícitamente evita ("mapeo de DTO evita
 * excepciones de carga perezosa").
 */
@Entity
@Table(name = "work_item")
@FilterDef(name = TenantFilterAspect.TENANT_FILTER_NAME, parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = TenantFilterAspect.TENANT_FILTER_NAME, condition = "tenant_id = CAST(:tenantId AS BIGINT)")
public class WorkItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "parent_work_item_id")
    private Long parentWorkItemId;

    @Column(name = "sprint_work_item_id")
    private Long sprintWorkItemId;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 32)
    private String state;

    @Column(nullable = false, length = 255)
    private String title;

    @Column
    private String description;

    @Column(name = "business_value")
    private Integer businessValue;

    @Column
    private Integer urgency;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Enumerated(EnumType.STRING)
    @Column(name = "tshirt_size", length = 8)
    private TShirtSize tshirtSize;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected WorkItemJpaEntity() {
        // Requerido por JPA.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getParentWorkItemId() {
        return parentWorkItemId;
    }

    public void setParentWorkItemId(Long parentWorkItemId) {
        this.parentWorkItemId = parentWorkItemId;
    }

    public Long getSprintWorkItemId() {
        return sprintWorkItemId;
    }

    public void setSprintWorkItemId(Long sprintWorkItemId) {
        this.sprintWorkItemId = sprintWorkItemId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getBusinessValue() {
        return businessValue;
    }

    public void setBusinessValue(Integer businessValue) {
        this.businessValue = businessValue;
    }

    public Integer getUrgency() {
        return urgency;
    }

    public void setUrgency(Integer urgency) {
        this.urgency = urgency;
    }

    public Integer getStoryPoints() {
        return storyPoints;
    }

    public void setStoryPoints(Integer storyPoints) {
        this.storyPoints = storyPoints;
    }

    public TShirtSize getTshirtSize() {
        return tshirtSize;
    }

    public void setTshirtSize(TShirtSize tshirtSize) {
        this.tshirtSize = tshirtSize;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

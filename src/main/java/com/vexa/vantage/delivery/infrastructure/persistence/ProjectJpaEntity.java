package com.vexa.vantage.delivery.infrastructure.persistence;

import com.vexa.vantage.delivery.domain.ProjectStatus;
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
 * Entidad JPA que mapea la tabla {@code project}, separada del objeto de
 * dominio puro {@code Project}.
 *
 * <p>Declara el filtro Hibernate {@code tenantFilter} (mismo nombre/mecanismo
 * que {@code AppUserJpaEntity} en Phase 3), habilitado por
 * {@code TenantFilterAspect} alrededor de cada llamada al adapter.
 */
@Entity
@Table(name = "project")
@FilterDef(name = TenantFilterAspect.TENANT_FILTER_NAME, parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = TenantFilterAspect.TENANT_FILTER_NAME, condition = "tenant_id = CAST(:tenantId AS BIGINT)")
public class ProjectJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column
    private String description;

    @Column(name = "workflow_definition_id", nullable = false)
    private Long workflowDefinitionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ProjectStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected ProjectJpaEntity() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public void setWorkflowDefinitionId(Long workflowDefinitionId) {
        this.workflowDefinitionId = workflowDefinitionId;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package com.vexa.vantage.delivery.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.TenantId;

/**
 * Entidad de dominio que representa un proyecto, mapeada a la tabla
 * {@code project}.
 *
 * <p>{@code status} nace en {@link ProjectStatus#ACTIVE} y solo cambia a
 * {@link ProjectStatus#ARCHIVED} mediante {@link #archive()} (y de vuelta
 * mediante {@link #restore()}) — un proyecto archivado se excluye de los
 * listados por defecto pero sigue siendo alcanzable por id directo (decidido
 * en la resolución final de preguntas abiertas).
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}).
 */
public final class Project {

    private final ProjectId id;
    private final TenantId tenantId;
    private String name;
    private String description;
    private WorkflowDefinitionId workflowDefinitionId;
    private ProjectStatus status;

    public Project(
            ProjectId id,
            TenantId tenantId,
            String name,
            String description,
            WorkflowDefinitionId workflowDefinitionId) {
        this(id, tenantId, name, description, workflowDefinitionId, ProjectStatus.ACTIVE);
    }

    public Project(
            ProjectId id,
            TenantId tenantId,
            String name,
            String description,
            WorkflowDefinitionId workflowDefinitionId,
            ProjectStatus status) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = DomainValidationException.requireNonBlank(name, "Project.name");
        this.description = description;
        this.workflowDefinitionId = workflowDefinitionId;
        this.status = status != null ? status : ProjectStatus.ACTIVE;
    }

    public ProjectId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public WorkflowDefinitionId workflowDefinitionId() {
        return workflowDefinitionId;
    }

    public ProjectStatus status() {
        return status;
    }

    public boolean isArchived() {
        return status == ProjectStatus.ARCHIVED;
    }

    public void archive() {
        this.status = ProjectStatus.ARCHIVED;
    }

    public void restore() {
        this.status = ProjectStatus.ACTIVE;
    }
}

package com.vexa.vantage.identity.infrastructure.persistence;

import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.identity.domain.ScrumLabel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entidad JPA que mapea la tabla {@code project_membership}, separada del
 * objeto de dominio puro {@code ProjectMembership}.
 *
 * <p>{@code projectId} se persiste como {@code Long} (columna
 * {@code BIGINT}), tal como exige el esquema, aunque el objeto de dominio lo
 * modela como {@code String} porque el contexto delimitado de
 * delivery/colaboración (dueño real de {@code project}) todavía no existe;
 * el mapper convierte entre ambas representaciones.
 */
@Entity
@Table(name = "project_membership")
public class ProjectMembershipJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProjectRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "scrum_label", length = 16)
    private ScrumLabel scrumLabel;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected ProjectMembershipJpaEntity() {
        // Requerido por JPA.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public ProjectRole getRole() {
        return role;
    }

    public void setRole(ProjectRole role) {
        this.role = role;
    }

    public ScrumLabel getScrumLabel() {
        return scrumLabel;
    }

    public void setScrumLabel(ScrumLabel scrumLabel) {
        this.scrumLabel = scrumLabel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

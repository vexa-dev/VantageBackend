package com.vexa.vantage.identity.domain;

import com.vexa.vantage.shared.domain.UserId;

import java.util.Optional;

/**
 * Entidad de dominio que representa la membresía de un usuario en un
 * proyecto, mapeada a la tabla {@code project_membership}.
 *
 * <p>Empareja un {@link ProjectRole} obligatorio con un {@link ScrumLabel}
 * opcional. El identificador del proyecto se modela como {@code String}
 * porque el contexto delimitado de delivery/colaboración (dueño de
 * {@code project}) todavía no existe en fases anteriores; este contexto de
 * identidad solo necesita la referencia cruda.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}).
 */
public final class ProjectMembership {

    private final String projectId;
    private final UserId userId;
    private final ProjectRole role;
    private final ScrumLabel scrumLabel;

    public ProjectMembership(String projectId, UserId userId, ProjectRole role, ScrumLabel scrumLabel) {
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
        this.scrumLabel = scrumLabel;
    }

    public String projectId() {
        return projectId;
    }

    public UserId userId() {
        return userId;
    }

    public ProjectRole role() {
        return role;
    }

    public Optional<ScrumLabel> scrumLabel() {
        return Optional.ofNullable(scrumLabel);
    }
}

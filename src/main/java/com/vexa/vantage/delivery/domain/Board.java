package com.vexa.vantage.delivery.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;
import com.vexa.vantage.shared.domain.ProjectId;

/**
 * Entidad de dominio que representa una vista de tablero guardada, mapeada
 * a la tabla {@code board}.
 *
 * <p>{@code board} es deliberadamente delgado: solo nombre + asociación a
 * proyecto. Las columnas (estados) se derivan en tiempo de lectura a partir
 * de la {@link WorkflowDefinition} activa del proyecto, filtradas por tipo
 * de item según la vista — nunca se guarda una lista de columnas aquí, para
 * no duplicar y desincronizarse de la definición de workflow. Un proyecto es
 * completamente usable con cero filas de {@code board}.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}).
 */
public final class Board {

    private final BoardId id;
    private final ProjectId projectId;
    private String name;

    public Board(BoardId id, ProjectId projectId, String name) {
        this.id = id;
        if (projectId == null) {
            throw new DomainValidationException("Board.projectId value must not be null or blank");
        }
        this.projectId = projectId;
        this.name = DomainValidationException.requireNonBlank(name, "Board.name");
    }

    public BoardId id() {
        return id;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public String name() {
        return name;
    }

    public void rename(String newName) {
        this.name = DomainValidationException.requireNonBlank(newName, "Board.name");
    }
}

package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.BoardId;
import com.vexa.vantage.shared.domain.ProjectId;

import java.util.List;
import java.util.Optional;

/**
 * Resultado de {@link BoardService#viewBoard}: columnas derivadas en tiempo
 * de lectura a partir de la {@code WorkflowDefinition} activa del proyecto.
 *
 * <p>{@code boardId} está vacío cuando el proyecto todavía no tiene una fila
 * de {@code board} guardada — en ese caso se resuelve un tablero default
 * (decisión de diseño: "GET .../board resolves the default").
 */
public final class BoardView {

    private final BoardId boardId;
    private final String boardName;
    private final ProjectId projectId;
    private final List<BoardColumn> columns;

    public BoardView(BoardId boardId, String boardName, ProjectId projectId, List<BoardColumn> columns) {
        this.boardId = boardId;
        this.boardName = boardName;
        this.projectId = projectId;
        this.columns = columns == null ? List.of() : List.copyOf(columns);
    }

    public Optional<BoardId> boardId() {
        return Optional.ofNullable(boardId);
    }

    public String boardName() {
        return boardName;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public List<BoardColumn> columns() {
        return columns;
    }
}

package com.vexa.vantage.delivery.infrastructure.dto;

import com.vexa.vantage.delivery.application.BoardColumn;
import com.vexa.vantage.delivery.application.BoardView;

import java.util.List;

/**
 * Representación de la vista de tablero expuesta por
 * {@code BoardController}. Se mapea manualmente desde {@link BoardView}
 * (capa de aplicación) en lugar de serializarlo directamente.
 */
public record BoardResponse(String boardId, String boardName, String projectId, List<ColumnResponse> columns) {

    public static BoardResponse from(BoardView view) {
        return new BoardResponse(
                view.boardId().map(id -> id.value()).orElse(null),
                view.boardName(),
                view.projectId().value(),
                view.columns().stream().map(ColumnResponse::from).toList());
    }

    public record ColumnResponse(
            String stateKey, String stateName, String category, int order, List<WorkItemResponse> items) {

        public static ColumnResponse from(BoardColumn column) {
            return new ColumnResponse(
                    column.state().key(),
                    column.state().name(),
                    column.state().category(),
                    column.state().order(),
                    column.items().stream().map(WorkItemResponse::from).toList());
        }
    }
}

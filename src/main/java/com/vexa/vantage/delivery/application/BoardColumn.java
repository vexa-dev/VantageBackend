package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.WorkItem;
import com.vexa.vantage.delivery.domain.WorkflowState;

import java.util.List;

/**
 * Una columna del tablero: un {@link WorkflowState} declarado y los work
 * items del proyecto actualmente en ese estado (para el tipo de item
 * consultado). Se incluye aunque {@code items} esté vacío — el tablero
 * siempre muestra todas las columnas aplicables, no solo las pobladas.
 */
public record BoardColumn(WorkflowState state, List<WorkItem> items) {

    public BoardColumn {
        items = items == null ? List.of() : List.copyOf(items);
    }
}

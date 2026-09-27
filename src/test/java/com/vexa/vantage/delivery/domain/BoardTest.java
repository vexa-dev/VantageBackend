package com.vexa.vantage.delivery.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;
import com.vexa.vantage.shared.domain.ProjectId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica los invariantes de asociación proyecto-tablero de {@link Board}.
 *
 * <p>{@code board} es una vista guardada: solo lleva nombre + proyecto. Las
 * columnas se derivan en tiempo de lectura a partir de la
 * {@link WorkflowDefinition} activa del proyecto (capa de aplicación, fuera
 * del alcance de este test de dominio) — un proyecto es completamente
 * usable con cero filas de {@code board}.
 */
class BoardTest {

    private static final ProjectId PROJECT_ID = ProjectId.of("project-1");
    private static final BoardId BOARD_ID = BoardId.of("board-1");

    @Test
    void createsBoardAssociatedWithItsProject() {
        Board board = new Board(BOARD_ID, PROJECT_ID, "Sprint Board");

        assertThat(board.id()).isEqualTo(BOARD_ID);
        assertThat(board.projectId()).isEqualTo(PROJECT_ID);
        assertThat(board.name()).isEqualTo("Sprint Board");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new Board(BOARD_ID, PROJECT_ID, "   "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Board.name");
    }

    @Test
    void rejectsMissingProjectAssociation() {
        assertThatThrownBy(() -> new Board(BOARD_ID, null, "Sprint Board"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Board.projectId");
    }

    @Test
    void boardCanBeRenamed() {
        Board board = new Board(BOARD_ID, PROJECT_ID, "Sprint Board");

        board.rename("Kanban Board");

        assertThat(board.name()).isEqualTo("Kanban Board");
    }
}

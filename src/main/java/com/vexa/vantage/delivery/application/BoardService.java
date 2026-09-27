package com.vexa.vantage.delivery.application;

import com.vexa.vantage.delivery.domain.Board;
import com.vexa.vantage.delivery.domain.Project;
import com.vexa.vantage.delivery.domain.WorkItem;
import com.vexa.vantage.delivery.domain.WorkflowDefinition;
import com.vexa.vantage.delivery.domain.WorkflowDefinitionId;
import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de aplicación responsable de la vista de tablero: agrupa los work
 * items de un proyecto por los estados declarados (para el tipo de item
 * indicado) en la {@link WorkflowDefinition} activa del proyecto.
 *
 * <p>{@code board} es una vista guardada delgada — si el proyecto no tiene
 * una fila de {@code board}, se resuelve un tablero default con el nombre
 * genérico {@code "Board"} en lugar de fallar (decisión de diseño: "GET
 * .../board resolves the default").
 *
 * <p>Ver un tablero exige al menos {@link ProjectRole#VIEWER} en el
 * proyecto — el rol de proyecto más bajo, ya que es un caso de uso de solo
 * lectura (decisión de usuario 2026-09-27).
 */
public class BoardService {

    private static final String DEFAULT_BOARD_NAME = "Board";

    private final BoardRepositoryPort boardRepository;
    private final ProjectLookup projectLookup;
    private final WorkflowDefinitionRepositoryPort workflowDefinitionRepository;
    private final WorkItemRepositoryPort workItemRepository;
    private final ProjectAuthorization authorization;

    public BoardService(
            BoardRepositoryPort boardRepository,
            ProjectRepositoryPort projectRepository,
            WorkflowDefinitionRepositoryPort workflowDefinitionRepository,
            WorkItemRepositoryPort workItemRepository,
            ProjectRoleResolverPort projectRoleResolver) {
        this.boardRepository = boardRepository;
        this.projectLookup = new ProjectLookup(projectRepository);
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workItemRepository = workItemRepository;
        this.authorization = new ProjectAuthorization(projectRoleResolver);
    }

    /**
     * Construye la vista de tablero del proyecto indicado, acotada al tipo
     * de item {@code itemType} — todas las columnas aplicables se incluyen,
     * incluso vacías.
     *
     * @throws ProjectNotFoundException            si {@code projectId} no existe
     * @throws com.vexa.vantage.identity.domain.InsufficientPermissionException
     *                                               si {@code actorId} no tiene al menos rol VIEWER en el proyecto
     * @throws WorkflowDefinitionNotFoundException si la definición de workflow del proyecto ya no existe
     */
    public BoardView viewBoard(ProjectId projectId, UserId actorId, String itemType) {
        Project project = projectLookup.loadOrThrow(projectId);
        authorization.requireAtLeast(projectId, actorId, ProjectRole.VIEWER);
        WorkflowDefinition definition = loadWorkflowDefinitionOrThrow(project.workflowDefinitionId());
        List<WorkItem> items = workItemRepository.findByProjectId(projectId);

        List<BoardColumn> columns = definition.statesForType(itemType).stream()
                .map(state -> new BoardColumn(
                        state,
                        items.stream()
                                .filter(item -> item.type().equals(itemType) && item.state().equals(state.key()))
                                .toList()))
                .toList();

        Optional<Board> board = boardRepository.findByProjectId(projectId);
        return new BoardView(
                board.map(Board::id).orElse(null),
                board.map(Board::name).orElse(DEFAULT_BOARD_NAME),
                projectId,
                columns);
    }

    private WorkflowDefinition loadWorkflowDefinitionOrThrow(WorkflowDefinitionId id) {
        return workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new WorkflowDefinitionNotFoundException(
                        "WorkflowDefinition '" + id.value() + "' not found"));
    }
}

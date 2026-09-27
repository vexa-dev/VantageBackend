package com.vexa.vantage.delivery.infrastructure;

import com.vexa.vantage.delivery.application.BoardService;
import com.vexa.vantage.delivery.application.BoardView;
import com.vexa.vantage.delivery.infrastructure.dto.BoardResponse;
import com.vexa.vantage.identity.domain.AppUser;
import com.vexa.vantage.identity.infrastructure.AuthenticatedActorResolver;
import com.vexa.vantage.shared.domain.ProjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone {@code GET /api/v1/projects/{id}/board}: la vista de tablero del
 * proyecto indicado, acotada al tipo de item {@code itemType}.
 *
 * <p><b>Decisión flaggeada</b> (spec/diseño no definen un tipo de item
 * default para el tablero — el diseño muestra dos vistas con conjuntos de
 * estados distintos, backlog (EPIC/STORY) y Kanban (TASK), sin indicar cuál
 * es "la" vista por defecto): {@code itemType} es un query param
 * OBLIGATORIO, sin valor por defecto, para no inventar una preferencia que
 * ni el spec ni el diseño exigen.
 */
@RestController
public class BoardController {

    private final BoardService boardService;
    private final AuthenticatedActorResolver actorResolver;

    public BoardController(BoardService boardService, AuthenticatedActorResolver actorResolver) {
        this.boardService = boardService;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/api/v1/projects/{id}/board")
    public ResponseEntity<BoardResponse> viewBoard(
            @PathVariable("id") String id,
            @RequestParam("itemType") String itemType,
            Authentication authentication) {
        AppUser actor = actorResolver.resolve(authentication);
        BoardView view = boardService.viewBoard(ProjectId.of(id), actor.id(), itemType);
        return ResponseEntity.ok(BoardResponse.from(view));
    }
}

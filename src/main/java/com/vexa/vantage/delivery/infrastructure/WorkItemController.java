package com.vexa.vantage.delivery.infrastructure;

import com.vexa.vantage.delivery.application.WorkItemService;
import com.vexa.vantage.delivery.domain.WorkItem;
import com.vexa.vantage.delivery.infrastructure.dto.PatchWorkItemRequest;
import com.vexa.vantage.delivery.infrastructure.dto.WorkItemRequest;
import com.vexa.vantage.delivery.infrastructure.dto.WorkItemResponse;
import com.vexa.vantage.identity.domain.AppUser;
import com.vexa.vantage.identity.infrastructure.AuthenticatedActorResolver;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.WorkItemId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone los casos de uso de {@link WorkItem}: {@code GET/POST
 * /api/v1/projects/{id}/work-items} (listar/crear) y {@code PATCH
 * /api/v1/work-items/{id}} (transición de estado, reparenting, reasignación
 * de sprint, edición de campos y reasignación de responsable, todo en una
 * sola solicitud vía {@link PatchWorkItemRequest#toCommand()}).
 *
 * <p>Resuelve la IDENTIDAD del actor autenticado con
 * {@link AuthenticatedActorResolver} y le delega, junto con los datos
 * propios de la ruta, la resolución de autorización a
 * {@link WorkItemService}.
 */
@RestController
public class WorkItemController {

    private final WorkItemService workItemService;
    private final AuthenticatedActorResolver actorResolver;

    public WorkItemController(WorkItemService workItemService, AuthenticatedActorResolver actorResolver) {
        this.workItemService = workItemService;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/api/v1/projects/{id}/work-items")
    public ResponseEntity<List<WorkItemResponse>> listWorkItems(
            @PathVariable("id") String id, Authentication authentication) {
        AppUser actor = actorResolver.resolve(authentication);
        List<WorkItem> items = workItemService.listByProject(ProjectId.of(id), actor.id());
        return ResponseEntity.ok(items.stream().map(WorkItemResponse::from).toList());
    }

    @PostMapping("/api/v1/projects/{id}/work-items")
    public ResponseEntity<WorkItemResponse> createWorkItem(
            @PathVariable("id") String id,
            @Valid @RequestBody WorkItemRequest request,
            Authentication authentication) {
        AppUser actor = actorResolver.resolve(authentication);
        WorkItem created = workItemService.createWorkItem(
                ProjectId.of(id), actor.id(), request.type(), request.state(), request.title(),
                request.description(), request.tshirtSize(), request.businessValue(), request.urgency(),
                request.storyPoints());
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkItemResponse.from(created));
    }

    @PatchMapping("/api/v1/work-items/{id}")
    public ResponseEntity<WorkItemResponse> patchWorkItem(
            @PathVariable("id") String id,
            @Valid @RequestBody PatchWorkItemRequest request,
            Authentication authentication) {
        AppUser actor = actorResolver.resolve(authentication);
        WorkItem patched = workItemService.patch(WorkItemId.of(id), actor.id(), request.toCommand());
        return ResponseEntity.ok(WorkItemResponse.from(patched));
    }
}

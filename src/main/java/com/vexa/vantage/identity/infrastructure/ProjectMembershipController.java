package com.vexa.vantage.identity.infrastructure;

import com.vexa.vantage.identity.application.ProjectMembershipService;
import com.vexa.vantage.identity.domain.AppUser;
import com.vexa.vantage.identity.domain.ProjectMembership;
import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.identity.infrastructure.dto.AddProjectMembershipRequest;
import com.vexa.vantage.identity.infrastructure.dto.ProjectMembershipResponse;
import com.vexa.vantage.shared.domain.UserId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone la administración de membresías de proyecto:
 * {@code GET /api/v1/projects/{id}/memberships} (listar miembros) y
 * {@code POST /api/v1/projects/{id}/memberships} (agregar un miembro).
 *
 * <p>A diferencia de {@link UserAdminController} (que resuelve el
 * {@code tenant_role} del actor, disponible directamente en su registro de
 * usuario), aquí el rol relevante es el rol de PROYECTO del actor, que
 * depende del {@code id} de la ruta; este controlador solo resuelve la
 * IDENTIDAD del actor autenticado con {@link AuthenticatedActorResolver} y
 * se la delega, junto con el {@code projectId}, a
 * {@link ProjectMembershipService}, que es quien resuelve el rol de
 * proyecto del actor y fuerza la exigencia vía
 * {@code AuthorizationPolicy.requireAtLeast} (403 si no alcanza, vía
 * {@code InsufficientPermissionException}).
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectMembershipController {

    private final ProjectMembershipService membershipService;
    private final AuthenticatedActorResolver actorResolver;

    public ProjectMembershipController(
            ProjectMembershipService membershipService, AuthenticatedActorResolver actorResolver) {
        this.membershipService = membershipService;
        this.actorResolver = actorResolver;
    }

    @GetMapping("/{id}/memberships")
    public ResponseEntity<List<ProjectMembershipResponse>> listMemberships(
            @PathVariable("id") String id, Authentication authentication) {
        UserId actorId = resolveActorUserId(authentication);
        List<ProjectMembership> memberships = membershipService.listMemberships(id, actorId);

        return ResponseEntity.ok(memberships.stream().map(ProjectMembershipResponse::from).toList());
    }

    @PostMapping("/{id}/memberships")
    public ResponseEntity<ProjectMembershipResponse> addMembership(
            @PathVariable("id") String id,
            @Valid @RequestBody AddProjectMembershipRequest request,
            Authentication authentication) {
        UserId actorId = resolveActorUserId(authentication);
        ProjectRole role = request.role();
        ProjectMembership created = membershipService.addMembership(
                id, actorId, UserId.of(request.userId()), role, request.scrumLabel());

        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMembershipResponse.from(created));
    }

    /**
     * Resuelve el {@link UserId} del actor autenticado.
     */
    private UserId resolveActorUserId(Authentication authentication) {
        AppUser actor = actorResolver.resolve(authentication);
        return actor.id();
    }
}

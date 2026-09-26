package com.vexa.vantage.identity.infrastructure;

import com.vexa.vantage.identity.application.UserManagementService;
import com.vexa.vantage.identity.application.UserNotFoundException;
import com.vexa.vantage.identity.domain.AppUser;
import com.vexa.vantage.identity.domain.TenantRole;
import com.vexa.vantage.identity.infrastructure.dto.UpdateUserDisabledRequest;
import com.vexa.vantage.identity.infrastructure.dto.UpdateUserTenantRoleRequest;
import com.vexa.vantage.shared.domain.UserId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone la administración de usuarios del tenant:
 * {@code PATCH /api/v1/users/{id}/disabled} (deshabilitar/reactivar) y
 * {@code PATCH /api/v1/users/{id}/tenant-role} (cambiar {@code tenant_role}).
 *
 * <p>Ambas operaciones están reservadas a {@link TenantRole#TENANT_ADMIN}:
 * este controlador resuelve la identidad del actor autenticado con
 * {@link AuthenticatedActorResolver} y le delega, YA RESUELTO, su rol a
 * {@link UserManagementService}, que es quien fuerza la exigencia vía
 * {@code AuthorizationPolicy.requireTenantAdmin} y lanza
 * {@code InsufficientPermissionException} (403) si no se cumple.
 *
 * <p>Solo importa el {@code id} del usuario objetivo dentro del tenant
 * vigente (acotado por {@code TenantContext}, ya establecido por
 * {@link JwtAuthenticationFilter} a partir del token de acceso del actor):
 * un {@code id} de otro tenant resulta invisible para el filtro Hibernate de
 * tenant y se reporta como {@link UserNotFoundException} (404), igual que un
 * {@code id} inexistente.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserAdminController {

    private final UserManagementService userManagementService;
    private final AuthenticatedActorResolver actorResolver;

    public UserAdminController(UserManagementService userManagementService, AuthenticatedActorResolver actorResolver) {
        this.userManagementService = userManagementService;
        this.actorResolver = actorResolver;
    }

    @PatchMapping("/{id}/disabled")
    public ResponseEntity<Void> updateDisabled(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateUserDisabledRequest request,
            Authentication authentication) {
        TenantRole actorRole = resolveActorTenantRole(authentication);
        UserId targetUserId = UserId.of(id);

        if (Boolean.TRUE.equals(request.disabled())) {
            userManagementService.disableUser(actorRole, targetUserId);
        } else {
            userManagementService.reactivateUser(actorRole, targetUserId);
        }

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/tenant-role")
    public ResponseEntity<Void> updateTenantRole(
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateUserTenantRoleRequest request,
            Authentication authentication) {
        TenantRole actorRole = resolveActorTenantRole(authentication);
        userManagementService.changeTenantRole(actorRole, UserId.of(id), request.tenantRole());

        return ResponseEntity.noContent().build();
    }

    /**
     * Resuelve el {@link TenantRole} del actor autenticado.
     */
    private TenantRole resolveActorTenantRole(Authentication authentication) {
        AppUser actor = actorResolver.resolve(authentication);
        return actor.tenantRole();
    }
}

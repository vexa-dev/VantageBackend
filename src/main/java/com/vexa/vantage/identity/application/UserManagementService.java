package com.vexa.vantage.identity.application;

import com.vexa.vantage.identity.domain.AppUser;
import com.vexa.vantage.identity.domain.AuthorizationPolicy;
import com.vexa.vantage.identity.domain.TenantRole;
import com.vexa.vantage.shared.domain.UserId;

import java.time.Instant;

/**
 * Servicio de aplicación responsable de la administración de usuarios dentro
 * de un tenant: deshabilitar/reactivar un usuario y cambiar su
 * {@code tenant_role}.
 *
 * <p>Todas las operaciones están reservadas a {@link TenantRole#TENANT_ADMIN}
 * y se fuerzan mediante {@link AuthorizationPolicy#requireTenantAdmin}, que
 * lanza {@code InsufficientPermissionException} si el rol del actor no
 * cumple el requisito.
 */
public class UserManagementService {

    private final AppUserRepositoryPort userRepository;

    public UserManagementService(AppUserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Deshabilita al usuario indicado, impidiéndole autenticarse.
     *
     * @param actorRole rol de tenant del actor que ejecuta la operación
     * @param targetUserId identificador del usuario a deshabilitar
     */
    public void disableUser(TenantRole actorRole, UserId targetUserId) {
        AuthorizationPolicy.requireTenantAdmin(actorRole);

        AppUser user = findUser(targetUserId);
        user.disable(Instant.now());
        userRepository.save(user);
    }

    /**
     * Reactiva al usuario indicado, permitiéndole autenticarse nuevamente.
     *
     * @param actorRole rol de tenant del actor que ejecuta la operación
     * @param targetUserId identificador del usuario a reactivar
     */
    public void reactivateUser(TenantRole actorRole, UserId targetUserId) {
        AuthorizationPolicy.requireTenantAdmin(actorRole);

        AppUser user = findUser(targetUserId);
        user.enable();
        userRepository.save(user);
    }

    /**
     * Cambia el {@code tenant_role} del usuario indicado.
     *
     * @param actorRole rol de tenant del actor que ejecuta la operación
     * @param targetUserId identificador del usuario cuyo rol se modifica
     * @param newRole nuevo rol de tenant a asignar
     */
    public void changeTenantRole(TenantRole actorRole, UserId targetUserId, TenantRole newRole) {
        AuthorizationPolicy.requireTenantAdmin(actorRole);

        AppUser user = findUser(targetUserId);
        user.changeTenantRole(newRole);
        userRepository.save(user);
    }

    private AppUser findUser(UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId.value()));
    }
}

package com.vexa.vantage.identity.infrastructure;

import com.vexa.vantage.identity.application.AppUserRepositoryPort;
import com.vexa.vantage.identity.application.UserNotFoundException;
import com.vexa.vantage.identity.domain.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Resuelve el {@link AppUser} autenticado a partir de su email (el
 * {@code Authentication#getName()} que puebla {@link JwtAuthenticationFilter}
 * en cada solicitud).
 *
 * <p>Preámbulo compartido por los controladores de administración del
 * contexto de identidad ({@link UserAdminController},
 * {@link ProjectMembershipController}): cada uno resuelve la IDENTIDAD del
 * actor (este helper) y le delega, junto con los datos propios de la ruta,
 * la resolución del ROL correspondiente y la exigencia de
 * {@code AuthorizationPolicy} a su respectivo servicio de aplicación
 * ({@code UserManagementService} exige {@code TenantRole#TENANT_ADMIN};
 * {@code ProjectMembershipService} exige un {@code ProjectRole} mínimo
 * dentro del proyecto de la ruta), ya que ese rol depende de cada caso de
 * uso y no puede generalizarse aquí.
 */
@Component
public class AuthenticatedActorResolver {

    private final AppUserRepositoryPort userRepository;

    public AuthenticatedActorResolver(AppUserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resuelve el {@link AppUser} autenticado.
     *
     * @throws UserNotFoundException si el email autenticado no corresponde
     *                                a ningún usuario (caso extremo: el
     *                                usuario fue eliminado después de emitido
     *                                su token de acceso)
     */
    public AppUser resolve(Authentication authentication) {
        String actorEmail = authentication.getName();
        return userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found: " + actorEmail));
    }
}

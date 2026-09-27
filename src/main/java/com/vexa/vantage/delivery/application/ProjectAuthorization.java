package com.vexa.vantage.delivery.application;

import com.vexa.vantage.identity.domain.AuthorizationPolicy;
import com.vexa.vantage.identity.domain.InsufficientPermissionException;
import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.UserId;

/**
 * Ayudante compartido que resuelve el rol de proyecto de un actor vía
 * {@link ProjectRoleResolverPort} y exige el mínimo requerido mediante
 * {@link AuthorizationPolicy#requireAtLeast}, lanzando
 * {@link InsufficientPermissionException} cuando el actor no tiene membresía
 * en el proyecto — un actor sin membresía se trata como rol insuficiente
 * (403), no como "proyecto inexistente", igual que
 * {@code identity.application.ProjectMembershipService}.
 *
 * <p>Extraído para evitar que {@link WorkItemService}, {@link ProjectService},
 * {@link BoardService} y {@link WorkflowDefinitionService} repitan cada uno
 * la misma lógica por separado — mismo espíritu que {@link ProjectLookup}
 * (refactor 4.20).
 */
class ProjectAuthorization {

    private final ProjectRoleResolverPort roleResolver;

    ProjectAuthorization(ProjectRoleResolverPort roleResolver) {
        this.roleResolver = roleResolver;
    }

    /**
     * Exige que el actor indicado tenga, en el proyecto indicado, al menos
     * el rol {@code minimum}.
     *
     * @throws InsufficientPermissionException si el actor no es miembro del
     *                                          proyecto, o su rol es inferior
     *                                          a {@code minimum}
     */
    void requireAtLeast(ProjectId projectId, UserId actorId, ProjectRole minimum) {
        ProjectRole actorRole = roleResolver.findRole(projectId, actorId)
                .orElseThrow(() -> new InsufficientPermissionException(
                        "User '" + actorId.value() + "' is not a member of project '" + projectId.value() + "'"));
        AuthorizationPolicy.requireAtLeast(actorRole, minimum);
    }
}

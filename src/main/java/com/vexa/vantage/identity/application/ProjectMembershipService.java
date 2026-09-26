package com.vexa.vantage.identity.application;

import com.vexa.vantage.identity.domain.AuthorizationPolicy;
import com.vexa.vantage.identity.domain.InsufficientPermissionException;
import com.vexa.vantage.identity.domain.ProjectMembership;
import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.identity.domain.ScrumLabel;
import com.vexa.vantage.shared.domain.UserId;

import java.util.List;

/**
 * Servicio de aplicación responsable de la administración de membresías de
 * proyecto: listar los miembros de un proyecto y agregar nuevos miembros.
 *
 * <p>A diferencia de {@link UserManagementService} (que recibe el rol del
 * actor YA RESUELTO por el llamador porque se trata de un rol de tenant,
 * disponible directamente en el usuario autenticado), aquí el rol relevante
 * es el rol del actor DENTRO DE ESE PROYECTO en particular, que este
 * servicio resuelve consultando {@link ProjectMembershipRepositoryPort} con
 * el {@code projectId} recibido; recién con ese rol resuelto aplica
 * {@link AuthorizationPolicy#requireAtLeast}. Un actor sin membresía en el
 * proyecto se trata como {@link InsufficientPermissionException} (403), no
 * como "proyecto inexistente".
 *
 * <ul>
 *     <li>Listar miembros exige al menos {@link ProjectRole#VIEWER}.</li>
 *     <li>Agregar un miembro exige al menos {@link ProjectRole#ADMIN}.</li>
 * </ul>
 */
public class ProjectMembershipService {

    private final ProjectMembershipRepositoryPort membershipRepository;

    public ProjectMembershipService(ProjectMembershipRepositoryPort membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    /**
     * Lista las membresías del proyecto indicado.
     *
     * @param projectId identificador del proyecto
     * @param actorId    identificador del actor que ejecuta la operación
     */
    public List<ProjectMembership> listMemberships(String projectId, UserId actorId) {
        ProjectRole actorRole = resolveActorRole(projectId, actorId);
        AuthorizationPolicy.requireAtLeast(actorRole, ProjectRole.VIEWER);

        return membershipRepository.findByProjectId(projectId);
    }

    /**
     * Agrega al usuario indicado como miembro del proyecto, con el rol y la
     * etiqueta Scrum (opcional) indicados.
     *
     * @param projectId    identificador del proyecto
     * @param actorId      identificador del actor que ejecuta la operación
     * @param targetUserId identificador del usuario a agregar
     * @param role         rol de proyecto a asignar al nuevo miembro
     * @param scrumLabel   etiqueta Scrum opcional del nuevo miembro
     * @return la membresía creada
     */
    public ProjectMembership addMembership(
            String projectId, UserId actorId, UserId targetUserId, ProjectRole role, ScrumLabel scrumLabel) {
        ProjectRole actorRole = resolveActorRole(projectId, actorId);
        AuthorizationPolicy.requireAtLeast(actorRole, ProjectRole.ADMIN);

        ProjectMembership membership = new ProjectMembership(projectId, targetUserId, role, scrumLabel);
        membershipRepository.save(membership);
        return membership;
    }

    private ProjectRole resolveActorRole(String projectId, UserId actorId) {
        return membershipRepository.findByProjectIdAndUserId(projectId, actorId)
                .map(ProjectMembership::role)
                .orElseThrow(() -> new InsufficientPermissionException(
                        "User '%s' is not a member of project '%s'".formatted(actorId.value(), projectId)));
    }
}

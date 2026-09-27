package com.vexa.vantage.delivery.application;

import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto (arquitectura hexagonal) que la capa de aplicación de delivery usa
 * para resolver el rol de proyecto ({@link ProjectRole}) de un actor, sin
 * conocer el mecanismo de persistencia concreto de las membresías —
 * implementado en infraestructura reutilizando la persistencia de membresías
 * que ya posee el contexto de identidad, en lugar de duplicar esa tabla o su
 * SQL.
 *
 * <p>Este puerto consume {@code identity.domain.ProjectRole} directamente
 * (en vez de un enum propio de delivery) porque
 * {@code AuthorizationPolicy.requireAtLeast} — también de
 * {@code identity.domain} — opera sobre ese tipo, y este servicio de
 * aplicación necesita invocarlo tal cual, sin traducir de un lado a otro.
 * {@code ArchitectureRulesTest} solo aísla los paquetes {@code *.domain}
 * entre contextos delimitados; un puerto de la capa de aplicación de
 * delivery consumiendo un tipo del dominio de identidad no cae bajo esa
 * regla, y es exactamente la misma forma que ya usa
 * {@code identity.application.ProjectMembershipService} — decisión de
 * usuario registrada el 2026-09-27 (ver informe de aplicación de esta fase).
 */
public interface ProjectRoleResolverPort {

    /**
     * Resuelve el rol de proyecto del actor indicado, si tiene membresía en
     * ese proyecto.
     */
    Optional<ProjectRole> findRole(ProjectId projectId, UserId actorId);

    /**
     * Devuelve los identificadores de todos los proyectos en los que el
     * actor indicado tiene alguna membresía (de cualquier rol) — usado por
     * {@code ProjectService#listProjects} porque ni el spec ni el diseño
     * exigen una regla distinta ("list = todos los proyectos del tenant" no
     * está especificado; se resuelve como "proyectos donde el actor es
     * miembro", ver informe de esta fase de aplicación).
     */
    List<ProjectId> findProjectIdsForActor(UserId actorId);
}

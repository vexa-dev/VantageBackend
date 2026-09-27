package com.vexa.vantage.delivery.application;

import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.UserId;

/**
 * Puerto (arquitectura hexagonal) que la capa de aplicación de delivery usa
 * para asignar la membresía inicial de un proyecto recién creado, sin
 * conocer el mecanismo de persistencia de membresías — implementado en
 * infraestructura delegando a {@code identity.application.ProjectMembershipRepositoryPort},
 * en lugar de duplicar la tabla {@code project_membership} o su SQL en el
 * contexto de delivery (mismo espíritu que {@link ProjectRoleResolverPort}).
 *
 * <p>Distinto de {@link ProjectRoleResolverPort} (consulta de solo lectura):
 * este puerto ESCRIBE la membresía inicial, sin pasar por
 * {@code identity.application.ProjectMembershipService.addMembership} porque
 * ese caso de uso exige que el actor YA tenga al menos rol
 * {@code ProjectRole#ADMIN} en el proyecto — invariante que el creador de un
 * proyecto nuevo no puede satisfacer todavía (requisito "generalized-rbac",
 * "Default Role Assignment": el creador se vuelve Owner automáticamente, sin
 * un paso de asignación de rol separado).
 */
public interface ProjectMembershipProvisioningPort {

    /**
     * Asigna al usuario indicado como {@code OWNER} del proyecto indicado.
     */
    void provisionOwner(ProjectId projectId, UserId ownerId);
}

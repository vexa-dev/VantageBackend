package com.vexa.vantage.delivery.infrastructure;

import com.vexa.vantage.delivery.application.ProjectMembershipProvisioningPort;
import com.vexa.vantage.identity.application.ProjectMembershipRepositoryPort;
import com.vexa.vantage.identity.domain.ProjectMembership;
import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.UserId;
import org.springframework.stereotype.Component;

/**
 * Adapter (arquitectura hexagonal) que implementa
 * {@link ProjectMembershipProvisioningPort} reutilizando la persistencia de
 * membresías que ya posee el contexto de identidad
 * ({@link ProjectMembershipRepositoryPort}), en lugar de duplicar la tabla
 * {@code project_membership} o su SQL en el contexto de delivery — mismo
 * espíritu que {@link ProjectRoleResolverAdapter}.
 *
 * <p>Llama a {@code ProjectMembershipRepositoryPort#save} directamente, no a
 * {@code identity.application.ProjectMembershipService.addMembership}: ese
 * caso de uso exige que el actor YA sea al menos {@code ADMIN} del proyecto,
 * invariante que no aplica al aprovisionar la membresía inicial del creador
 * (ver Javadoc del puerto).
 */
@Component
public class ProjectMembershipProvisioningAdapter implements ProjectMembershipProvisioningPort {

    private final ProjectMembershipRepositoryPort membershipRepository;

    public ProjectMembershipProvisioningAdapter(ProjectMembershipRepositoryPort membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    public void provisionOwner(ProjectId projectId, UserId ownerId) {
        membershipRepository.save(new ProjectMembership(projectId.value(), ownerId, ProjectRole.OWNER, null));
    }
}

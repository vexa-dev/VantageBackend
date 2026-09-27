package com.vexa.vantage.delivery.infrastructure;

import com.vexa.vantage.delivery.application.ProjectRoleResolverPort;
import com.vexa.vantage.identity.application.ProjectMembershipRepositoryPort;
import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.shared.domain.ProjectId;
import com.vexa.vantage.shared.domain.UserId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapter (arquitectura hexagonal) que implementa
 * {@link ProjectRoleResolverPort} reutilizando la persistencia de
 * membresías que ya posee el contexto de identidad
 * ({@link ProjectMembershipRepositoryPort}), en lugar de duplicar la tabla
 * {@code project_membership} o su SQL en el contexto de delivery.
 *
 * <p>No vive en {@code infrastructure.persistence} (a diferencia de los
 * adapters de persistencia propios de delivery introducidos en esta misma
 * fase) porque no persiste nada por sí mismo — solo traduce una consulta de
 * solo lectura hacia un puerto de aplicación de OTRO contexto delimitado.
 * Por eso {@code TenantFilterAspect} (cuyo pointcut apunta a
 * {@code *.infrastructure.persistence..*RepositoryAdapter}) no lo intercepta:
 * la membresía de proyecto no tiene columna de tenant propia (ver
 * {@code ProjectMembershipRepositoryAdapter}), así que no habría nada que
 * acotar de todos modos.
 */
@Component
public class ProjectRoleResolverAdapter implements ProjectRoleResolverPort {

    private final ProjectMembershipRepositoryPort membershipRepository;

    public ProjectRoleResolverAdapter(ProjectMembershipRepositoryPort membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    public Optional<ProjectRole> findRole(ProjectId projectId, UserId actorId) {
        return membershipRepository.findByProjectIdAndUserId(projectId.value(), actorId)
                .map(com.vexa.vantage.identity.domain.ProjectMembership::role);
    }

    @Override
    public List<ProjectId> findProjectIdsForActor(UserId actorId) {
        return membershipRepository.findByUserId(actorId).stream()
                .map(membership -> ProjectId.of(membership.projectId()))
                .toList();
    }
}

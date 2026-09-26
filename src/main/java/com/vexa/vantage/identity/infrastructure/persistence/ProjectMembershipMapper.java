package com.vexa.vantage.identity.infrastructure.persistence;

import com.vexa.vantage.identity.domain.ProjectMembership;
import com.vexa.vantage.shared.domain.UserId;

/**
 * Mapper manual entre {@link ProjectMembershipJpaEntity} (infraestructura) y
 * {@link ProjectMembership} (dominio puro).
 */
public final class ProjectMembershipMapper {

    private ProjectMembershipMapper() {
    }

    public static ProjectMembership toDomain(ProjectMembershipJpaEntity entity) {
        return new ProjectMembership(
                String.valueOf(entity.getProjectId()),
                UserId.of(String.valueOf(entity.getUserId())),
                entity.getRole(),
                entity.getScrumLabel());
    }

    public static ProjectMembershipJpaEntity toEntity(ProjectMembership domain) {
        ProjectMembershipJpaEntity entity = new ProjectMembershipJpaEntity();
        entity.setProjectId(Long.parseLong(domain.projectId()));
        entity.setUserId(Long.parseLong(domain.userId().value()));
        entity.setRole(domain.role());
        entity.setScrumLabel(domain.scrumLabel().orElse(null));
        return entity;
    }
}

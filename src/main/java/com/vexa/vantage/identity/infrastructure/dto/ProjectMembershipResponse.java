package com.vexa.vantage.identity.infrastructure.dto;

import com.vexa.vantage.identity.domain.ProjectMembership;
import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.identity.domain.ScrumLabel;

/**
 * Representación de una membresía de proyecto expuesta por
 * {@code ProjectMembershipController}. Se mapea manualmente desde
 * {@link ProjectMembership} (dominio puro, sin dependencia de Jackson) en
 * lugar de serializar el objeto de dominio directamente.
 */
public record ProjectMembershipResponse(String userId, ProjectRole role, ScrumLabel scrumLabel) {

    public static ProjectMembershipResponse from(ProjectMembership membership) {
        return new ProjectMembershipResponse(
                membership.userId().value(), membership.role(), membership.scrumLabel().orElse(null));
    }
}

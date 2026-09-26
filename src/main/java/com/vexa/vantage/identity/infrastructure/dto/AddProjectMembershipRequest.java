package com.vexa.vantage.identity.infrastructure.dto;

import com.vexa.vantage.identity.domain.ProjectRole;
import com.vexa.vantage.identity.domain.ScrumLabel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de la solicitud de {@code POST /api/v1/projects/{id}/memberships}:
 * el usuario a agregar como miembro del proyecto, con su rol y una etiqueta
 * Scrum opcional.
 */
public record AddProjectMembershipRequest(

        @NotBlank
        String userId,

        @NotNull
        ProjectRole role,

        ScrumLabel scrumLabel) {
}

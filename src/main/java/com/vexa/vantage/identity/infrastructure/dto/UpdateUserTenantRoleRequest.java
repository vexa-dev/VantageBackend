package com.vexa.vantage.identity.infrastructure.dto;

import com.vexa.vantage.identity.domain.TenantRole;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de la solicitud de {@code PATCH /api/v1/users/{id}/tenant-role}: el
 * nuevo {@code tenant_role} a asignar al usuario indicado.
 */
public record UpdateUserTenantRoleRequest(

        @NotNull
        TenantRole tenantRole) {
}

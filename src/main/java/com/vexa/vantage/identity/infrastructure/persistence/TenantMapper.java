package com.vexa.vantage.identity.infrastructure.persistence;

import com.vexa.vantage.identity.domain.Tenant;
import com.vexa.vantage.shared.domain.TenantId;

/**
 * Mapper manual entre {@link TenantJpaEntity} (infraestructura) y
 * {@link Tenant} (dominio puro).
 *
 * <p>{@code subscriptionType} y {@code createdAt} son propios de la entidad
 * y no tienen contraparte en el objeto de dominio actual; se preservan al
 * convertir hacia la entidad solo cuando ya existen en ella (ver
 * {@link #toEntity(Tenant, TenantJpaEntity)}).
 */
public final class TenantMapper {

    private TenantMapper() {
    }

    public static Tenant toDomain(TenantJpaEntity entity) {
        return new Tenant(TenantId.of(String.valueOf(entity.getId())), entity.getName());
    }

    /**
     * Vuelca los campos modelados por el dominio sobre una entidad existente
     * (preservando su {@code subscriptionType} actual), o crea una entidad
     * nueva con el valor por defecto {@code FREE} cuando no hay una previa.
     */
    public static TenantJpaEntity toEntity(Tenant domain, TenantJpaEntity existing) {
        TenantJpaEntity entity = existing != null ? existing : new TenantJpaEntity();
        if (existing == null) {
            entity.setId(Long.parseLong(domain.id().value()));
            entity.setSubscriptionType("FREE");
        }
        entity.setName(domain.name());
        return entity;
    }
}

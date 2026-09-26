package com.vexa.vantage.identity.infrastructure.persistence;

import com.vexa.vantage.identity.domain.AppUser;
import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.domain.UserId;

/**
 * Mapper manual entre {@link AppUserJpaEntity} (infraestructura) y
 * {@link AppUser} (dominio puro).
 *
 * <p>Esta fase de infraestructura solo actualiza usuarios ya existentes
 * (login, refresh/logout, habilitar/deshabilitar, cambio de rol de tenant);
 * no hay flujo de alta de usuarios nuevos todavía, así que
 * {@link AppUser#id()} siempre corresponde a una fila ya persistida y
 * {@link #toEntity(AppUser)} siempre puede resolver un {@code id} numérico
 * real.
 */
public final class AppUserMapper {

    private AppUserMapper() {
    }

    public static AppUser toDomain(AppUserJpaEntity entity) {
        return new AppUser(
                UserId.of(String.valueOf(entity.getId())),
                TenantId.of(String.valueOf(entity.getTenantId())),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getFullName(),
                entity.getTenantRole(),
                entity.getDisabledAt());
    }

    public static AppUserJpaEntity toEntity(AppUser domain) {
        AppUserJpaEntity entity = new AppUserJpaEntity();
        entity.setId(Long.parseLong(domain.id().value()));
        entity.setTenantId(Long.parseLong(domain.tenantId().value()));
        entity.setEmail(domain.email());
        entity.setPasswordHash(domain.passwordHash());
        entity.setFullName(domain.fullName());
        entity.setTenantRole(domain.tenantRole());
        entity.setDisabledAt(domain.disabledAt());
        return entity;
    }
}

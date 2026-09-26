package com.vexa.vantage.identity.infrastructure.persistence;

import com.vexa.vantage.identity.domain.RefreshToken;
import com.vexa.vantage.shared.domain.UserId;

/**
 * Mapper manual entre {@link RefreshTokenJpaEntity} (infraestructura) y
 * {@link RefreshToken} (dominio puro).
 *
 * <p>La identidad de dominio de {@link RefreshToken} es {@code jti}, no el
 * {@code id} numérico de la fila; por eso {@link #toEntity} recibe la
 * entidad existente (si la hay, localizada por {@code jti} en el adapter)
 * para preservar su {@code id} de fila al actualizar.
 */
public final class RefreshTokenMapper {

    private RefreshTokenMapper() {
    }

    public static RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return new RefreshToken(
                entity.getJti(),
                UserId.of(String.valueOf(entity.getUserId())),
                entity.getIssuedAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt());
    }

    public static RefreshTokenJpaEntity toEntity(RefreshToken domain, RefreshTokenJpaEntity existing) {
        RefreshTokenJpaEntity entity = existing != null ? existing : new RefreshTokenJpaEntity();
        entity.setJti(domain.jti());
        entity.setUserId(Long.parseLong(domain.userId().value()));
        entity.setIssuedAt(domain.issuedAt());
        entity.setExpiresAt(domain.expiresAt());
        entity.setRevokedAt(domain.revokedAt());
        return entity;
    }
}

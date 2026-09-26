package com.vexa.vantage.identity.application;

import com.vexa.vantage.identity.domain.RefreshToken;
import com.vexa.vantage.shared.domain.UserId;

import java.util.Optional;

/**
 * Puerto (en el sentido de arquitectura hexagonal) que la capa de
 * aplicación usa para persistir y consultar {@link RefreshToken}, sin
 * conocer el mecanismo de persistencia concreto (JPA, memoria, etc.), que
 * se implementa en la capa de infraestructura.
 */
public interface RefreshTokenRepositoryPort {

    /**
     * Guarda (inserta o actualiza) un refresh token.
     */
    void save(RefreshToken refreshToken);

    /**
     * Busca un refresh token por su {@code jti} (JWT ID).
     */
    Optional<RefreshToken> findByJti(String jti);

    /**
     * Revoca todos los refresh tokens vigentes de un usuario, usado cuando
     * se detecta el reuso de un {@code jti} ya revocado.
     */
    void revokeAllForUser(UserId userId);
}

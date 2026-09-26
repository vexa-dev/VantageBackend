package com.vexa.vantage.identity.domain;

import com.vexa.vantage.shared.domain.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * Entidad de dominio que representa un refresh token, mapeada a la tabla
 * {@code refresh_token}. La identidad de esta entidad está dada por
 * {@code jti} (JWT ID), no por el {@code id} numérico de la fila.
 *
 * <p>{@code revokedAt} transiciona de {@code null} a un timestamp mediante
 * {@link #revoke(Instant)}. {@link #isRevoked()} es la señal de dominio que
 * la capa de aplicación consulta para detectar el reuso de un token que ya
 * fue revocado (por ejemplo, tras una rotación previa): si un {@code jti}
 * que llega en una solicitud de refresh ya está revocado, se trata de un
 * intento de reuso y debe disparar la revocación de todos los tokens del
 * usuario.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}).
 */
public final class RefreshToken {

    private final String jti;
    private final UserId userId;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private Instant revokedAt;

    public RefreshToken(String jti, UserId userId, Instant issuedAt, Instant expiresAt) {
        this(jti, userId, issuedAt, expiresAt, null);
    }

    public RefreshToken(String jti, UserId userId, Instant issuedAt, Instant expiresAt, Instant revokedAt) {
        this.jti = jti;
        this.userId = userId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public String jti() {
        return jti;
    }

    public UserId userId() {
        return userId;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke(Instant when) {
        this.revokedAt = when;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RefreshToken that)) {
            return false;
        }
        return jti.equals(that.jti);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jti);
    }
}

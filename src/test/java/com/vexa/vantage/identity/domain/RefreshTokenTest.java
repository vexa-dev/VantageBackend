package com.vexa.vantage.identity.domain;

import com.vexa.vantage.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica {@link RefreshToken}: identidad por {@code jti}, la transición de
 * {@code revokedAt} (de {@code null} a un timestamp), y la señal de dominio
 * de reuso de un token ya revocado, expuesta como {@link RefreshToken#isRevoked()}.
 */
class RefreshTokenTest {

    private static final UserId USER_ID = UserId.of("user-1");
    private static final Instant ISSUED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-01-08T00:00:00Z");

    @Test
    void identityIsBasedOnJti() {
        RefreshToken tokenA = new RefreshToken("jti-1", USER_ID, ISSUED_AT, EXPIRES_AT);
        RefreshToken tokenB = new RefreshToken("jti-1", USER_ID, ISSUED_AT, EXPIRES_AT);
        RefreshToken tokenC = new RefreshToken("jti-2", USER_ID, ISSUED_AT, EXPIRES_AT);

        assertThat(tokenA).isEqualTo(tokenB);
        assertThat(tokenA).isNotEqualTo(tokenC);
        assertThat(tokenA.jti()).isEqualTo("jti-1");
    }

    @Test
    void isNotRevokedByDefault() {
        RefreshToken token = new RefreshToken("jti-1", USER_ID, ISSUED_AT, EXPIRES_AT);

        assertThat(token.isRevoked()).isFalse();
        assertThat(token.revokedAt()).isNull();
    }

    @Test
    void revokeTransitionsRevokedAtFromNullToTimestamp() {
        RefreshToken token = new RefreshToken("jti-1", USER_ID, ISSUED_AT, EXPIRES_AT);
        Instant revocationInstant = Instant.parse("2026-01-02T00:00:00Z");

        token.revoke(revocationInstant);

        assertThat(token.revokedAt()).isEqualTo(revocationInstant);
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void isRevokedSignalsReuseOfAnAlreadyRevokedToken() {
        RefreshToken token = new RefreshToken("jti-1", USER_ID, ISSUED_AT, EXPIRES_AT);
        token.revoke(Instant.parse("2026-01-02T00:00:00Z"));

        // Una segunda presentación del mismo jti, ya revocado, es la señal
        // de dominio de un intento de reuso — la capa de aplicación consulta
        // isRevoked() para decidir si debe revocar todos los tokens del usuario.
        assertThat(token.isRevoked()).isTrue();
    }
}

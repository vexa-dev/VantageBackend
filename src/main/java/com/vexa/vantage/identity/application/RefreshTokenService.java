package com.vexa.vantage.identity.application;

import com.vexa.vantage.identity.domain.RefreshToken;
import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.domain.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Servicio de aplicación responsable de emitir, rotar y detectar el reuso
 * de refresh tokens para el contexto delimitado de identidad.
 *
 * <p>Un refresh token es un JWT con claim {@code typ=refresh} y un tiempo
 * de vida de {@value #REFRESH_TOKEN_TTL_DAYS} días. Su identidad de
 * dominio ({@code jti}) se persiste mediante {@link RefreshTokenRepositoryPort}
 * para poder detectar la reutilización de un token ya rotado (revocado):
 * si se presenta un {@code jti} que ya está revocado, se asume que el
 * token fue robado y se revocan todos los refresh tokens vigentes del
 * usuario.
 */
public class RefreshTokenService {

    private static final long REFRESH_TOKEN_TTL_DAYS = 7;

    private final SecretKey key;
    private final RefreshTokenRepositoryPort repository;

    public RefreshTokenService(String base64Secret, RefreshTokenRepositoryPort repository) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.repository = repository;
    }

    /**
     * Emite un nuevo refresh token para el usuario/tenant dados y lo
     * persiste mediante el puerto de repositorio.
     */
    public String issueRefreshToken(UserId userId, TenantId tenantId, String email) {
        String jti = UUID.randomUUID().toString();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofDays(REFRESH_TOKEN_TTL_DAYS));

        repository.save(new RefreshToken(jti, userId, issuedAt, expiresAt));

        return buildToken(jti, userId, tenantId, email, issuedAt, expiresAt);
    }

    /**
     * Rota un refresh token: valida su firma/expiración, revoca el
     * {@code jti} presentado y emite uno nuevo.
     *
     * <p>Si el {@code jti} presentado ya estaba revocado, se interpreta
     * como un intento de reuso y se revocan todos los refresh tokens del
     * usuario, rechazando la operación.
     *
     * @throws RefreshTokenNotFoundException      si el {@code jti} no es conocido
     * @throws RefreshTokenReuseDetectedException si se detecta reuso de un {@code jti} ya revocado
     */
    public String rotate(String refreshToken) {
        Claims claims = parseAndValidate(refreshToken);
        String jti = claims.getId();
        UserId userId = UserId.of(claims.get("uid", String.class));
        TenantId tenantId = TenantId.of(claims.get("tid", String.class));
        String email = claims.getSubject();

        RefreshToken existing = repository.findByJti(jti)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Unknown refresh token jti: " + jti));

        if (existing.isRevoked()) {
            revokeAllForUserOnReuseDetected(userId);
        }

        existing.revoke(Instant.now());
        repository.save(existing);

        return issueRefreshToken(userId, tenantId, email);
    }

    /**
     * Revoca el refresh token indicado (identificado por su {@code jti}),
     * usado por el logout para invalidarlo del lado del servidor.
     *
     * <p>Operación idempotente: si el {@code jti} no existe o ya estaba
     * revocado, no hace nada. El objetivo del logout es que el token deje de
     * ser válido, sea cual sea su estado previo.
     *
     * @throws io.jsonwebtoken.JwtException si el token está malformado o su firma es inválida
     */
    public void logout(String refreshToken) {
        Claims claims = parseAndValidate(refreshToken);
        String jti = claims.getId();

        repository.findByJti(jti).ifPresent(existing -> {
            if (!existing.isRevoked()) {
                existing.revoke(Instant.now());
                repository.save(existing);
            }
        });
    }

    /**
     * Reacciona a la detección de reuso de un {@code jti} ya revocado:
     * revoca preventivamente todos los refresh tokens vigentes del usuario
     * (se asume que el token fue robado) y rechaza la operación en curso.
     *
     * @throws RefreshTokenReuseDetectedException siempre, para abortar la rotación en curso
     */
    private void revokeAllForUserOnReuseDetected(UserId userId) {
        repository.revokeAllForUser(userId);
        throw new RefreshTokenReuseDetectedException(
                "Refresh token reuse detected for user '" + userId.value() + "'");
    }

    private String buildToken(
            String jti, UserId userId, TenantId tenantId, String email, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .id(jti)
                .subject(email)
                .claim("typ", "refresh")
                .claim("uid", userId.value())
                .claim("tid", tenantId.value())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

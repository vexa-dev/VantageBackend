package com.vexa.vantage.identity.application;

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

/**
 * Servicio de aplicación responsable de emitir y validar tokens de acceso
 * JWT (JSON Web Token) para el contexto delimitado de identidad.
 *
 * <p>El token de acceso incluye los claims {@code sub} (email del usuario),
 * {@code uid} (identificador de usuario) y {@code tid} (identificador de
 * tenant), con un tiempo de vida de {@value #ACCESS_TOKEN_TTL_MINUTES}
 * minutos. Se firma con HMAC-SHA256 usando jjwt 0.12.6.
 */
public class JwtService {

    private static final long ACCESS_TOKEN_TTL_MINUTES = 15;

    private final SecretKey key;

    public JwtService(String base64Secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
    }

    /**
     * Emite un token de acceso firmado para el usuario/tenant dados, con
     * expiración a los {@value #ACCESS_TOKEN_TTL_MINUTES} minutos desde el
     * momento de la emisión.
     */
    public String issueAccessToken(UserId userId, TenantId tenantId, String email) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plus(Duration.ofMinutes(ACCESS_TOKEN_TTL_MINUTES));
        return Jwts.builder()
                .subject(email)
                .claim("uid", userId.value())
                .claim("tid", tenantId.value())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Analiza un token y valida su firma y expiración, devolviendo sus
     * claims.
     *
     * @throws io.jsonwebtoken.ExpiredJwtException si el token ya expiró
     * @throws io.jsonwebtoken.JwtException        si la firma es inválida o el token está malformado
     */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

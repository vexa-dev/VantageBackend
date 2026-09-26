package com.vexa.vantage.identity.application;

import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.domain.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba unitaria pura (sin contexto Spring) de {@link JwtService}: emisión
 * de un token de acceso con los claims {@code sub}/{@code uid}/{@code tid},
 * y validación de expiración usando jjwt 0.12.6.
 */
class JwtServiceTest {

    private static final String SECRET = randomBase64Secret();

    private static String randomBase64Secret() {
        byte[] rawKey = new byte[32];
        new SecureRandom().nextBytes(rawKey);
        return Base64.getEncoder().encodeToString(rawKey);
    }

    @Test
    void issuesAccessTokenWithExpectedClaims() {
        JwtService jwtService = new JwtService(SECRET);

        String token = jwtService.issueAccessToken(UserId.of("user-1"), TenantId.of("tenant-1"), "ana@acme.com");
        Claims claims = jwtService.parseAndValidate(token);

        assertThat(claims.getSubject()).isEqualTo("ana@acme.com");
        assertThat(claims.get("uid", String.class)).isEqualTo("user-1");
        assertThat(claims.get("tid", String.class)).isEqualTo("tenant-1");
    }

    @Test
    void issuesAccessTokenWithDifferentClaimsForAnotherUser() {
        JwtService jwtService = new JwtService(SECRET);

        String token = jwtService.issueAccessToken(UserId.of("user-2"), TenantId.of("tenant-2"), "beto@acme.com");
        Claims claims = jwtService.parseAndValidate(token);

        assertThat(claims.getSubject()).isEqualTo("beto@acme.com");
        assertThat(claims.get("uid", String.class)).isEqualTo("user-2");
        assertThat(claims.get("tid", String.class)).isEqualTo("tenant-2");
    }

    @Test
    void accessTokenExpiresFifteenMinutesAfterIssuance() {
        JwtService jwtService = new JwtService(SECRET);

        String token = jwtService.issueAccessToken(UserId.of("user-1"), TenantId.of("tenant-1"), "ana@acme.com");
        Claims claims = jwtService.parseAndValidate(token);

        long ttlMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(ttlMillis).isEqualTo(java.time.Duration.ofMinutes(15).toMillis());
    }

    @Test
    void parseAndValidateRejectsExpiredToken() {
        JwtService jwtService = new JwtService(SECRET);
        SecretKey key = Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(SECRET));
        Instant past = Instant.now().minus(java.time.Duration.ofDays(1));
        String expiredToken = Jwts.builder()
                .subject("ana@acme.com")
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.parseAndValidate(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }
}

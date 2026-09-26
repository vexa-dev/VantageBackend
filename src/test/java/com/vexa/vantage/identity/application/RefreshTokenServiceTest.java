package com.vexa.vantage.identity.application;

import com.vexa.vantage.identity.domain.RefreshToken;
import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.domain.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba unitaria de {@link RefreshTokenService} usando Mockito sobre
 * {@link RefreshTokenRepositoryPort}: emisión y guardado de un refresh
 * token nuevo, rotación (revoca el {@code jti} viejo y emite uno nuevo), y
 * detección de reuso de un {@code jti} ya revocado (revoca todos los
 * refresh tokens del usuario).
 */
class RefreshTokenServiceTest {

    private static final String SECRET = randomBase64Secret();
    private static final UserId USER_ID = UserId.of("user-1");
    private static final TenantId TENANT_ID = TenantId.of("tenant-1");
    private static final String EMAIL = "ana@acme.com";

    private RefreshTokenRepositoryPort repository;
    private RefreshTokenService service;

    private static String randomBase64Secret() {
        byte[] rawKey = new byte[32];
        new SecureRandom().nextBytes(rawKey);
        return Base64.getEncoder().encodeToString(rawKey);
    }

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenRepositoryPort.class);
        service = new RefreshTokenService(SECRET, repository);
    }

    private Claims decode(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    @Test
    void issuesAndSavesNewRefreshTokenWithSevenDayTtlAndRefreshClaim() {
        String token = service.issueRefreshToken(USER_ID, TENANT_ID, EMAIL);

        Claims claims = decode(token);
        assertThat(claims.get("typ", String.class)).isEqualTo("refresh");
        assertThat(claims.getSubject()).isEqualTo(EMAIL);
        long ttlMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(ttlMillis).isEqualTo(Duration.ofDays(7).toMillis());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().jti()).isEqualTo(claims.getId());
        assertThat(captor.getValue().isRevoked()).isFalse();
    }

    @Test
    void rotatingRefreshTokenRevokesOldJtiAndIssuesADifferentNewOne() {
        String originalToken = service.issueRefreshToken(USER_ID, TENANT_ID, EMAIL);
        Claims originalClaims = decode(originalToken);
        String originalJti = originalClaims.getId();

        RefreshToken storedOriginal = new RefreshToken(
                originalJti,
                USER_ID,
                originalClaims.getIssuedAt().toInstant(),
                originalClaims.getExpiration().toInstant());
        when(repository.findByJti(originalJti)).thenReturn(Optional.of(storedOriginal));

        String rotatedToken = service.rotate(originalToken);

        assertThat(storedOriginal.isRevoked()).isTrue();
        verify(repository, atLeastOnce()).save(storedOriginal);

        Claims rotatedClaims = decode(rotatedToken);
        assertThat(rotatedClaims.getId()).isNotEqualTo(originalJti);
        assertThat(rotatedClaims.get("typ", String.class)).isEqualTo("refresh");
        assertThat(rotatedClaims.getSubject()).isEqualTo(EMAIL);

        verify(repository, never()).revokeAllForUser(any());
    }

    /**
     * Construye un refresh token firmado sin pasar por
     * {@link RefreshTokenService#issueRefreshToken}, para no ensuciar el
     * historial de invocaciones del mock {@code repository} con el
     * {@code save} interno que ese método ya dispara (lo que rompería las
     * verificaciones de {@code logout} sobre un {@code RefreshToken} con el
     * mismo {@code jti}, dado que {@link RefreshToken#equals} solo compara
     * por {@code jti}).
     */
    private String buildRefreshTokenJwt(String jti, Instant issuedAt, Instant expiresAt) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .id(jti)
                .subject(EMAIL)
                .claim("typ", "refresh")
                .claim("uid", USER_ID.value())
                .claim("tid", TENANT_ID.value())
                .issuedAt(java.util.Date.from(issuedAt))
                .expiration(java.util.Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Test
    void logoutRevokesTheRefreshTokenIdentifiedByItsJti() {
        String jti = java.util.UUID.randomUUID().toString();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofDays(7));
        String token = buildRefreshTokenJwt(jti, issuedAt, expiresAt);

        RefreshToken stored = new RefreshToken(jti, USER_ID, issuedAt, expiresAt);
        when(repository.findByJti(jti)).thenReturn(Optional.of(stored));

        service.logout(token);

        assertThat(stored.isRevoked()).isTrue();
        verify(repository).save(stored);
    }

    @Test
    void logoutOnAnAlreadyRevokedTokenIsIdempotentAndDoesNotSaveAgain() {
        String jti = java.util.UUID.randomUUID().toString();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofDays(7));
        String token = buildRefreshTokenJwt(jti, issuedAt, expiresAt);

        RefreshToken alreadyRevoked = new RefreshToken(jti, USER_ID, issuedAt, expiresAt);
        alreadyRevoked.revoke(Instant.now());
        when(repository.findByJti(jti)).thenReturn(Optional.of(alreadyRevoked));

        service.logout(token);

        verify(repository, never()).save(any());
    }

    @Test
    void reusingAnAlreadyRevokedJtiRevokesAllRefreshTokensForThatUser() {
        String originalToken = service.issueRefreshToken(USER_ID, TENANT_ID, EMAIL);
        Claims originalClaims = decode(originalToken);
        String originalJti = originalClaims.getId();

        RefreshToken revokedOriginal = new RefreshToken(
                originalJti,
                USER_ID,
                originalClaims.getIssuedAt().toInstant(),
                originalClaims.getExpiration().toInstant());
        revokedOriginal.revoke(Instant.now());
        when(repository.findByJti(originalJti)).thenReturn(Optional.of(revokedOriginal));

        assertThatThrownBy(() -> service.rotate(originalToken))
                .isInstanceOf(RefreshTokenReuseDetectedException.class);

        verify(repository).revokeAllForUser(USER_ID);
    }
}

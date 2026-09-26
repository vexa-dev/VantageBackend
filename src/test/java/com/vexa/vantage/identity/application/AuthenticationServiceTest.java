package com.vexa.vantage.identity.application;

import com.vexa.vantage.identity.domain.AppUser;
import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Prueba unitaria de {@link AuthenticationService} usando Mockito: login
 * exitoso delegando la emisión de tokens en {@link JwtService} y
 * {@link RefreshTokenService}, y login fallido (usuario inexistente,
 * contraseña incorrecta, usuario deshabilitado) sin emitir tokens.
 */
class AuthenticationServiceTest {

    private static final UserId USER_ID = UserId.of("user-1");
    private static final TenantId TENANT_ID = TenantId.of("tenant-1");
    private static final String EMAIL = "ana@acme.com";
    private static final String RAW_PASSWORD = "correct-horse-battery-staple";
    private static final String PASSWORD_HASH = "hashed-password";

    private AppUserRepositoryPort userRepository;
    private PasswordVerifierPort passwordVerifier;
    private JwtService jwtService;
    private RefreshTokenService refreshTokenService;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(AppUserRepositoryPort.class);
        passwordVerifier = mock(PasswordVerifierPort.class);
        jwtService = mock(JwtService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        authenticationService =
                new AuthenticationService(userRepository, passwordVerifier, jwtService, refreshTokenService);
    }

    private AppUser activeUser() {
        return new AppUser(USER_ID, TENANT_ID, EMAIL, PASSWORD_HASH, "Ana Torres");
    }

    @Test
    void loginIssuesAccessAndRefreshTokensWhenCredentialsAreValid() {
        AppUser user = activeUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordVerifier.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(jwtService.issueAccessToken(USER_ID, TENANT_ID, EMAIL)).thenReturn("access-token-123");
        when(refreshTokenService.issueRefreshToken(USER_ID, TENANT_ID, EMAIL)).thenReturn("refresh-token-456");

        AuthenticationResult result = authenticationService.login(EMAIL, RAW_PASSWORD);

        assertThat(result.accessToken()).isEqualTo("access-token-123");
        assertThat(result.refreshToken()).isEqualTo("refresh-token-456");
        verify(jwtService).issueAccessToken(USER_ID, TENANT_ID, EMAIL);
        verify(refreshTokenService).issueRefreshToken(USER_ID, TENANT_ID, EMAIL);
    }

    @Test
    void loginRejectsUnknownEmailWithoutIssuingTokens() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(EMAIL, RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    void loginRejectsWrongPasswordWithoutIssuingTokens() {
        AppUser user = activeUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordVerifier.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.login(EMAIL, RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).issueAccessToken(any(), any(), any());
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void loginRejectsDisabledUserEvenWithCorrectPasswordWithoutIssuingTokens() {
        AppUser user = activeUser();
        user.disable(Instant.parse("2026-01-01T00:00:00Z"));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.login(EMAIL, RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(jwtService, refreshTokenService);
    }
}

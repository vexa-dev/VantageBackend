package com.vexa.vantage.identity.application;

import com.vexa.vantage.identity.domain.AppUser;

/**
 * Servicio de aplicación responsable del login: resuelve al usuario por
 * email, verifica su contraseña y estado, y delega la emisión del par de
 * tokens (acceso + refresh) en {@link JwtService} y
 * {@link RefreshTokenService}.
 *
 * <p>Un email inexistente, una contraseña incorrecta y un usuario
 * deshabilitado se tratan de forma indistinguible (misma excepción, mismo
 * mensaje) para no revelar por enumeración qué emails existen en el
 * sistema.
 */
public class AuthenticationService {

    private final AppUserRepositoryPort userRepository;
    private final PasswordVerifierPort passwordVerifier;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationService(
            AppUserRepositoryPort userRepository,
            PasswordVerifierPort passwordVerifier,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordVerifier = passwordVerifier;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Intenta autenticar al usuario con el email/contraseña dados.
     *
     * @throws InvalidCredentialsException si el email no existe, la
     *                                      contraseña no coincide, o el
     *                                      usuario está deshabilitado
     */
    public AuthenticationResult login(String email, String rawPassword) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(AuthenticationService::invalidCredentials);

        if (user.isDisabled() || !passwordVerifier.matches(rawPassword, user.passwordHash())) {
            throw invalidCredentials();
        }

        String accessToken = jwtService.issueAccessToken(user.id(), user.tenantId(), user.email());
        String refreshToken = refreshTokenService.issueRefreshToken(user.id(), user.tenantId(), user.email());

        return new AuthenticationResult(accessToken, refreshToken);
    }

    private static InvalidCredentialsException invalidCredentials() {
        return new InvalidCredentialsException("Invalid email or password");
    }
}

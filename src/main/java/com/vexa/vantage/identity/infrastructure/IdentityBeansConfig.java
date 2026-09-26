package com.vexa.vantage.identity.infrastructure;

import com.vexa.vantage.identity.application.AppUserRepositoryPort;
import com.vexa.vantage.identity.application.AuthenticationService;
import com.vexa.vantage.identity.application.JwtService;
import com.vexa.vantage.identity.application.PasswordVerifierPort;
import com.vexa.vantage.identity.application.ProjectMembershipRepositoryPort;
import com.vexa.vantage.identity.application.ProjectMembershipService;
import com.vexa.vantage.identity.application.RefreshTokenRepositoryPort;
import com.vexa.vantage.identity.application.RefreshTokenService;
import com.vexa.vantage.identity.application.UserManagementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensambla como beans de Spring los servicios de aplicación del contexto
 * delimitado de identidad ({@link JwtService}, {@link RefreshTokenService},
 * {@link AuthenticationService}, {@link UserManagementService},
 * {@link ProjectMembershipService}), que son clases planas de Java sin
 * anotaciones de Spring (por diseño, para mantener la capa de aplicación
 * libre de dependencias de framework).
 *
 * <p>Reutiliza la misma propiedad {@code application.security.jwt.secret-key}
 * que ya usa el {@code JwtUtils} legado (par clave/expiración de
 * {@code com.vexa.vantage.security}) para no introducir una segunda fuente
 * de verdad para el secreto de firma JWT mientras ambos mecanismos
 * coexisten.
 */
@Configuration
public class IdentityBeansConfig {

    @Value("${application.security.jwt.secret-key}")
    private String jwtSecret;

    @Bean
    public JwtService jwtService() {
        return new JwtService(jwtSecret);
    }

    @Bean
    public RefreshTokenService refreshTokenService(RefreshTokenRepositoryPort refreshTokenRepositoryPort) {
        return new RefreshTokenService(jwtSecret, refreshTokenRepositoryPort);
    }

    @Bean
    public AuthenticationService authenticationService(
            AppUserRepositoryPort appUserRepositoryPort,
            PasswordVerifierPort passwordVerifierPort,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        return new AuthenticationService(appUserRepositoryPort, passwordVerifierPort, jwtService, refreshTokenService);
    }

    @Bean
    public UserManagementService userManagementService(AppUserRepositoryPort appUserRepositoryPort) {
        return new UserManagementService(appUserRepositoryPort);
    }

    @Bean
    public ProjectMembershipService projectMembershipService(
            ProjectMembershipRepositoryPort projectMembershipRepositoryPort) {
        return new ProjectMembershipService(projectMembershipRepositoryPort);
    }
}

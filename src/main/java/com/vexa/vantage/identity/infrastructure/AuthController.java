package com.vexa.vantage.identity.infrastructure;

import com.vexa.vantage.identity.application.AuthenticationResult;
import com.vexa.vantage.identity.application.AuthenticationService;
import com.vexa.vantage.identity.application.JwtService;
import com.vexa.vantage.identity.application.RefreshTokenService;
import com.vexa.vantage.identity.infrastructure.dto.LoginRequest;
import com.vexa.vantage.identity.infrastructure.dto.LoginResponse;
import com.vexa.vantage.identity.infrastructure.dto.RefreshResponse;
import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.domain.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone el flujo de autenticación del contexto delimitado de identidad:
 * login, refresh (rotación) y logout.
 *
 * <p>El refresh token SIEMPRE viaja en la cookie {@code refresh_token}
 * ({@code HttpOnly;Secure;SameSite=Strict}, acotada a {@code /api/v1/auth}),
 * nunca en el cuerpo JSON: solo el token de acceso se devuelve en el body.
 *
 * <p>El bean se nombra explícitamente {@code identityAuthController} porque
 * ya existe un {@code com.vexa.vantage.controller.AuthController} legado
 * (ruta {@code /api/auth}, sin versionar) cuyo nombre de bean por defecto
 * ("authController") coincidiría con el de esta clase si no se
 * desambiguara; ambos coexisten temporalmente hasta que el legado se elimine
 * en una fase futura.
 */
@RestController("identityAuthController")
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/v1/auth";

    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    public AuthController(
            AuthenticationService authenticationService,
            RefreshTokenService refreshTokenService,
            JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticationResult result = authenticationService.login(request.email(), request.password());

        ResponseCookie cookie = refreshTokenCookie(result.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponse(result.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(REFRESH_TOKEN_COOKIE_NAME) String refreshTokenCookie) {
        String rotatedRefreshToken = refreshTokenService.rotate(refreshTokenCookie);
        String accessToken = issueAccessTokenFor(rotatedRefreshToken);

        ResponseCookie cookie = refreshTokenCookie(rotatedRefreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new RefreshResponse(accessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshTokenCookie) {
        if (StringUtils.hasText(refreshTokenCookie)) {
            try {
                refreshTokenService.logout(refreshTokenCookie);
            } catch (JwtException ex) {
                // Un token ya inválido/expirado no impide el logout: el objetivo
                // (dejar de tener una cookie de refresh válida) igual se cumple.
            }
        }

        ResponseCookie expiredCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }

    /**
     * Extrae {@code uid}/{@code tid}/{@code sub} del refresh token ya
     * rotado (firmado con la misma clave que {@link JwtService}) para emitir
     * el nuevo token de acceso correspondiente.
     */
    private String issueAccessTokenFor(String refreshToken) {
        Claims claims = jwtService.parseAndValidate(refreshToken);
        UserId userId = UserId.of(claims.get("uid", String.class));
        TenantId tenantId = TenantId.of(claims.get("tid", String.class));
        String email = claims.getSubject();
        return jwtService.issueAccessToken(userId, tenantId, email);
    }

    private ResponseCookie refreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .build();
    }
}

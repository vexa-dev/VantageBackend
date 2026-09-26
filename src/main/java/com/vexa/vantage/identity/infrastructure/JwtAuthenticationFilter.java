package com.vexa.vantage.identity.infrastructure;

import com.vexa.vantage.identity.application.JwtService;
import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.infrastructure.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Filtro de autenticación stateless: lee el token de acceso Bearer del
 * encabezado {@code Authorization}, lo valida con {@link JwtService} y, si es
 * válido, puebla tanto el {@code SecurityContext} de Spring Security (para
 * que el resto de la cadena de filtros reconozca la solicitud como
 * autenticada) como {@link TenantContext} (para que {@code TenantFilterAspect}
 * acote las consultas de persistencia al tenant del token).
 *
 * <p>Un token ausente, expirado o inválido simplemente deja la solicitud sin
 * autenticar (no lanza ninguna excepción): las reglas de autorización de
 * {@code SecurityConfig} son las responsables de rechazar con 401/403 el
 * acceso a rutas protegidas cuando corresponda.
 *
 * <p>Un refresh token (claim {@code typ=refresh}, emitido por
 * {@code RefreshTokenService}) presentado como bearer también se trata como
 * inválido: solo un token de ACCESO (sin ese claim, emitido por
 * {@link JwtService#issueAccessToken}) puede autenticar una solicitud. Sin
 * esta distinción, un refresh token robado del cuerpo de una respuesta (o de
 * la cookie {@code refresh_token}) podría usarse directamente como bearer
 * token para acceder a cualquier ruta protegida durante sus 7 días de vida,
 * en lugar de estar limitado a rotar en {@code /api/v1/auth/refresh}.
 *
 * <p>{@link TenantContext} se limpia siempre en un bloque {@code finally} al
 * terminar la solicitud, para que un tenant no se filtre entre solicitudes
 * distintas que reutilizan el mismo hilo del pool de Tomcat.
 *
 * <p>Deliberadamente NO es un {@code @Component}: se instancia como
 * {@code @Bean} dentro de {@link SecurityConfig}. Un {@code Filter} anotado
 * como {@code @Component} es detectado automáticamente por el slice
 * {@code @WebMvcTest} (que sí escanea beans {@code Filter} aunque no estén
 * explícitamente importados), lo que rompería cualquier test de ese tipo
 * que no aporte un bean {@code JwtService}.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REFRESH_TOKEN_TYPE_CLAIM = "typ";
    private static final String REFRESH_TOKEN_TYPE_VALUE = "refresh";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            extractBearerToken(request).ifPresent(this::authenticate);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void authenticate(String token) {
        try {
            Claims claims = jwtService.parseAndValidate(token);
            if (REFRESH_TOKEN_TYPE_VALUE.equals(claims.get(REFRESH_TOKEN_TYPE_CLAIM, String.class))) {
                SecurityContextHolder.clearContext();
                return;
            }

            String email = claims.getSubject();
            String tenantId = claims.get("tid", String.class);

            TenantContext.set(TenantId.of(tenantId));

            var authentication = new UsernamePasswordAuthenticationToken(email, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }
    }

    private Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }
}

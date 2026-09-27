package com.vexa.vantage.identity.infrastructure;

import com.vexa.vantage.identity.application.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Configuración de Spring Security para el contexto delimitado de identidad:
 * cadena de filtros stateless (sin sesión de servidor) con
 * {@link JwtAuthenticationFilter} como único mecanismo de autenticación.
 *
 * <p>CSRF queda deshabilitado para {@code /api/v1/auth/refresh},
 * {@code /api/v1/auth/logout}, {@code /api/v1/users/**},
 * {@code /api/v1/projects/**} y {@code /api/v1/work-items/**} (Phase 4: el
 * contexto de delivery). Las dos primeras dependen de la cookie
 * {@code refresh_token} (con envío automático del navegador) en lugar de un
 * bearer token explícito, por lo que SÍ están expuestas al patrón de ataque
 * que CSRF previene (una credencial ambiente que el navegador adjunta solo);
 * las dos últimas (fases 3.32/3.35) se autentican exclusivamente por bearer
 * token en el encabezado {@code Authorization} (que el navegador nunca
 * adjunta automáticamente), por lo que quedan fuera de ese patrón de ataque
 * igual que el resto de la API ya excluida. {@code /api/v1/auth/login} sigue
 * exigiendo CSRF (decisión previa de esta fase, sin cambios).
 *
 * <p>Coexiste temporalmente con el {@code WebSecurityConfig} legado de
 * {@code com.vexa.vantage.security} (que se eliminará en una fase futura).
 * Spring Security exige que, cuando hay más de un {@code SecurityFilterChain},
 * a lo sumo uno de ellos pueda ser un catch-all sin {@code securityMatcher}
 * (falla al arrancar en caso contrario: "filter chain... has already been
 * configured"); como el catch-all sin acotar ya lo tiene el
 * {@code WebSecurityConfig} legado (que no debe tocarse), esta cadena se
 * acota explícitamente con {@link HttpSecurity#securityMatcher} a las rutas
 * que le pertenecen ({@code /api/v1/auth/**} y los health checks). Cuando el
 * legado se elimine, este {@code securityMatcher} debería quitarse para que
 * esta cadena pase a ser el catch-all único de la aplicación.
 *
 * <p>{@code SECURITY_MATCHER_PATHS} amplía ese acotamiento con
 * {@code /api/v1/users/**} y {@code /api/v1/projects/**} (fases 3.32/3.35),
 * y con {@code /api/v1/work-items/**} (Phase 4 batch 3b): son rutas nuevas
 * que también deben pasar por {@link JwtAuthenticationFilter} (no por el
 * {@code AuthTokenFilter} legado, que no entiende los claims
 * {@code uid}/{@code tid} ni el usuario de {@code app_user}), aunque a
 * diferencia de {@code /api/v1/auth/**} SÍ exigen autenticación (no están en
 * {@code PERMIT_ALL_PATHS}). {@code /api/v1/projects/**} ya cubre
 * {@code GET /api/v1/projects/{id}/board} y
 * {@code GET/PATCH /api/v1/projects/{id}/workflow-definition|status}
 * (delivery); solo {@code PATCH /api/v1/work-items/{id}} vive fuera de ese
 * prefijo.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PERMIT_ALL_PATHS = {
        "/actuator/health/liveness",
        "/actuator/health/readiness",
        "/api/v1/auth/**"
    };

    private static final String[] SECURITY_MATCHER_PATHS = {
        "/actuator/health/liveness",
        "/actuator/health/readiness",
        "/api/v1/auth/**",
        "/api/v1/users/**",
        "/api/v1/projects/**",
        "/api/v1/work-items/**"
    };

    /**
     * Bean interno del filtro, deliberadamente NO expuesto como
     * {@code @Component} (ver Javadoc de {@link JwtAuthenticationFilter}).
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain identitySecurityFilterChain(
            HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http.securityMatcher(SECURITY_MATCHER_PATHS)
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/v1/auth/refresh", "/api/v1/auth/logout",
                        "/api/v1/users/**", "/api/v1/projects/**", "/api/v1/work-items/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(SecurityConfig::respondUnauthorized)
                        .accessDeniedHandler(SecurityConfig::respondUnauthorized))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PERMIT_ALL_PATHS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Responde siempre 401 Unauthorized, tanto para {@code AuthenticationEntryPoint}
     * (solicitud anónima que llega a una ruta que exige autenticación) como
     * para {@code AccessDeniedHandler} (Spring Security trata al usuario
     * anónimo como una autenticación "presente pero insuficiente" frente a la
     * regla {@code anyRequest().authenticated()}, lo que dispara
     * {@code AccessDeniedException} en lugar de una excepción de
     * autenticación).
     *
     * <p>Unifica ambos casos en 401 (en vez del 403 por defecto de Spring
     * Security para {@code AccessDeniedException}) porque, para este cliente
     * sin sesión, "no autenticado" y "autenticado de forma anónima e
     * insuficiente" son la misma situación observable: falta un bearer token
     * válido. El 403 queda reservado para
     * {@link com.vexa.vantage.identity.domain.InsufficientPermissionException}
     * (rol de un usuario YA autenticado que no alcanza), mapeado por
     * {@code ApiExceptionHandler} después de que la solicitud ya atravesó
     * esta cadena de filtros.
     */
    private static void respondUnauthorized(
            HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}

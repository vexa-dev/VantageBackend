package com.vexa.vantage.shared.infrastructure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de springdoc-openapi: documenta la superficie
 * {@code /api/v1} bajo un único grupo, y declara el esquema de seguridad
 * bearer (token de acceso JWT) usado por cada endpoint protegido.
 */
@Configuration
public class OpenApiConfig {

    static final String API_TITLE = "Vantage API";
    static final String API_VERSION = "v1";
    static final String BEARER_SECURITY_SCHEME = "bearerAuth";
    static final String API_V1_GROUP = "v1";
    static final String API_V1_PATHS = "/api/v1/**";

    @Bean
    public OpenAPI vantageOpenAPI() {
        return new OpenAPI()
                .info(new Info().title(API_TITLE).version(API_VERSION))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SECURITY_SCHEME, bearerAccessTokenScheme()));
    }

    @Bean
    public GroupedOpenApi apiV1Group() {
        return GroupedOpenApi.builder()
                .group(API_V1_GROUP)
                .pathsToMatch(API_V1_PATHS)
                .build();
    }

    private SecurityScheme bearerAccessTokenScheme() {
        return new SecurityScheme()
                .name(BEARER_SECURITY_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Access token issued by /api/auth/login");
    }
}

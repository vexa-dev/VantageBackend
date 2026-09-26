package com.vexa.vantage.shared.infrastructure;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato simple (sin contexto de Spring) para las definiciones de bean de
 * {@link OpenApiConfig}: título/versión, el grupo {@code /api/v1} y el
 * esquema de seguridad bearer.
 */
class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void openApiDocumentsTitleAndVersion() {
        OpenAPI openApi = config.vantageOpenAPI();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("Vantage API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("v1");
    }

    @Test
    void openApiDeclaresABearerJwtSecurityScheme() {
        OpenAPI openApi = config.vantageOpenAPI();

        SecurityScheme scheme = openApi.getComponents().getSecuritySchemes().get("bearerAuth");

        assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme()).isEqualTo("bearer");
        assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
        assertThat(openApi.getSecurity()).anySatisfy(requirement ->
                assertThat(requirement).containsKey("bearerAuth"));
    }

    @Test
    void groupsTheApiV1SurfaceUnderItsOwnGroup() {
        GroupedOpenApi group = config.apiV1Group();

        assertThat(group.getGroup()).isEqualTo("v1");
        assertThat(group.getPathsToMatch()).containsExactly("/api/v1/**");
    }
}

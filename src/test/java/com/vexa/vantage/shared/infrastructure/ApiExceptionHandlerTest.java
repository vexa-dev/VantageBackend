package com.vexa.vantage.shared.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de extremo a extremo (MockMvc) para {@link ApiExceptionHandler}:
 * cada tipo de excepción mapeada produce el estado HTTP y la forma de
 * cuerpo documentados.
 */
@WebMvcTest(controllers = ApiExceptionHandlerTestController.class)
@Import(ApiExceptionHandler.class)
class ApiExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void domainValidationExceptionMapsTo400() throws Exception {
        mockMvc.perform(post("/test/api-exception-handler/domain-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("raw value must not be null or blank"));
    }

    @Test
    void crossTenantAccessExceptionMapsTo404() throws Exception {
        mockMvc.perform(post("/test/api-exception-handler/cross-tenant-access"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Project 'p-1' does not belong to tenant 'tenant-acme'"));
    }

    @Test
    void insufficientPermissionExceptionMapsTo403() throws Exception {
        mockMvc.perform(post("/test/api-exception-handler/insufficient-permission"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Role 'MEMBER' is not a tenant administrator"));
    }

    @Test
    void invalidCredentialsExceptionMapsTo401() throws Exception {
        mockMvc.perform(post("/test/api-exception-handler/invalid-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void jwtExceptionMapsTo401() throws Exception {
        mockMvc.perform(post("/test/api-exception-handler/jwt-exception"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    void refreshTokenReuseDetectedExceptionMapsTo401() throws Exception {
        mockMvc.perform(post("/test/api-exception-handler/refresh-token-reuse"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Refresh token reuse detected for user 'user-1'"));
    }

    @Test
    void refreshTokenNotFoundExceptionMapsTo401() throws Exception {
        mockMvc.perform(post("/test/api-exception-handler/unknown-jti"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unknown refresh token jti: abc-123"));
    }

    @Test
    void userNotFoundExceptionMapsTo404() throws Exception {
        mockMvc.perform(post("/test/api-exception-handler/unknown-user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found: user-1"));
    }

    @Test
    void projectNotFoundExceptionMapsTo404() throws Exception {
        mockMvc.perform(post("/test/api-exception-handler/unknown-project"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Project 'p-1' not found"));
    }

    @Test
    void workItemNotFoundExceptionMapsTo404() throws Exception {
        mockMvc.perform(post("/test/api-exception-handler/unknown-work-item"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("WorkItem 'w-1' not found"));
    }

    @Test
    void workflowDefinitionNotFoundExceptionMapsTo404() throws Exception {
        mockMvc.perform(post("/test/api-exception-handler/unknown-workflow-definition"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("WorkflowDefinition 'wfd-1' not found"));
    }

    @Test
    void illegalTransitionExceptionMapsTo400() throws Exception {
        mockMvc.perform(post("/test/api-exception-handler/illegal-transition"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Transition from 'TODO' to 'DONE' is not defined for item type 'STORY'"));
    }

    @Test
    void beanValidationFailureMapsTo400WithFieldDetail() throws Exception {
        mockMvc.perform(post("/test/api-exception-handler/bean-validation")
                        .contentType("application/json")
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("name must not be blank"));
    }
}

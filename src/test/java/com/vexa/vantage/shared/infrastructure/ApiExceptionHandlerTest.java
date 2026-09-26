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

package com.vexa.vantage.delivery.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de la solicitud de {@code POST /api/v1/projects}: los datos
 * mínimos para crear un proyecto. El {@code tenantId}/{@code actorId} NO
 * viajan en el cuerpo — se resuelven del actor autenticado (JWT), nunca del
 * payload del cliente.
 */
public record ProjectRequest(

        @NotBlank
        @Size(max = 100)
        String name,

        String description) {
}

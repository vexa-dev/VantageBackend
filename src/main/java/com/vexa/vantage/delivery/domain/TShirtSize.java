package com.vexa.vantage.delivery.domain;

/**
 * Estimación de esfuerzo en talla de camiseta para un {@link WorkItem},
 * mapeada a la columna nullable {@code work_item.tshirt_size} (decisión de
 * usuario 2026-09-27: columna dedicada con {@code CHECK}, no un valor dentro
 * de un JSON de campos por tipo).
 *
 * <p>Deliberadamente distinto del enum legado {@code com.vexa.vantage.model.TShirtSize}
 * (mismos cinco valores) — este contexto delimitado no reutiliza tipos de
 * dominio del modelo legado que está siendo reemplazado.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}).
 */
public enum TShirtSize {
    XS,
    S,
    M,
    L,
    XL
}

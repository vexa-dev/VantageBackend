package com.vexa.vantage.delivery.domain;

/**
 * Estado de ciclo de vida de un {@link Project}, tal como se persiste en
 * {@code project.status} ({@code 'ACTIVE'|'ARCHIVED'}).
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson.
 */
public enum ProjectStatus {
    ACTIVE,
    ARCHIVED
}

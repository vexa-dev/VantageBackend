package com.vexa.vantage.identity.domain;

/**
 * Etiqueta Scrum opcional de una membresía de proyecto, tal como se persiste
 * en {@code project_membership.scrum_label}
 * ({@code NULL|'PRODUCT_OWNER'|'SCRUM_MASTER'|'DEVELOPER'}).
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson.
 */
public enum ScrumLabel {
    PRODUCT_OWNER,
    SCRUM_MASTER,
    DEVELOPER
}

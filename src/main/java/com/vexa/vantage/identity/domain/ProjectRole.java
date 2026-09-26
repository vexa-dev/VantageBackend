package com.vexa.vantage.identity.domain;

/**
 * Rol de un usuario dentro de un proyecto específico, tal como se persiste
 * en {@code project_membership.role}
 * ({@code 'OWNER'|'ADMIN'|'MEMBER'|'VIEWER'}).
 *
 * <p>El orden de declaración define el nivel jerárquico de menor a mayor
 * privilegio ({@code VIEWER < MEMBER < ADMIN < OWNER}).
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson.
 */
public enum ProjectRole {
    VIEWER,
    MEMBER,
    ADMIN,
    OWNER;

    /**
     * Nivel jerárquico numérico del rol (mayor es más privilegio). Se basa
     * en {@link #ordinal()} porque el orden de declaración de esta
     * enumeración ya coincide con la jerarquía deseada; es la única fuente
     * de verdad para el orden, usada por {@link AuthorizationPolicy} para
     * no duplicarlo.
     */
    public int level() {
        return ordinal();
    }
}

package com.vexa.vantage.identity.domain;

/**
 * Rol de un usuario a nivel de tenant, tal como se persiste en la columna
 * {@code app_user.tenant_role} ({@code 'TENANT_ADMIN'|'MEMBER'}).
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson.
 */
public enum TenantRole {
    TENANT_ADMIN,
    MEMBER
}

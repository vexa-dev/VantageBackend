package com.vexa.vantage.identity.domain;

/**
 * Política de autorización de dominio puro: recibe el rol YA RESUELTO por
 * el llamador (nunca {@code projectId}/{@code userId}, ni realiza consultas)
 * y aplica las reglas de jerarquía de rol de proyecto y de administración
 * de tenant.
 *
 * <p>La resolución real del rol de un usuario contra la base de datos es
 * responsabilidad de la capa de aplicación en fases posteriores.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}). El orden jerárquico de {@link ProjectRole}
 * vive únicamente en {@link ProjectRole#level()}; esta clase lo consume en
 * lugar de duplicarlo.
 */
public final class AuthorizationPolicy {

    private AuthorizationPolicy() {
    }

    /**
     * Exige que {@code actual} sea al menos tan privilegiado como
     * {@code minimum} en la jerarquía {@code VIEWER < MEMBER < ADMIN < OWNER}.
     *
     * @throws InsufficientPermissionException si {@code actual} es menos
     *                                          privilegiado que {@code minimum}
     */
    public static void requireAtLeast(ProjectRole actual, ProjectRole minimum) {
        if (actual.level() < minimum.level()) {
            throw new InsufficientPermissionException(
                    "Role '%s' does not meet the required minimum role '%s'".formatted(actual, minimum));
        }
    }

    /**
     * Exige que {@code actual} sea {@link TenantRole#TENANT_ADMIN}.
     *
     * @throws InsufficientPermissionException si {@code actual} no es administrador de tenant
     */
    public static void requireTenantAdmin(TenantRole actual) {
        if (actual != TenantRole.TENANT_ADMIN) {
            throw new InsufficientPermissionException(
                    "Role '%s' is not a tenant administrator".formatted(actual));
        }
    }
}

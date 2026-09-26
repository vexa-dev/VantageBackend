package com.vexa.vantage.identity.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica {@link AuthorizationPolicy}: dominio puro que recibe roles ya
 * resueltos por el llamador (sin consultar {@code projectId}/{@code userId})
 * y aplica las reglas de autorización de jerarquía de proyecto y de
 * administración de tenant.
 */
class AuthorizationPolicyTest {

    @Test
    void requireAtLeastAllowsEqualRole() {
        assertThatCode(() -> AuthorizationPolicy.requireAtLeast(ProjectRole.ADMIN, ProjectRole.ADMIN))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAtLeastAllowsHigherRole() {
        assertThatCode(() -> AuthorizationPolicy.requireAtLeast(ProjectRole.OWNER, ProjectRole.VIEWER))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAtLeastRejectsLowerRole() {
        assertThatThrownBy(() -> AuthorizationPolicy.requireAtLeast(ProjectRole.VIEWER, ProjectRole.MEMBER))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void requireAtLeastRejectsMemberBelowAdmin() {
        assertThatThrownBy(() -> AuthorizationPolicy.requireAtLeast(ProjectRole.MEMBER, ProjectRole.ADMIN))
                .isInstanceOf(InsufficientPermissionException.class);
    }

    @Test
    void requireTenantAdminAllowsTenantAdmin() {
        assertThatCode(() -> AuthorizationPolicy.requireTenantAdmin(TenantRole.TENANT_ADMIN))
                .doesNotThrowAnyException();
    }

    @Test
    void requireTenantAdminRejectsMember() {
        assertThatThrownBy(() -> AuthorizationPolicy.requireTenantAdmin(TenantRole.MEMBER))
                .isInstanceOf(InsufficientPermissionException.class);
    }
}

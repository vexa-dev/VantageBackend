package com.vexa.vantage.identity.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;
import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica los invariantes de dominio de {@link AppUser}: {@code fullName}
 * requerido, {@code disabledAt} nulo/asignable, y {@code tenantRole} con un
 * valor por defecto cuando no se especifica.
 */
class AppUserTest {

    private static final UserId USER_ID = UserId.of("user-1");
    private static final TenantId TENANT_ID = TenantId.of("tenant-1");

    @Test
    void createsUserWithDefaultTenantRoleAndNoDisabledAt() {
        AppUser user = new AppUser(USER_ID, TENANT_ID, "ana@acme.com", "hash", "Ana Torres");

        assertThat(user.fullName()).isEqualTo("Ana Torres");
        assertThat(user.tenantRole()).isEqualTo(TenantRole.MEMBER);
        assertThat(user.disabledAt()).isNull();
        assertThat(user.isDisabled()).isFalse();
    }

    @Test
    void rejectsBlankFullName() {
        assertThatThrownBy(() -> new AppUser(USER_ID, TENANT_ID, "ana@acme.com", "hash", "   "))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("AppUser.fullName");
    }

    @Test
    void disableSetsDisabledAtTimestamp() {
        AppUser user = new AppUser(USER_ID, TENANT_ID, "ana@acme.com", "hash", "Ana Torres");
        java.time.Instant now = java.time.Instant.parse("2026-01-01T00:00:00Z");

        user.disable(now);

        assertThat(user.disabledAt()).isEqualTo(now);
        assertThat(user.isDisabled()).isTrue();
    }

    @Test
    void enableClearsDisabledAt() {
        AppUser user = new AppUser(USER_ID, TENANT_ID, "ana@acme.com", "hash", "Ana Torres");
        user.disable(java.time.Instant.parse("2026-01-01T00:00:00Z"));

        user.enable();

        assertThat(user.disabledAt()).isNull();
        assertThat(user.isDisabled()).isFalse();
    }

    @Test
    void tenantRoleCanBeChanged() {
        AppUser user = new AppUser(USER_ID, TENANT_ID, "ana@acme.com", "hash", "Ana Torres");

        user.changeTenantRole(TenantRole.TENANT_ADMIN);

        assertThat(user.tenantRole()).isEqualTo(TenantRole.TENANT_ADMIN);
    }
}

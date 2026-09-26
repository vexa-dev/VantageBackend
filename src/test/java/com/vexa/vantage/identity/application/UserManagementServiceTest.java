package com.vexa.vantage.identity.application;

import com.vexa.vantage.identity.domain.AppUser;
import com.vexa.vantage.identity.domain.InsufficientPermissionException;
import com.vexa.vantage.identity.domain.TenantRole;
import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba unitaria de {@link UserManagementService} usando Mockito sobre
 * {@link AppUserRepositoryPort}: deshabilitar un usuario y cambiar su
 * {@code tenant_role}, ambas operaciones forzadas vía
 * {@link com.vexa.vantage.identity.domain.AuthorizationPolicy#requireTenantAdmin}.
 */
class UserManagementServiceTest {

    private static final UserId TARGET_USER_ID = UserId.of("user-1");
    private static final TenantId TENANT_ID = TenantId.of("tenant-1");

    private AppUserRepositoryPort userRepository;
    private UserManagementService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(AppUserRepositoryPort.class);
        service = new UserManagementService(userRepository);
    }

    private AppUser targetUser() {
        return new AppUser(TARGET_USER_ID, TENANT_ID, "ana@acme.com", "hash", "Ana Torres");
    }

    @Test
    void tenantAdminCanDisableAUser() {
        AppUser user = targetUser();
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

        service.disableUser(TenantRole.TENANT_ADMIN, TARGET_USER_ID);

        assertThat(user.isDisabled()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void memberCannotDisableAUser() {
        assertThatThrownBy(() -> service.disableUser(TenantRole.MEMBER, TARGET_USER_ID))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void tenantAdminCanReactivateADisabledUser() {
        AppUser user = targetUser();
        user.disable(Instant.now());
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

        service.reactivateUser(TenantRole.TENANT_ADMIN, TARGET_USER_ID);

        assertThat(user.isDisabled()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void memberCannotReactivateADisabledUser() {
        assertThatThrownBy(() -> service.reactivateUser(TenantRole.MEMBER, TARGET_USER_ID))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void tenantAdminCanChangeTenantRoleOfAUser() {
        AppUser user = targetUser();
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

        service.changeTenantRole(TenantRole.TENANT_ADMIN, TARGET_USER_ID, TenantRole.TENANT_ADMIN);

        assertThat(user.tenantRole()).isEqualTo(TenantRole.TENANT_ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    void memberCannotChangeTenantRoleOfAUser() {
        assertThatThrownBy(() -> service.changeTenantRole(TenantRole.MEMBER, TARGET_USER_ID, TenantRole.TENANT_ADMIN))
                .isInstanceOf(InsufficientPermissionException.class);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}

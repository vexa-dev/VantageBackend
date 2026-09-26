package com.vexa.vantage.identity.domain;

import com.vexa.vantage.shared.domain.DomainValidationException;
import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.domain.UserId;

import java.time.Instant;

/**
 * Entidad de dominio que representa un usuario de la aplicación, mapeada a
 * la tabla {@code app_user}.
 *
 * <p>Java puro: sin dependencia de Spring, JPA ni Jackson (verificado por
 * {@code ArchitectureRulesTest}). Invariantes:
 * <ul>
 *     <li>{@code fullName} es requerido (no nulo ni en blanco).</li>
 *     <li>{@code disabledAt} es nulo por defecto y puede asignarse/limpiarse
 *     mediante {@link #disable(Instant)} / {@link #enable()}.</li>
 *     <li>{@code tenantRole} tiene por defecto {@link TenantRole#MEMBER}
 *     cuando no se especifica explícitamente.</li>
 * </ul>
 */
public final class AppUser {

    private final UserId id;
    private final TenantId tenantId;
    private final String email;
    private final String passwordHash;
    private String fullName;
    private TenantRole tenantRole;
    private Instant disabledAt;

    public AppUser(UserId id, TenantId tenantId, String email, String passwordHash, String fullName) {
        this(id, tenantId, email, passwordHash, fullName, TenantRole.MEMBER, null);
    }

    public AppUser(
            UserId id,
            TenantId tenantId,
            String email,
            String passwordHash,
            String fullName,
            TenantRole tenantRole,
            Instant disabledAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = DomainValidationException.requireNonBlank(fullName, "AppUser.fullName");
        this.tenantRole = tenantRole != null ? tenantRole : TenantRole.MEMBER;
        this.disabledAt = disabledAt;
    }

    public UserId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String fullName() {
        return fullName;
    }

    public TenantRole tenantRole() {
        return tenantRole;
    }

    public Instant disabledAt() {
        return disabledAt;
    }

    public boolean isDisabled() {
        return disabledAt != null;
    }

    public void disable(Instant when) {
        this.disabledAt = when;
    }

    public void enable() {
        this.disabledAt = null;
    }

    public void changeTenantRole(TenantRole newRole) {
        this.tenantRole = newRole;
    }
}

package com.vexa.vantage.identity.infrastructure.persistence;

import com.vexa.vantage.identity.domain.TenantRole;
import com.vexa.vantage.shared.infrastructure.TenantFilterAspect;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.Instant;

/**
 * Entidad JPA que mapea la tabla {@code app_user}, separada del objeto de
 * dominio puro {@code AppUser}.
 *
 * <p>Declara el filtro Hibernate {@code tenantFilter} (habilitado/deshabilitado
 * por {@code TenantFilterAspect} alrededor de cada llamada al adapter) para
 * acotar las lecturas al tenant vigente en {@code TenantContext} cuando hay
 * uno establecido. La columna {@code tenant_id} es {@code BIGINT}, por lo que
 * la condición castea el parámetro string del filtro a {@code BIGINT} antes
 * de compararlo.
 */
@Entity
@Table(name = "app_user")
@FilterDef(name = TenantFilterAspect.TENANT_FILTER_NAME, parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = TenantFilterAspect.TENANT_FILTER_NAME, condition = "tenant_id = CAST(:tenantId AS BIGINT)")
public class AppUserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_role", nullable = false, length = 16)
    private TenantRole tenantRole;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected AppUserJpaEntity() {
        // Requerido por JPA.
    }

    /**
     * Constructor de conveniencia para crear una fila nueva (usado por
     * fixtures de prueba). {@code tenantRole} por defecto {@code MEMBER}
     * cuando es {@code null}, igual que el constructor corto del agregado de
     * dominio {@code AppUser}.
     */
    public AppUserJpaEntity(Long tenantId, String email, String passwordHash, String fullName, TenantRole tenantRole) {
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.tenantRole = tenantRole != null ? tenantRole : TenantRole.MEMBER;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Instant getDisabledAt() {
        return disabledAt;
    }

    public void setDisabledAt(Instant disabledAt) {
        this.disabledAt = disabledAt;
    }

    public TenantRole getTenantRole() {
        return tenantRole;
    }

    public void setTenantRole(TenantRole tenantRole) {
        this.tenantRole = tenantRole;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

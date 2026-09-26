package com.vexa.vantage.shared.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Entidad JPA de fixture exclusiva para pruebas, usada únicamente para
 * verificar de forma aislada el mecanismo genérico de acotación por tenant
 * (tenant-scoping) ({@code TenantFilterAspect}), antes de que exista
 * cualquier entidad de dominio real de Identity/Delivery/Collaboration.
 * Esto NO forma parte del modelo de dominio real.
 */
@Entity
@FilterDef(name = TenantFilterAspect.TENANT_FILTER_NAME, parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = TenantFilterAspect.TENANT_FILTER_NAME, condition = "tenant_id = :tenantId")
public class TestTenantScopedEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    private String name;

    protected TestTenantScopedEntity() {
        // JPA
    }

    public TestTenantScopedEntity(String tenantId, String name) {
        this.tenantId = tenantId;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }
}

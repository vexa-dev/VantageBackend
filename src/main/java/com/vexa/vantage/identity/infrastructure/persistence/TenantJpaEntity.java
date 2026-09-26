package com.vexa.vantage.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entidad JPA que mapea la tabla {@code tenant}, separada del objeto de
 * dominio puro {@code Tenant}.
 *
 * <p>{@code subscriptionType} y {@code createdAt} son columnas propias de la
 * tabla que el objeto de dominio {@code Tenant} todavía no modela (ese
 * agregado solo expone {@code id} y {@code name} por ahora); se conservan
 * aquí para que la entidad refleje el esquema completo, listas para cuando
 * una fase futura de gestión de tenants las necesite.
 */
@Entity
@Table(name = "tenant")
public class TenantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "subscription_type", nullable = false, length = 20)
    private String subscriptionType;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected TenantJpaEntity() {
        // Requerido por JPA.
    }

    /**
     * Constructor de conveniencia para crear una fila nueva (usado por
     * fixtures de prueba y, potencialmente, por un futuro adapter de alta de
     * tenants).
     */
    public TenantJpaEntity(String name, String subscriptionType) {
        this.name = name;
        this.subscriptionType = subscriptionType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

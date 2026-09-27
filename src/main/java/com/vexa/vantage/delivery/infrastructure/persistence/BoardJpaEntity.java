package com.vexa.vantage.delivery.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entidad JPA que mapea la tabla {@code board}, separada del objeto de
 * dominio puro {@code Board}.
 *
 * <p>{@code board} no tiene columna de tenant propia (el alcance de tenant se
 * deriva transitivamente del proyecto), así que no declara
 * {@code @FilterDef}/{@code @Filter} — el guard explícito basado en el join a
 * {@code project} queda para la fase 6 (Multi-Tenancy Hardening), que lista
 * explícitamente a {@code BoardJpaRepository} entre los seis adapters que lo
 * necesitan.
 */
@Entity
@Table(name = "board")
public class BoardJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected BoardJpaEntity() {
        // Requerido por JPA.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

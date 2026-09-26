package com.vexa.vantage.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de Spring Data para {@link TenantJpaEntity}. Sin adapter de
 * puerto propio todavía: la capa de aplicación no define
 * {@code TenantRepositoryPort} en esta fase, así que esta interfaz existe
 * únicamente como soporte de mapeo relacional para las fases futuras de
 * gestión de tenants.
 */
public interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, Long> {
}

package com.vexa.vantage.shared.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de Spring Data exclusivo para pruebas, para
 * {@link TestTenantScopedEntity}, usado únicamente para verificar de forma
 * aislada el mecanismo genérico de acotación por tenant (tenant-scoping).
 */
public interface TestTenantScopedEntityRepository extends JpaRepository<TestTenantScopedEntity, Long> {
}

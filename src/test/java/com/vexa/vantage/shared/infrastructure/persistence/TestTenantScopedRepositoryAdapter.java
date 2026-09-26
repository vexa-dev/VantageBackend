package com.vexa.vantage.shared.infrastructure.persistence;

import com.vexa.vantage.shared.infrastructure.TestTenantScopedEntity;
import com.vexa.vantage.shared.infrastructure.TestTenantScopedEntityRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sustituto exclusivo para pruebas de un {@code *RepositoryAdapter} real
 * (los adaptadores de Identity/Delivery/Collaboration llegarán en fases
 * posteriores). Su paquete ({@code ..infrastructure.persistence..}) y su
 * nombre {@code *RepositoryAdapter} coinciden intencionalmente con el
 * pointcut de {@code TenantFilterAspect}, de modo que el mecanismo genérico
 * de acotación por tenant (tenant-scoping) pueda verificarse antes de que
 * exista ningún adaptador real. Esto NO forma parte del modelo de dominio
 * real.
 */
@Component
public class TestTenantScopedRepositoryAdapter {

    private final TestTenantScopedEntityRepository repository;

    public TestTenantScopedRepositoryAdapter(TestTenantScopedEntityRepository repository) {
        this.repository = repository;
    }

    public List<TestTenantScopedEntity> findAll() {
        return repository.findAll();
    }
}

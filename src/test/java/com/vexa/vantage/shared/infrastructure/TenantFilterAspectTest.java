package com.vexa.vantage.shared.infrastructure;

import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.infrastructure.persistence.TestTenantScopedRepositoryAdapter;
import com.vexa.vantage.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

/**
 * Verifica el mecanismo genérico de acotación por tenant (tenant-scoping):
 * {@code TenantFilterAspect} debe habilitar el filtro Hibernate
 * {@code tenantFilter} (acotado al tenant vinculado en {@link TenantContext})
 * alrededor de cada llamada a {@code *RepositoryAdapter}, y volver a
 * deshabilitarlo una vez que la llamada retorna.
 *
 * <p>Usa una entidad/repositorio/adaptador de fixture exclusivos para
 * pruebas (ver {@link TestTenantScopedEntity}) ya que todavía no existe
 * ninguna entidad de dominio real.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({TenantFilterAspect.class, TestTenantScopedRepositoryAdapter.class, TenantFilterAspectTest.AspectProxyingConfig.class})
class TenantFilterAspectTest extends PostgresTestContainerConfig {

    static class AspectProxyingConfig {
        @Bean
        static AnnotationAwareAspectJAutoProxyCreator aspectJAutoProxyCreator() {
            return new AnnotationAwareAspectJAutoProxyCreator();
        }
    }

    @Autowired
    private TestTenantScopedEntityRepository repository;

    @Autowired
    private TestTenantScopedRepositoryAdapter adapter;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void adapterCallOnlySeesRowsOfTheTenantBoundInTenantContext() {
        repository.saveAndFlush(new TestTenantScopedEntity("tenant-acme", "acme-row"));
        repository.saveAndFlush(new TestTenantScopedEntity("tenant-globex", "globex-row"));

        TenantContext.set(TenantId.of("tenant-acme"));

        var visibleThroughAdapter = adapter.findAll();

        assertThat(visibleThroughAdapter)
                .extracting(TestTenantScopedEntity::getTenantId)
                .containsExactly("tenant-acme");
    }

    @Test
    void filterIsDisabledAgainAfterTheAdapterCallReturns() {
        repository.saveAndFlush(new TestTenantScopedEntity("tenant-acme", "acme-row"));
        repository.saveAndFlush(new TestTenantScopedEntity("tenant-globex", "globex-row"));

        TenantContext.set(TenantId.of("tenant-acme"));
        adapter.findAll();

        // Se omite el adaptador (y por lo tanto el aspecto) después de la llamada anterior:
        // si el filtro se hubiera filtrado (leak), aquí solo sería visible 1 fila (tenant-acme).
        var visibleWithoutTheAdapter = repository.findAll();

        assertThat(visibleWithoutTheAdapter)
                .extracting(TestTenantScopedEntity::getTenantId)
                .containsExactlyInAnyOrder("tenant-acme", "tenant-globex");
    }
}

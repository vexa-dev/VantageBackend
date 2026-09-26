package com.vexa.vantage.identity.infrastructure;

import com.vexa.vantage.identity.domain.AppUser;
import com.vexa.vantage.identity.domain.TenantRole;
import com.vexa.vantage.identity.infrastructure.persistence.AppUserJpaEntity;
import com.vexa.vantage.identity.infrastructure.persistence.AppUserRepositoryAdapter;
import com.vexa.vantage.identity.infrastructure.persistence.AppUserSpringDataRepository;
import com.vexa.vantage.identity.infrastructure.persistence.TenantJpaEntity;
import com.vexa.vantage.shared.domain.TenantId;
import com.vexa.vantage.shared.infrastructure.TenantContext;
import com.vexa.vantage.shared.infrastructure.TenantFilterAspect;
import com.vexa.vantage.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

/**
 * Prueba de integración (con PostgreSQL real vía Testcontainers) del mapeo
 * JPA de {@code app_user}: verifica el round-trip de {@code full_name},
 * {@code disabled_at} y {@code tenant_role} a través de
 * {@link AppUserRepositoryAdapter}, y que el filtro Hibernate
 * {@code tenantFilter} (habilitado por {@link TenantFilterAspect} alrededor
 * de cada llamada al adapter) restringe la lectura entre tenants distintos.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({TenantFilterAspect.class, AppUserRepositoryAdapter.class, AppUserJpaRepositoryIT.AspectProxyingConfig.class})
class AppUserJpaRepositoryIT extends PostgresTestContainerConfig {

    static class AspectProxyingConfig {
        @Bean
        static AnnotationAwareAspectJAutoProxyCreator aspectJAutoProxyCreator() {
            return new AnnotationAwareAspectJAutoProxyCreator();
        }
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AppUserSpringDataRepository rawRepository;

    @Autowired
    private AppUserRepositoryAdapter adapter;

    private Long tenantAId;
    private Long tenantBId;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private Long persistTenant(String name) {
        TenantJpaEntity persisted = entityManager.persistFlushFind(new TenantJpaEntity(name, "FREE"));
        return persisted.getId();
    }

    private Long persistAppUser(Long tenantId, String email, String fullName, TenantRole tenantRole) {
        AppUserJpaEntity persisted = entityManager.persistFlushFind(
                new AppUserJpaEntity(tenantId, email, "bcrypt-hash-" + email, fullName, tenantRole));
        return persisted.getId();
    }

    private void seedTwoTenantsWithOneUserEach() {
        tenantAId = persistTenant("Acme Corp");
        tenantBId = persistTenant("Globex Corp");
        persistAppUser(tenantAId, "ana@acme.com", "Ana Pérez", TenantRole.MEMBER);
        persistAppUser(tenantBId, "bob@globex.com", "Bob Smith", TenantRole.TENANT_ADMIN);
    }

    @Test
    void roundTripsFullNameDisabledAtAndTenantRoleThroughTheAdapter() {
        seedTwoTenantsWithOneUserEach();
        TenantContext.set(TenantId.of(String.valueOf(tenantAId)));

        AppUser fetched = adapter.findByEmail("ana@acme.com").orElseThrow();
        assertThat(fetched.fullName()).isEqualTo("Ana Pérez");
        assertThat(fetched.tenantRole()).isEqualTo(TenantRole.MEMBER);
        assertThat(fetched.disabledAt()).isNull();

        Instant disabledAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        fetched.disable(disabledAt);
        adapter.save(fetched);
        entityManager.flush();
        entityManager.clear();

        AppUserJpaEntity persisted = rawRepository.findByEmail("ana@acme.com").orElseThrow();
        assertThat(persisted.getDisabledAt()).isEqualTo(disabledAt);
        assertThat(persisted.getTenantRole()).isEqualTo(TenantRole.MEMBER);
        assertThat(persisted.getFullName()).isEqualTo("Ana Pérez");
    }

    @Test
    void adapterCallOnlySeesTheAppUserOfTheTenantBoundInTenantContext() {
        seedTwoTenantsWithOneUserEach();
        TenantContext.set(TenantId.of(String.valueOf(tenantAId)));

        Optional<AppUser> ownTenantUser = adapter.findByEmail("ana@acme.com");
        Optional<AppUser> otherTenantUser = adapter.findByEmail("bob@globex.com");

        assertThat(ownTenantUser).isPresent();
        assertThat(otherTenantUser).isEmpty();
    }
}

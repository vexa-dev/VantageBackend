package com.vexa.vantage.shared.infrastructure;

import com.vexa.vantage.shared.domain.TenantId;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Contrato unitario basado en Mockito para la lógica de advice de
 * {@link TenantFilterAspect} (habilitar-antes / deshabilitar-después
 * alrededor del join point), independiente de una sesión de Hibernate o
 * base de datos reales.
 *
 * <p>Esto complementa (pero no reemplaza) a {@link TenantFilterAspectTest},
 * que demuestra el mismo comportamiento de extremo a extremo contra un
 * Testcontainer de PostgreSQL real.
 */
class TenantFilterAspectUnitTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void enablesTheFilterWithTheCurrentTenantBeforeProceedingAndDisablesItAfter() throws Throwable {
        EntityManager entityManager = mock(EntityManager.class);
        Session session = mock(Session.class);
        Filter filter = mock(Filter.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter(TenantFilterAspect.TENANT_FILTER_NAME)).thenReturn(filter);
        when(filter.setParameter(any(), any())).thenReturn(filter);
        when(joinPoint.proceed()).thenReturn("result");

        TenantContext.set(TenantId.of("tenant-acme"));
        TenantFilterAspect aspect = new TenantFilterAspect(entityManager);

        Object result = aspect.scopeToCurrentTenant(joinPoint);

        assertThat(result).isEqualTo("result");
        verify(session, times(1)).enableFilter(TenantFilterAspect.TENANT_FILTER_NAME);
        verify(filter, times(1)).setParameter("tenantId", "tenant-acme");
        verify(session, times(1)).disableFilter(TenantFilterAspect.TENANT_FILTER_NAME);
    }

    @Test
    void doesNotTouchTheFilterWhenNoTenantIsBound() throws Throwable {
        EntityManager entityManager = mock(EntityManager.class);
        Session session = mock(Session.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(joinPoint.proceed()).thenReturn("result");

        TenantFilterAspect aspect = new TenantFilterAspect(entityManager);

        Object result = aspect.scopeToCurrentTenant(joinPoint);

        assertThat(result).isEqualTo("result");
        verify(session, never()).enableFilter(any());
        verify(session, never()).disableFilter(any());
    }

    @Test
    void stillDisablesTheFilterWhenTheJoinPointThrows() throws Throwable {
        EntityManager entityManager = mock(EntityManager.class);
        Session session = mock(Session.class);
        Filter filter = mock(Filter.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter(TenantFilterAspect.TENANT_FILTER_NAME)).thenReturn(filter);
        when(filter.setParameter(any(), any())).thenReturn(filter);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("repository failure"));

        TenantContext.set(TenantId.of("tenant-globex"));
        TenantFilterAspect aspect = new TenantFilterAspect(entityManager);

        assertThatThrownBy(() -> aspect.scopeToCurrentTenant(joinPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("repository failure");
        verify(session, times(1)).disableFilter(TenantFilterAspect.TENANT_FILTER_NAME);
    }
}

package com.vexa.vantage.shared.infrastructure;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Habilita el filtro Hibernate {@code tenantFilter}, acotado al tenant
 * vinculado en {@link TenantContext}, alrededor de cada llamada al adaptador
 * de repositorio, y lo vuelve a deshabilitar una vez que la llamada retorna
 * (con éxito o no).
 *
 * <p>Apunta a las clases {@code *RepositoryAdapter} bajo cualquier paquete
 * {@code *.infrastructure.persistence..}, de modo que se aplica de manera
 * uniforme a los adaptadores de persistencia de cada contexto delimitado
 * (bounded context) una vez que existan (Identity, Delivery y Collaboration
 * llegarán en fases posteriores), sin que esta clase del núcleo compartido
 * dependa de ninguno de ellos.
 */
@Aspect
@Component
public class TenantFilterAspect {

    /**
     * Nombre del filtro Hibernate que este aspecto habilita/deshabilita.
     * Todo par {@code @FilterDef}/{@code @Filter} que acote una entidad por
     * tenant debe usar exactamente este nombre.
     */
    public static final String TENANT_FILTER_NAME = "tenantFilter";

    private static final String TENANT_FILTER_PARAMETER = "tenantId";

    private final EntityManager entityManager;

    public TenantFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Around("execution(* com.vexa.vantage..infrastructure.persistence..*RepositoryAdapter.*(..))")
    public Object scopeToCurrentTenant(ProceedingJoinPoint joinPoint) throws Throwable {
        Session session = entityManager.unwrap(Session.class);
        boolean filterEnabled = TenantContext.get()
                .map(tenantId -> {
                    session.enableFilter(TENANT_FILTER_NAME).setParameter(TENANT_FILTER_PARAMETER, tenantId.value());
                    return true;
                })
                .orElse(false);
        try {
            return joinPoint.proceed();
        } finally {
            if (filterEnabled) {
                session.disableFilter(TENANT_FILTER_NAME);
            }
        }
    }
}

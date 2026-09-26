package com.vexa.vantage.shared.infrastructure;

import com.vexa.vantage.shared.domain.TenantId;

import java.util.Optional;

/**
 * Contenedor de alcance por hilo (thread-scoped) para el tenant que se está
 * atendiendo actualmente, respaldado por un {@link ThreadLocal}.
 *
 * <p>Habitualmente se completa al inicio de una solicitud (por ejemplo,
 * mediante un filtro de servlet que resuelve el tenant a partir del token de
 * acceso o de un encabezado) y es leído por {@code TenantFilterAspect} para
 * acotar el acceso a los repositorios, y se limpia al final de la solicitud
 * para evitar que un tenant se filtre entre reutilizaciones del pool de
 * hilos.
 */
public final class TenantContext {

    private static final ThreadLocal<TenantId> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    /**
     * Vincula {@code tenantId} al hilo actual.
     */
    public static void set(TenantId tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * @return el tenant vinculado al hilo actual, o vacío cuando no se ha
     * establecido ninguno.
     */
    public static Optional<TenantId> get() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    /**
     * @return el tenant vinculado al hilo actual.
     * @throws IllegalStateException cuando no se ha establecido ningún tenant en este hilo.
     */
    public static TenantId require() {
        TenantId tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant is set on the current thread");
        }
        return tenantId;
    }

    /**
     * Elimina el tenant vinculado al hilo actual, si lo hay.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

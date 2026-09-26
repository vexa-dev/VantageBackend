package com.vexa.vantage.shared.infrastructure;

import com.vexa.vantage.shared.domain.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Contrato de comportamiento para {@link TenantContext}: semántica de
 * set/get/require/clear con alcance ThreadLocal.
 */
class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void getIsEmptyWhenNoTenantHasBeenSet() {
        assertThat(TenantContext.get()).isEmpty();
    }

    @Test
    void setThenGetReturnsTheSameTenantId() {
        TenantId tenantId = TenantId.of("tenant-acme");

        TenantContext.set(tenantId);

        assertThat(TenantContext.get()).contains(tenantId);
    }

    @Test
    void requireReturnsTheSetTenantId() {
        TenantId tenantId = TenantId.of("tenant-globex");

        TenantContext.set(tenantId);

        assertThat(TenantContext.require()).isEqualTo(tenantId);
    }

    @Test
    void requireThrowsWhenNoTenantHasBeenSet() {
        assertThrows(IllegalStateException.class, TenantContext::require);
    }

    @Test
    void clearRemovesTheCurrentTenantSoGetBecomesEmptyAndRequireThrows() {
        TenantContext.set(TenantId.of("tenant-initech"));

        TenantContext.clear();

        assertThat(TenantContext.get()).isEmpty();
        assertThrows(IllegalStateException.class, TenantContext::require);
    }

    @Test
    void tenantContextIsIsolatedPerThread() throws InterruptedException {
        TenantContext.set(TenantId.of("tenant-main"));

        AtomicReference<Boolean> otherThreadSawEmptyContext = new AtomicReference<>(false);
        Thread otherThread = new Thread(() -> otherThreadSawEmptyContext.set(TenantContext.get().isEmpty()));
        otherThread.start();
        otherThread.join();

        assertThat(otherThreadSawEmptyContext.get()).isTrue();
        assertThat(TenantContext.require()).isEqualTo(TenantId.of("tenant-main"));
    }
}

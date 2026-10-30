package com.netspeedmeter.monitor;

import com.netspeedmeter.model.SpeedSample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledOnOs(OS.WINDOWS)
class WindowsNetworkSamplerTest {

    @Test
    void readsRealWindowsByteCountersAndComputesMbpsFromDeltas() throws Exception {
        WindowsNetworkSampler sampler = new WindowsNetworkSampler();
        SpeedSample first = sampler.sample();
        assertFalse(first.adapters().isEmpty(), "Windows should report at least one adapter counter");
        first.adapters().forEach(adapter -> {
            assertNotNull(adapter.name());
            assertTrue(adapter.receivedBytes() >= 0);
            assertTrue(adapter.sentBytes() >= 0);
        });
        assertNotNull(first.warning());

        SpeedSample second = sampler.sample();
        assertTrue(Double.isFinite(second.downloadMbps()));
        assertTrue(Double.isFinite(second.uploadMbps()));
        assertTrue(second.downloadMbps() >= 0);
        assertTrue(second.uploadMbps() >= 0);
        assertNotNull(second.activeAdapter());
        assertFalse(second.activeAdapter().name().isBlank());
        long rx = second.adapters().stream().mapToLong(adapter -> adapter.receivedBytes()).sum();
        long tx = second.adapters().stream().mapToLong(adapter -> adapter.sentBytes()).sum();
        assertTrue(rx >= 0 && tx >= 0);
    }

    @Test
    void formatMbpsDoesNotInventTraffic() {
        assertEquals("0.00", WindowsNetworkSampler.formatMbps(0));
        assertEquals("0.00", WindowsNetworkSampler.formatMbps(0.009));
        assertEquals("1.25", WindowsNetworkSampler.formatMbps(1.25));
    }
}

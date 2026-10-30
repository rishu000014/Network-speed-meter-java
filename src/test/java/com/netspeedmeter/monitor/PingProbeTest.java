package com.netspeedmeter.monitor;

import com.netspeedmeter.model.PingResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledOnOs(OS.WINDOWS)
class PingProbeTest {

    private final PingProbe probe = new PingProbe();

    @Test
    void pingLocalhostUsesWindowsPingExe() {
        PingResult result = probe.ping("127.0.0.1");
        assertEquals("127.0.0.1", result.host());
        assertTrue(result.reachable(), "loopback ping should succeed: " + result.detail());
        assertTrue(result.latencyMs() != null && result.latencyMs() >= 0);
        assertTrue(result.detail().toLowerCase().contains("reply"));
    }

    @Test
    void pingGoogleDnsUsesWindowsPingExe() {
        PingResult result = probe.ping("8.8.8.8");
        assertEquals("8.8.8.8", result.host());
        if (result.reachable()) {
            assertTrue(result.latencyMs() >= 0);
        } else {
            assertTrue(result.detail().toLowerCase().contains("timed out")
                    || result.detail().toLowerCase().contains("unreachable")
                    || result.detail().toLowerCase().contains("no reply"));
        }
    }

    @Test
    void rejectsUnsafePingHost() {
        PingResult result = probe.ping("8.8.8.8 & calc.exe");
        assertFalse(result.reachable());
        assertEquals("Invalid ping host", result.detail());
    }
}

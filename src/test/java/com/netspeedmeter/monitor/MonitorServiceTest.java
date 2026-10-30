package com.netspeedmeter.monitor;

import com.netspeedmeter.model.SpeedSample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledOnOs(OS.WINDOWS)
class MonitorServiceTest {

    @Test
    void startStopCollectsRealSamplesOnBackgroundThreadsThenCleansUp() throws Exception {
        MonitorService service = new MonitorService();
        CopyOnWriteArrayList<SpeedSample> samples = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);
        service.setSpeedListener(sample -> {
            samples.add(sample);
            latch.countDown();
        });
        service.start();
        assertTrue(service.isRunning());
        assertTrue(latch.await(40, TimeUnit.SECONDS), "should collect two live adapter samples");
        service.stop();
        assertFalse(service.isRunning());
        service.close();
        assertFalse(samples.isEmpty());
        assertTrue(samples.stream().allMatch(sample -> Double.isFinite(sample.downloadMbps())));
        assertTrue(samples.stream().allMatch(sample -> Double.isFinite(sample.uploadMbps())));
    }
}

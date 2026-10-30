package com.netspeedmeter.monitor;

import com.netspeedmeter.model.PingResult;
import com.netspeedmeter.model.SpeedSample;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class MonitorService implements AutoCloseable {

    private final WindowsNetworkSampler sampler = new WindowsNetworkSampler();
    private final PingProbe pingProbe = new PingProbe();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> speedTask;
    private ScheduledFuture<?> pingTask;
    private ScheduledFuture<?> adapterTask;
    private volatile String pingHost = "8.8.8.8";

    private Consumer<SpeedSample> speedListener = sample -> { };
    private Consumer<PingResult> pingListener = result -> { };
    private Consumer<String> errorListener = message -> { };

    public void setSpeedListener(Consumer<SpeedSample> speedListener) {
        this.speedListener = Objects.requireNonNull(speedListener);
    }

    public void setPingListener(Consumer<PingResult> pingListener) {
        this.pingListener = Objects.requireNonNull(pingListener);
    }

    public void setErrorListener(Consumer<String> errorListener) {
        this.errorListener = Objects.requireNonNull(errorListener);
    }

    public void setPingHost(String pingHost) {
        this.pingHost = pingHost;
    }

    public boolean isRunning() {
        return running.get();
    }

    public synchronized void start() {
        if (running.get()) {
            return;
        }
        sampler.reset();
        executor = Executors.newScheduledThreadPool(3, daemonFactory());
        running.set(true);
        speedTask = executor.scheduleWithFixedDelay(this::sampleSpeeds, 0, 400, TimeUnit.MILLISECONDS);
        pingTask = executor.scheduleWithFixedDelay(this::samplePing, 200, 2500, TimeUnit.MILLISECONDS);
        adapterTask = executor.scheduleWithFixedDelay(this::refreshAdapters, 0, 8, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        running.set(false);
        cancel(speedTask);
        cancel(pingTask);
        cancel(adapterTask);
        speedTask = null;
        pingTask = null;
        adapterTask = null;
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            executor = null;
        }
        sampler.reset();
    }

    private void sampleSpeeds() {
        if (!running.get()) {
            return;
        }
        try {
            SpeedSample sample = sampler.sample();
            if (running.get()) {
                speedListener.accept(sample);
            }
        } catch (Exception ex) {
            errorListener.accept("Network sampling error: " + ex.getMessage());
        }
    }

    private void refreshAdapters() {
        if (!running.get()) {
            return;
        }
        try {
            sampler.refreshAdapterDetails();
        } catch (Exception ex) {
            errorListener.accept("Adapter details error: " + ex.getMessage());
        }
    }

    private void samplePing() {
        if (!running.get()) {
            return;
        }
        try {
            PingResult result = pingProbe.ping(pingHost);
            if (running.get()) {
                pingListener.accept(result);
            }
        } catch (Exception ex) {
            errorListener.accept("Ping error: " + ex.getMessage());
        }
    }

    private static void cancel(ScheduledFuture<?> task) {
        if (task != null) {
            task.cancel(true);
        }
    }

    private static ThreadFactory daemonFactory() {
        AtomicInteger index = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("netspeed-monitor-" + index.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }

    @Override
    public void close() {
        stop();
    }
}

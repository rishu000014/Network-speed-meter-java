package com.netspeedmeter.monitor;

import com.netspeedmeter.model.AdapterInfo;
import com.netspeedmeter.model.SpeedSample;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Reads real Windows NIC byte counters via Get-NetAdapterStatistics.
 * Speeds are computed from successive samples, not invented values.
 */
public final class WindowsNetworkSampler {

    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(12);

    private Path statsScript;
    private Path infoScript;
    private Map<String, Long> previousRx = Map.of();
    private Map<String, Long> previousTx = Map.of();
    private Instant previousInstant;
    private volatile Map<String, AdapterInfo> adapterDetails = Map.of();

    public synchronized SpeedSample sample() throws Exception {
        Instant now = Instant.now();
        Map<String, long[]> counters = readCounters();
        if (counters.isEmpty()) {
            previousRx = Map.of();
            previousTx = Map.of();
            previousInstant = now;
            return new SpeedSample(now, 0, 0, AdapterInfo.empty(), List.of(), "No network adapters reported byte counters.");
        }

        double downloadMbps = 0;
        double uploadMbps = 0;
        String busiestName = counters.keySet().iterator().next();
        long busiestDelta = -1;
        String warning = null;

        if (previousInstant != null && !previousRx.isEmpty()) {
            double seconds = Math.max(0.05, Duration.between(previousInstant, now).toMillis() / 1000.0);
            for (Map.Entry<String, long[]> entry : counters.entrySet()) {
                String name = entry.getKey();
                long rx = entry.getValue()[0];
                long tx = entry.getValue()[1];
                long rxDelta = Math.max(0L, rx - previousRx.getOrDefault(name, rx));
                long txDelta = Math.max(0L, tx - previousTx.getOrDefault(name, tx));
                if (!previousRx.containsKey(name)) {
                    continue;
                }
                downloadMbps += bitsPerSecondToMbps(rxDelta, seconds);
                uploadMbps += bitsPerSecondToMbps(txDelta, seconds);
                long totalDelta = rxDelta + txDelta;
                if (totalDelta > busiestDelta) {
                    busiestDelta = totalDelta;
                    busiestName = name;
                }
            }
        } else {
            warning = "Calibrating first sample from Windows interface counters…";
        }

        Map<String, Long> nextRx = new LinkedHashMap<>();
        Map<String, Long> nextTx = new LinkedHashMap<>();
        List<AdapterInfo> adapters = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : counters.entrySet()) {
            nextRx.put(entry.getKey(), entry.getValue()[0]);
            nextTx.put(entry.getKey(), entry.getValue()[1]);
            adapters.add(merge(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        }

        previousRx = nextRx;
        previousTx = nextTx;
        previousInstant = now;
        AdapterInfo active = merge(busiestName,
                counters.get(busiestName)[0],
                counters.get(busiestName)[1]);
        return new SpeedSample(now, downloadMbps, uploadMbps, active, List.copyOf(adapters), warning);
    }

    public void refreshAdapterDetails() throws Exception {
        Map<String, AdapterInfo> details = new LinkedHashMap<>();
        for (String line : runScript(infoScript(), "adapter-info.ps1")) {
            if (!line.startsWith("NIC|")) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            if (parts.length < 8) {
                continue;
            }
            details.put(parts[1], new AdapterInfo(
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[4],
                    parts[5],
                    parseIntSafe(parts[6]),
                    parts[7],
                    0L,
                    0L
            ));
        }
        adapterDetails = Map.copyOf(details);
    }

    public synchronized void reset() {
        previousRx = Map.of();
        previousTx = Map.of();
        previousInstant = null;
    }

    private AdapterInfo merge(String name, long rx, long tx) {
        AdapterInfo detail = adapterDetails.get(name);
        if (detail == null) {
            return new AdapterInfo(name, "Windows network adapter", "—", "—", "Up", -1, "—", rx, tx);
        }
        return new AdapterInfo(
                detail.name(),
                detail.description(),
                detail.macAddress(),
                detail.linkSpeed(),
                detail.status(),
                detail.interfaceIndex(),
                detail.ipv4(),
                rx,
                tx
        );
    }

    private static double bitsPerSecondToMbps(long byteDelta, double seconds) {
        return (byteDelta * 8.0) / seconds / 1_000_000.0;
    }

    private Map<String, long[]> readCounters() throws Exception {
        Map<String, long[]> counters = new LinkedHashMap<>();
        for (String line : runScript(statsScript(), "adapter-stats.ps1")) {
            if (!line.startsWith("STAT|")) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            if (parts.length < 4) {
                continue;
            }
            counters.put(parts[1], new long[]{parseLongSafe(parts[2]), parseLongSafe(parts[3])});
        }
        return counters;
    }

    private List<String> runScript(Path script, String label) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy", "Bypass",
                "-File",
                script.toAbsolutePath().toString()
        );
        builder.redirectErrorStream(true);
        Process process = builder.start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(line.trim());
                }
            }
        } finally {
            boolean finished = process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Timed out running " + label);
            }
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException(label + " failed: " + String.join(" ", lines));
        }
        return lines;
    }

    private Path statsScript() throws Exception {
        if (statsScript == null || !Files.isRegularFile(statsScript)) {
            statsScript = materialize("/scripts/adapter-stats.ps1", "netspeed-adapter-stats");
        }
        return statsScript;
    }

    private Path infoScript() throws Exception {
        if (infoScript == null || !Files.isRegularFile(infoScript)) {
            infoScript = materialize("/scripts/adapter-info.ps1", "netspeed-adapter-info");
        }
        return infoScript;
    }

    private static Path materialize(String resource, String prefix) throws Exception {
        try (InputStream input = WindowsNetworkSampler.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing classpath resource " + resource);
            }
            Path temp = Files.createTempFile(prefix, ".ps1");
            temp.toFile().deleteOnExit();
            Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp;
        }
    }

    private static int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static long parseLongSafe(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public static String formatMbps(double mbps) {
        if (mbps < 0.01) {
            return "0.00";
        }
        if (mbps < 10) {
            return String.format(Locale.US, "%.2f", mbps);
        }
        if (mbps < 100) {
            return String.format(Locale.US, "%.1f", mbps);
        }
        return String.format(Locale.US, "%.0f", mbps);
    }
}

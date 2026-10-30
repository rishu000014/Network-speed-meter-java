package com.netspeedmeter.monitor;

import com.netspeedmeter.model.PingResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PingProbe {

    private static final Pattern TIME_PATTERN = Pattern.compile("time[=<]\\s*(\\d+)\\s*ms", Pattern.CASE_INSENSITIVE);

    private static final Pattern HOST_PATTERN = Pattern.compile("^[A-Za-z0-9.-]{1,253}$");

    public PingResult ping(String host) {
        String target = (host == null || host.isBlank()) ? "8.8.8.8" : host.trim();
        if (!HOST_PATTERN.matcher(target).matches()) {
            return new PingResult(target, false, null, "Invalid ping host");
        }
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder("ping.exe", "-n", "1", "-w", "2000", target);
            builder.redirectErrorStream(true);
            process = builder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            boolean finished = process.waitFor(6, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new PingResult(target, false, null, "Ping timed out waiting for Windows ping.exe");
            }
            String text = output.toString();
            Matcher matcher = TIME_PATTERN.matcher(text);
            if (matcher.find()) {
                int latency = Integer.parseInt(matcher.group(1));
                return new PingResult(target, true, latency, "Reply from " + target);
            }
            if (text.toLowerCase(Locale.ROOT).contains("destination host unreachable")) {
                return new PingResult(target, false, null, "Destination host unreachable");
            }
            if (text.toLowerCase(Locale.ROOT).contains("request timed out")
                    || text.toLowerCase(Locale.ROOT).contains("100% loss")) {
                return new PingResult(target, false, null, "Request timed out");
            }
            return new PingResult(target, false, null, "No reply from " + target);
        } catch (Exception ex) {
            return new PingResult(target, false, null, "Ping failed: " + ex.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}

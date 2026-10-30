package com.netspeedmeter.model;

public record PingResult(String host, boolean reachable, Integer latencyMs, String detail) {
    public static PingResult idle(String host) {
        return new PingResult(host, false, null, "Idle");
    }
}

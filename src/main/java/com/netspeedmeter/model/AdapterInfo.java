package com.netspeedmeter.model;

public record AdapterInfo(
        String name,
        String description,
        String macAddress,
        String linkSpeed,
        String status,
        int interfaceIndex,
        String ipv4,
        long receivedBytes,
        long sentBytes
) {
    public static AdapterInfo empty() {
        return new AdapterInfo("—", "No active adapter", "—", "—", "Unknown", -1, "—", 0L, 0L);
    }
}

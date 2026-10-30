package com.netspeedmeter.model;

import java.time.Instant;
import java.util.List;

public record SpeedSample(
        Instant timestamp,
        double downloadMbps,
        double uploadMbps,
        AdapterInfo activeAdapter,
        List<AdapterInfo> adapters,
        String warning
) {
}

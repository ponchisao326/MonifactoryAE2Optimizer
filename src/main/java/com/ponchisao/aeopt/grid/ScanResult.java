package com.ponchisao.aeopt.grid;

import com.ponchisao.aeopt.diagnostics.Finding;
import com.ponchisao.aeopt.diagnostics.GridTickSample;

import java.util.List;

public record ScanResult(GridTickSample sample, List<Finding> findings) {

    private static final ScanResult EMPTY = new ScanResult(
            new GridTickSample(0L, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, List.of(), List.of()),
            List.of());

    public static ScanResult empty() {
        return EMPTY;
    }

    public boolean isHealthy() {
        return findings.isEmpty();
    }
}

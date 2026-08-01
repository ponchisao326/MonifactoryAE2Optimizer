package com.ponchisao.aeopt.diagnostics;

import com.ponchisao.aeopt.tracking.CpuProgressTracker;
import com.ponchisao.aeopt.tracking.ProviderStallTracker;

public record DiagnosticContext(GridTickSample sample,
                                CpuProgressTracker cpuProgress,
                                ProviderStallTracker providerStalls) {
}

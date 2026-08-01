package com.ponchisao.aeopt.diagnostics;

import java.util.List;

public record GridTickSample(long tick,
                             double storedPower,
                             double maxStoredPower,
                             double averagePowerUsage,
                             double averagePowerInjection,
                             double starvedTickRatio,
                             List<CraftingCpuView> cpus,
                             List<PatternProviderView> providers) {

    public long countCpusWithJob() {
        return cpus.stream().filter(CraftingCpuView::hasJob).count();
    }

    public long countIdleCpusWithJob() {
        return cpus.stream().filter(CraftingCpuView::isIdleWhileHavingWork).count();
    }

    public long countBusyProviders() {
        return providers.stream().filter(PatternProviderView::busy).count();
    }

    public double storedPowerRatio() {
        if (maxStoredPower <= 0.0D) {
            return 0.0D;
        }
        return storedPower / maxStoredPower;
    }
}

package com.ponchisao.aeopt.diagnostics.probe;

import com.ponchisao.aeopt.config.AeOptConfig;
import com.ponchisao.aeopt.diagnostics.DiagnosticContext;
import com.ponchisao.aeopt.diagnostics.DiagnosticProbe;
import com.ponchisao.aeopt.diagnostics.Finding;
import com.ponchisao.aeopt.diagnostics.GridTickSample;

import java.util.List;

public final class EnergyStarvationProbe implements DiagnosticProbe {

    private static final double SIGNIFICANT_STARVED_RATIO = 0.10D;

    @Override
    public String id() {
        return "energy_starvation";
    }

    @Override
    public List<Finding> inspect(DiagnosticContext context) {
        GridTickSample sample = context.sample();
        if (!isCraftingGatedByPower(sample)) {
            return List.of();
        }
        return List.of(Finding.critical(id(), describe(sample)));
    }

    private boolean isCraftingGatedByPower(GridTickSample sample) {
        return hasJobsWaiting(sample) && wasStarvedOftenEnough(sample);
    }

    private boolean hasJobsWaiting(GridTickSample sample) {
        return sample.countCpusWithJob() > 0L;
    }

    private boolean wasStarvedOftenEnough(GridTickSample sample) {
        return sample.starvedTickRatio() >= SIGNIFICANT_STARVED_RATIO;
    }

    private String describe(GridTickSample sample) {
        return String.format(
                "Crafting was blocked by lack of power on %.0f%% of the sampled ticks. "
                        + "AE buffer at %.1f%% (%.0f / %.0f AE), usage %.0f AE/t against %.0f AE/t injected, "
                        + "with %d of %d CPUs holding a job. "
                        + "Add energy cells or raise power input before adding more co-processors, "
                        + "and consider enabling fairness.rotateCpuTickOrder so the same CPUs stop winning every tick.",
                sample.starvedTickRatio() * 100.0D,
                sample.storedPowerRatio() * 100.0D,
                sample.storedPower(),
                sample.maxStoredPower(),
                sample.averagePowerUsage(),
                sample.averagePowerInjection(),
                sample.countIdleCpusWithJob(),
                sample.countCpusWithJob());
    }
}

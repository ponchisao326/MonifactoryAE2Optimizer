package com.ponchisao.aeopt.diagnostics.probe;

import com.ponchisao.aeopt.config.AeOptConfig;
import com.ponchisao.aeopt.diagnostics.CraftingCpuView;
import com.ponchisao.aeopt.diagnostics.DiagnosticContext;
import com.ponchisao.aeopt.diagnostics.DiagnosticProbe;
import com.ponchisao.aeopt.diagnostics.Finding;

import java.util.ArrayList;
import java.util.List;

public final class StalledJobProbe implements DiagnosticProbe {

    @Override
    public String id() {
        return "stalled_job";
    }

    @Override
    public List<Finding> inspect(DiagnosticContext context) {
        List<Finding> findings = new ArrayList<>();
        long tick = context.sample().tick();
        for (CraftingCpuView view : context.sample().cpus()) {
            long idleTicks = context.cpuProgress().ticksSinceProgress(view.cpu(), tick);
            if (isStalled(view, idleTicks)) {
                findings.add(Finding.critical(id(), describe(view, idleTicks)));
            }
        }
        return findings;
    }

    private boolean isStalled(CraftingCpuView view, long idleTicks) {
        return view.hasJob() && idleTicks >= AeOptConfig.stalledJobThresholdTicks();
    }

    private String describe(CraftingCpuView view, long idleTicks) {
        return String.format(
                "CPU '%s' has made no progress for %d ticks (%.1f min) with %d items still pending, crafting %s.",
                view.name(),
                idleTicks,
                idleTicks / 1200.0D,
                view.remainingItemCount(),
                view.jobOutput());
    }
}

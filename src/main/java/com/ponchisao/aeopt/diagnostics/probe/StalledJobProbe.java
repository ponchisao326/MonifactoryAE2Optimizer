package com.ponchisao.aeopt.diagnostics.probe;

import com.ponchisao.aeopt.config.AeOptConfig;
import com.ponchisao.aeopt.diagnostics.CraftingCpuView;
import com.ponchisao.aeopt.diagnostics.DiagnosticContext;
import com.ponchisao.aeopt.diagnostics.DiagnosticProbe;
import com.ponchisao.aeopt.diagnostics.Finding;

import java.util.ArrayList;
import java.util.List;

public final class StalledJobProbe implements DiagnosticProbe {

    private static final double TICKS_PER_MINUTE = 1200.0D;

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
        return String.format("CPU at %s stalled for %.1f min while crafting %s. %s",
                view.position(),
                idleTicks / TICKS_PER_MINUTE,
                view.jobOutput(),
                describeCause(view));
    }

    private String describeCause(CraftingCpuView view) {
        if (view.isWaitingForMachineOutput()) {
            return "Waiting for " + view.describeWaitedItems()
                    + ". A machine took the ingredients and never returned that exact output.";
        }
        return "Nothing is pending return, so a pattern could not be pushed: "
                + "no provider accepted it, or an ingredient is missing.";
    }
}

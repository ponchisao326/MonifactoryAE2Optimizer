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
            return "Waiting for " + view.describeWaitedItems() + ". " + describeMissingReturn(view);
        }
        if (view.isDeadlocked()) {
            return "DEADLOCKED - every remaining step waits on another step of the same job and nothing is in "
                    + "flight, so no step can ever start. The intermediates were lost after the job began. "
                    + "Cancel and re-request. CPU holds: " + view.describeStoredItems() + ". "
                    + describeBlockedSteps(view);
        }
        if (view.hasBlockedPatterns()) {
            return "Nothing is pending return. CPU holds: " + view.describeStoredItems()
                    + ". " + describeBlockedSteps(view);
        }
        return "Nothing is pending return and no pattern is left to push, "
                + "so the job is waiting on a final output insertion that never completed.";
    }

    private String describeBlockedSteps(CraftingCpuView view) {
        String prefix = view.hasHiddenBlockedTasks()
                ? "Blocked on " + view.reportedPatternCount() + " of " + view.totalBlockedTasks() + " steps: "
                : "Blocked on: ";
        return prefix + view.describeBlockedPatterns();
    }

    private String describeMissingReturn(CraftingCpuView view) {
        if (view.hasOutputStrandedInNetwork()) {
            return "DEAD JOB - that output already exists in network storage but was never routed back to this CPU, "
                    + "so the job can never claim it. Cancel and re-request.";
        }
        return "The output does not exist anywhere in the network, so the machine still holds the ingredients "
                + "or never ran. Check its output hatch, power and whether the recipe actually fits.";
    }
}

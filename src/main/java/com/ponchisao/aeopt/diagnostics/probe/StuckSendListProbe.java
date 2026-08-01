package com.ponchisao.aeopt.diagnostics.probe;

import com.ponchisao.aeopt.config.AeOptConfig;
import com.ponchisao.aeopt.diagnostics.DiagnosticContext;
import com.ponchisao.aeopt.diagnostics.DiagnosticProbe;
import com.ponchisao.aeopt.diagnostics.Finding;
import com.ponchisao.aeopt.diagnostics.PatternProviderView;

import java.util.ArrayList;
import java.util.List;

public final class StuckSendListProbe implements DiagnosticProbe {

    @Override
    public String id() {
        return "stuck_send_list";
    }

    @Override
    public List<Finding> inspect(DiagnosticContext context) {
        List<Finding> findings = new ArrayList<>();
        long tick = context.sample().tick();
        for (PatternProviderView view : context.sample().providers()) {
            long stuckTicks = context.providerStalls().ticksStuck(view.position(), tick);
            if (isStuck(stuckTicks)) {
                findings.add(Finding.critical(id(), describe(view, stuckTicks)));
            }
        }
        return findings;
    }

    private boolean isStuck(long stuckTicks) {
        return stuckTicks >= AeOptConfig.stuckProviderThresholdTicks();
    }

    private String describe(PatternProviderView view, long stuckTicks) {
        String reason = view.isHoldingUndeliverableStacks()
                ? "its send direction was lost, so the stacks can never drain"
                : "its target has not accepted the pending stacks";
        return String.format(
                "Pattern provider at %s has been busy for %d ticks (%.1f min) with %d undelivered stacks: %s. "
                        + "It reports isBusy() and is skipped by every crafting CPU while in this state.",
                view.describePosition(),
                stuckTicks,
                stuckTicks / 1200.0D,
                view.pendingStackCount(),
                reason);
    }
}

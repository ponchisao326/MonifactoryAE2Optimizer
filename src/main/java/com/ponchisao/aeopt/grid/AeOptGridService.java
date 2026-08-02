package com.ponchisao.aeopt.grid;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridService;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.energy.IEnergyService;
import com.ponchisao.aeopt.config.AeOptConfig;
import com.ponchisao.aeopt.diagnostics.CraftingCpuView;
import com.ponchisao.aeopt.diagnostics.DiagnosticContext;
import com.ponchisao.aeopt.diagnostics.DiagnosticsRegistry;
import com.ponchisao.aeopt.diagnostics.Finding;
import com.ponchisao.aeopt.diagnostics.GridTickSample;
import com.ponchisao.aeopt.diagnostics.PatternProviderView;
import com.ponchisao.aeopt.tracking.CpuProgressTracker;
import com.ponchisao.aeopt.tracking.ProviderStallTracker;
import com.ponchisao.aeopt.tracking.StarvationCounter;
import net.minecraft.core.GlobalPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AeOptGridService implements IGridService, IGridServiceProvider {

    private static final Logger LOGGER = LogManager.getLogger("aeopt");

    private final IGrid grid;
    private final CpuProgressTracker cpuProgress = new CpuProgressTracker();
    private final ProviderStallTracker providerStalls = new ProviderStallTracker();
    private final StarvationCounter starvation = new StarvationCounter();
    private final StallRecoveryService recovery;

    private volatile ScanResult latestResult = ScanResult.empty();

    private long tick;
    private long lastProbeTick;
    private long lastWarningTick = Long.MIN_VALUE;

    public AeOptGridService(IGrid grid) {
        this.grid = grid;
        this.recovery = new StallRecoveryService(grid);
    }

    @Override
    public void onServerEndTick() {
        if (!AeOptConfig.isDiagnosticsEnabled()) {
            return;
        }
        tick++;
        starvation.observe(isCraftingBlockedByPower());
        if (!isProbeDue()) {
            return;
        }
        runScheduledProbe();
    }

    public ScanResult scanNow() {
        ScanResult result = evaluate();
        latestResult = result;
        return result;
    }

    public ScanResult latestResult() {
        return latestResult;
    }

    private void runScheduledProbe() {
        lastProbeTick = tick;
        ScanResult result = evaluate();
        latestResult = result;
        starvation.reset();
        logFindingsIfDue(result.findings());
        recovery.recover(result.sample(), providerStalls, cpuProgress);
    }

    private ScanResult evaluate() {
        GridTickSample sample = buildSample();
        updateTrackers(sample);
        List<Finding> findings = DiagnosticsRegistry.runAll(
                new DiagnosticContext(sample, cpuProgress, providerStalls));
        return new ScanResult(sample, findings);
    }

    private GridTickSample buildSample() {
        IEnergyService energy = grid.getEnergyService();
        return new GridTickSample(
                tick,
                energy.getStoredPower(),
                energy.getMaxStoredPower(),
                energy.getAvgPowerUsage(),
                energy.getAvgPowerInjection(),
                starvation.starvedRatio(),
                NetworkInspector.inspectCpus(grid),
                NetworkInspector.inspectProviders(grid));
    }

    private void updateTrackers(GridTickSample sample) {
        Set<ICraftingCPU> presentCpus = new HashSet<>();
        for (CraftingCpuView view : sample.cpus()) {
            cpuProgress.observe(view, sample.tick());
            presentCpus.add(view.cpu());
        }
        cpuProgress.retainOnly(presentCpus);

        Set<GlobalPos> presentProviders = new HashSet<>();
        for (PatternProviderView view : sample.providers()) {
            providerStalls.observe(view, sample.tick());
            presentProviders.add(view.position());
        }
        providerStalls.retainOnly(presentProviders);
    }

    private boolean isProbeDue() {
        return tick - lastProbeTick >= AeOptConfig.probeIntervalTicks();
    }

    private boolean isCraftingBlockedByPower() {
        return hasCraftingDemand() && hasDepletedEnergyBuffer();
    }

    private boolean hasCraftingDemand() {
        return grid.getCraftingService().isRequestingAny();
    }

    private boolean hasDepletedEnergyBuffer() {
        IEnergyService energy = grid.getEnergyService();
        double maxStored = energy.getMaxStoredPower();
        if (maxStored <= 0.0D) {
            return false;
        }
        return energy.getStoredPower() / maxStored <= AeOptConfig.starvedEnergyRatio();
    }

    private void logFindingsIfDue(List<Finding> findings) {
        if (findings.isEmpty() || !AeOptConfig.isWarningLoggingEnabled()) {
            return;
        }
        if (!hasWarningCooldownElapsed()) {
            return;
        }
        lastWarningTick = tick;
        for (Finding finding : findings) {
            LOGGER.warn("[{}] {}", finding.code(), finding.message());
        }
    }

    private boolean hasWarningCooldownElapsed() {
        return tick - lastWarningTick >= AeOptConfig.warningCooldownTicks();
    }
}

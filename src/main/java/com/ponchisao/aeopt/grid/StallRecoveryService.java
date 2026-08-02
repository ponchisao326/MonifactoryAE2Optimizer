package com.ponchisao.aeopt.grid;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import appeng.helpers.patternprovider.PatternProviderLogic;
import com.ponchisao.aeopt.config.AeOptConfig;
import com.ponchisao.aeopt.diagnostics.CraftingCpuView;
import com.ponchisao.aeopt.diagnostics.GridTickSample;
import com.ponchisao.aeopt.diagnostics.PatternProviderView;
import com.ponchisao.aeopt.mixin.PatternProviderLogicAccessor;
import com.ponchisao.aeopt.tracking.CpuProgressTracker;
import com.ponchisao.aeopt.tracking.ProviderStallTracker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public final class StallRecoveryService {

    private static final Logger LOGGER = LogManager.getLogger("aeopt");

    private final IGrid grid;

    public StallRecoveryService(IGrid grid) {
        this.grid = grid;
    }

    public void recover(GridTickSample sample, ProviderStallTracker providerStalls, CpuProgressTracker cpuProgress) {
        releaseStuckProviders(sample, providerStalls);
        cancelDeadJobs(sample, cpuProgress);
    }

    private void releaseStuckProviders(GridTickSample sample, ProviderStallTracker providerStalls) {
        if (!AeOptConfig.isProviderRecoveryEnabled()) {
            return;
        }
        for (PatternProviderView view : sample.providers()) {
            long stuckTicks = providerStalls.ticksStuck(view.position(), sample.tick());
            if (shouldReleaseProvider(stuckTicks)) {
                releaseProvider(view);
            }
        }
    }

    private boolean shouldReleaseProvider(long stuckTicks) {
        return stuckTicks >= AeOptConfig.providerRecoveryThresholdTicks();
    }

    private void releaseProvider(PatternProviderView view) {
        PatternProviderLogic logic = view.logic();
        if (!(logic instanceof PatternProviderLogicAccessor accessor)) {
            return;
        }
        List<GenericStack> sendList = accessor.aeopt$getSendList();
        if (sendList.isEmpty()) {
            return;
        }
        int returned = returnStacksToNetwork(sendList);
        if (sendList.isEmpty()) {
            accessor.aeopt$setSendDirection(null);
        }
        logic.saveChanges();
        LOGGER.warn("Released stuck pattern provider at {}: returned {} stack(s) to network storage",
                view.describePosition(), returned);
    }

    private int returnStacksToNetwork(List<GenericStack> sendList) {
        MEStorage storage = grid.getStorageService().getInventory();
        IActionSource source = IActionSource.empty();
        int returned = 0;
        for (int index = sendList.size() - 1; index >= 0; index--) {
            GenericStack stack = sendList.get(index);
            long inserted = storage.insert(stack.what(), stack.amount(), Actionable.MODULATE, source);
            if (inserted >= stack.amount()) {
                sendList.remove(index);
                returned++;
            } else if (inserted > 0L) {
                sendList.set(index, new GenericStack(stack.what(), stack.amount() - inserted));
            }
        }
        return returned;
    }

    private void cancelDeadJobs(GridTickSample sample, CpuProgressTracker cpuProgress) {
        if (!AeOptConfig.isDeadJobCancellationEnabled()) {
            return;
        }
        for (CraftingCpuView view : sample.cpus()) {
            long idleTicks = cpuProgress.ticksSinceProgress(view.cpu(), sample.tick());
            if (shouldCancelJob(view, idleTicks)) {
                cancelJob(view);
            }
        }
    }

    private boolean shouldCancelJob(CraftingCpuView view, long idleTicks) {
        return view.hasJob()
                && idleTicks >= AeOptConfig.deadJobCancellationThresholdTicks()
                && isProvablyUnrecoverable(view);
    }

    private boolean isProvablyUnrecoverable(CraftingCpuView view) {
        return view.hasOutputStrandedInNetwork()
                || view.blockedPatterns().stream().anyMatch(pattern -> pattern.isUnrecoverable());
    }

    private void cancelJob(CraftingCpuView view) {
        ICraftingCPU cpu = view.cpu();
        LOGGER.warn("Cancelling dead crafting job on CPU at {} ({}). Its inventory returns to network storage",
                view.position(), view.jobOutput());
        cpu.cancelJob();
    }
}

package com.ponchisao.aeopt.grid;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CraftingJobStatus;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.ponchisao.aeopt.diagnostics.CraftingCpuView;
import com.ponchisao.aeopt.diagnostics.PatternProviderView;
import com.ponchisao.aeopt.mixin.CraftingCpuLogicAccessor;
import com.ponchisao.aeopt.mixin.PatternProviderLogicAccessor;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class NetworkInspector {

    private static final String NO_ACTIVE_JOB = "nothing";

    private NetworkInspector() {
    }

    public static List<CraftingCpuView> inspectCpus(IGrid grid) {
        List<CraftingCpuView> views = new ArrayList<>();
        for (ICraftingCPU cpu : grid.getCraftingService().getCpus()) {
            views.add(toCpuView(cpu));
        }
        return List.copyOf(views);
    }

    public static List<PatternProviderView> inspectProviders(IGrid grid) {
        List<PatternProviderView> views = new ArrayList<>();
        for (Class<?> machineClass : grid.getMachineClasses()) {
            if (!isPatternProviderClass(machineClass)) {
                continue;
            }
            collectProvidersOfClass(grid, machineClass, views);
        }
        return List.copyOf(views);
    }

    private static void collectProvidersOfClass(IGrid grid, Class<?> machineClass, List<PatternProviderView> target) {
        for (IGridNode node : grid.getMachineNodes(machineClass)) {
            PatternProviderLogicHost host = asPatternProviderHost(node);
            if (host == null) {
                continue;
            }
            PatternProviderView view = toProviderView(host);
            if (view != null) {
                target.add(view);
            }
        }
    }

    private static boolean isPatternProviderClass(Class<?> machineClass) {
        return PatternProviderLogicHost.class.isAssignableFrom(machineClass);
    }

    private static PatternProviderLogicHost asPatternProviderHost(IGridNode node) {
        Object owner = node.getOwner();
        return owner instanceof PatternProviderLogicHost host ? host : null;
    }

    private static PatternProviderView toProviderView(PatternProviderLogicHost host) {
        BlockEntity blockEntity = host.getBlockEntity();
        if (!isPlacedInLoadedLevel(blockEntity)) {
            return null;
        }
        PatternProviderLogic logic = host.getLogic();
        if (!(logic instanceof PatternProviderLogicAccessor accessor)) {
            return null;
        }
        Level level = blockEntity.getLevel();
        GlobalPos position = GlobalPos.of(level.dimension(), blockEntity.getBlockPos());
        List<GenericStack> pendingStacks = accessor.aeopt$getSendList();
        return new PatternProviderView(
                position,
                logic.isBusy(),
                pendingStacks.size(),
                accessor.aeopt$getSendDirection() != null);
    }

    private static boolean isPlacedInLoadedLevel(BlockEntity blockEntity) {
        return blockEntity != null && blockEntity.getLevel() != null;
    }

    private static CraftingCpuView toCpuView(ICraftingCPU cpu) {
        CraftingJobStatus status = cpu.getJobStatus();
        boolean hasJob = status != null;
        return new CraftingCpuView(
                cpu,
                readName(cpu),
                hasJob,
                readRemainingItemCount(status),
                readPatternsPushed(cpu),
                cpu.getCoProcessors(),
                describeJobOutput(status));
    }

    private static String readName(ICraftingCPU cpu) {
        return cpu.getName() == null ? "unnamed" : cpu.getName().getString();
    }

    private static long readRemainingItemCount(CraftingJobStatus status) {
        if (status == null) {
            return 0L;
        }
        return Math.max(0L, status.totalItems() - status.progress());
    }

    private static String describeJobOutput(CraftingJobStatus status) {
        if (status == null || status.crafting() == null) {
            return NO_ACTIVE_JOB;
        }
        GenericStack crafting = status.crafting();
        return crafting.amount() + "x " + crafting.what().getDisplayName().getString();
    }

    private static int readPatternsPushed(ICraftingCPU cpu) {
        if (!(cpu instanceof CraftingCPUCluster cluster)) {
            return 0;
        }
        if (!(cluster.craftingLogic instanceof CraftingCpuLogicAccessor accessor)) {
            return 0;
        }
        int[] usedOps = accessor.aeopt$getUsedOps();
        return usedOps[0] + usedOps[1] + usedOps[2];
    }
}

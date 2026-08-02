package com.ponchisao.aeopt.grid;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CraftingJobStatus;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.ponchisao.aeopt.diagnostics.BlockedPattern;
import com.ponchisao.aeopt.diagnostics.CraftingCpuView;
import com.ponchisao.aeopt.diagnostics.ItemAmount;
import com.ponchisao.aeopt.diagnostics.PatternProviderView;
import com.ponchisao.aeopt.diagnostics.WaitedItem;
import com.ponchisao.aeopt.mixin.CraftingCpuLogicAccessor;
import com.ponchisao.aeopt.mixin.PatternProviderLogicAccessor;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NetworkInspector {

    private static final String NO_ACTIVE_JOB = "nothing";
    private static final String UNKNOWN_POSITION = "unknown position";

    private NetworkInspector() {
    }

    public static List<CraftingCpuView> inspectCpus(IGrid grid) {
        List<CraftingCpuView> views = new ArrayList<>();
        for (ICraftingCPU cpu : grid.getCraftingService().getCpus()) {
            views.add(toCpuView(grid, cpu));
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

    private static CraftingCpuView toCpuView(IGrid grid, ICraftingCPU cpu) {
        CraftingJobStatus status = cpu.getJobStatus();
        List<WaitedItem> waitingFor = readWaitedItems(grid, cpu);
        int patternsPushed = readPatternsPushed(cpu);
        boolean potentiallyStalled = isPotentiallyStalled(status != null, patternsPushed);
        return new CraftingCpuView(
                cpu,
                readName(cpu),
                readPosition(cpu),
                status != null,
                sumWaitedAmounts(waitingFor),
                patternsPushed,
                cpu.getCoProcessors(),
                describeJobOutput(status),
                waitingFor,
                readStoredItems(cpu, potentiallyStalled),
                readBlockedPatterns(grid, cpu, potentiallyStalled));
    }

    private static List<ItemAmount> readStoredItems(ICraftingCPU cpu, boolean potentiallyStalled) {
        CraftingCpuLogic logic = readCraftingLogic(cpu);
        if (!potentiallyStalled || logic == null) {
            return List.of();
        }
        List<ItemAmount> items = new ArrayList<>();
        for (Object2LongMap.Entry<AEKey> entry : logic.getInventory().list) {
            if (entry.getLongValue() > 0L) {
                items.add(new ItemAmount(entry.getKey().getDisplayName().getString(), entry.getLongValue()));
            }
        }
        items.sort(Comparator.comparingLong(ItemAmount::amount).reversed());
        return List.copyOf(items);
    }

    private static List<BlockedPattern> readBlockedPatterns(IGrid grid,
                                                            ICraftingCPU cpu,
                                                            boolean potentiallyStalled) {
        if (!potentiallyStalled) {
            return List.of();
        }
        CraftingCpuLogic logic = readCraftingLogic(cpu);
        if (logic == null) {
            return List.of();
        }
        return BlockedPatternAnalyzer.analyze(grid, logic);
    }

    private static boolean isPotentiallyStalled(boolean hasJob, int patternsPushed) {
        return hasJob && patternsPushed == 0;
    }

    private static String readName(ICraftingCPU cpu) {
        return cpu.getName() == null ? "unnamed" : cpu.getName().getString();
    }

    private static String readPosition(ICraftingCPU cpu) {
        if (!(cpu instanceof CraftingCPUCluster cluster) || cluster.getLevel() == null) {
            return UNKNOWN_POSITION;
        }
        return cluster.getBoundsMin().toShortString() + " in " + cluster.getLevel().dimension().location();
    }

    private static List<WaitedItem> readWaitedItems(IGrid grid, ICraftingCPU cpu) {
        CraftingCpuLogic logic = readCraftingLogic(cpu);
        if (logic == null) {
            return List.of();
        }
        KeyCounter networkStock = grid.getStorageService().getCachedInventory();
        Set<AEKey> keys = new HashSet<>();
        logic.getAllWaitingFor(keys);
        List<WaitedItem> items = new ArrayList<>(keys.size());
        for (AEKey key : keys) {
            long amount = logic.getWaitingFor(key);
            if (amount > 0L) {
                items.add(new WaitedItem(key.getDisplayName().getString(), amount, networkStock.get(key)));
            }
        }
        items.sort(Comparator.comparingLong(WaitedItem::amount).reversed());
        return List.copyOf(items);
    }

    private static long sumWaitedAmounts(List<WaitedItem> items) {
        long total = 0L;
        for (WaitedItem item : items) {
            total += item.amount();
        }
        return total;
    }

    private static String describeJobOutput(CraftingJobStatus status) {
        if (status == null || status.crafting() == null) {
            return NO_ACTIVE_JOB;
        }
        GenericStack crafting = status.crafting();
        return crafting.amount() + "x " + crafting.what().getDisplayName().getString();
    }

    private static int readPatternsPushed(ICraftingCPU cpu) {
        CraftingCpuLogic logic = readCraftingLogic(cpu);
        if (!(logic instanceof CraftingCpuLogicAccessor accessor)) {
            return 0;
        }
        int[] usedOps = accessor.aeopt$getUsedOps();
        return usedOps[0] + usedOps[1] + usedOps[2];
    }

    private static CraftingCpuLogic readCraftingLogic(ICraftingCPU cpu) {
        if (!(cpu instanceof CraftingCPUCluster cluster)) {
            return null;
        }
        return cluster.craftingLogic;
    }
}

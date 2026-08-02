package com.ponchisao.aeopt.grid;

import appeng.api.config.Actionable;
import appeng.api.config.LockCraftingMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.me.service.CraftingService;
import com.ponchisao.aeopt.diagnostics.BlockedPattern;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class BlockedPatternAnalyzer {

    private static final int MAX_REPORTED_PATTERNS = 6;
    private static final int MAX_REPORTED_LOCATIONS = 3;
    private static final String UNKNOWN_LOCATION = "unknown";

    private BlockedPatternAnalyzer() {
    }

    public static List<BlockedPattern> analyze(IGrid grid, CraftingCpuLogic logic) {
        ExecutingCraftingJob job = logic.getJob();
        if (job == null) {
            return List.of();
        }
        Map<IPatternDetails, ExecutingCraftingJob.TaskProgress> tasks = job.getTasks();
        Set<AEKey> producibleKeys = collectProducibleKeys(tasks);
        List<BlockedPattern> blocked = new ArrayList<>();
        for (Map.Entry<IPatternDetails, ExecutingCraftingJob.TaskProgress> task : tasks.entrySet()) {
            if (!hasRemainingRuns(task.getValue().value)) {
                continue;
            }
            blocked.add(inspectPattern(grid, logic.getInventory(), producibleKeys,
                    task.getKey(), task.getValue().value));
            if (blocked.size() >= MAX_REPORTED_PATTERNS) {
                break;
            }
        }
        return sortRootCausesFirst(blocked);
    }

    private static List<BlockedPattern> sortRootCausesFirst(List<BlockedPattern> blocked) {
        List<BlockedPattern> sorted = new ArrayList<>(blocked);
        sorted.sort((first, second) -> Integer.compare(rank(second), rank(first)));
        return List.copyOf(sorted);
    }

    private static int rank(BlockedPattern pattern) {
        if (pattern.areAllProvidersStuck()) {
            return 3;
        }
        if (pattern.isUnrecoverable()) {
            return 2;
        }
        if (pattern.hasNoProvider()) {
            return 1;
        }
        return 0;
    }

    private static Set<AEKey> collectProducibleKeys(Map<IPatternDetails, ExecutingCraftingJob.TaskProgress> tasks) {
        Set<AEKey> keys = new HashSet<>();
        for (Map.Entry<IPatternDetails, ExecutingCraftingJob.TaskProgress> task : tasks.entrySet()) {
            if (!hasRemainingRuns(task.getValue().value)) {
                continue;
            }
            for (GenericStack output : task.getKey().getOutputs()) {
                keys.add(output.what());
            }
        }
        return keys;
    }

    public static int countBlockedTasks(CraftingCpuLogic logic) {
        ExecutingCraftingJob job = logic.getJob();
        if (job == null) {
            return 0;
        }
        int total = 0;
        for (Map.Entry<IPatternDetails, ExecutingCraftingJob.TaskProgress> task : job.getTasks().entrySet()) {
            if (hasRemainingRuns(task.getValue().value)) {
                total++;
            }
        }
        return total;
    }

    private static boolean hasRemainingRuns(long remainingRuns) {
        return remainingRuns > 0L;
    }

    private static BlockedPattern inspectPattern(IGrid grid,
                                                 ListCraftingInventory inventory,
                                                 Set<AEKey> producibleKeys,
                                                 IPatternDetails details,
                                                 long remainingRuns) {
        List<ICraftingProvider> providers = collectProviders(grid, details);
        MissingIngredient missing = findMissingIngredient(inventory, details);
        return new BlockedPattern(
                describeOutput(details),
                remainingRuns,
                providers.size(),
                countStuckProviders(providers),
                describeLocations(providers),
                missing == null ? null : missing.description(),
                missing != null && producibleKeys.contains(missing.key()),
                describeRefusal(providers));
    }

    private static String describeRefusal(List<ICraftingProvider> providers) {
        if (findLockedProvider(providers) != null) {
            return "the provider is locked, waiting for its unlock event ("
                    + findLockedProvider(providers) + ")";
        }
        if (hasBlockingProvider(providers)) {
            return "blocking mode is on and the machine still holds inputs from the previous batch, "
                    + "so nothing new can be sent until it drains";
        }
        return "the machine cannot accept the inputs - a full hatch, or the pattern asks for more "
                + "than one hatch can hold";
    }

    private static LockCraftingMode findLockedProvider(List<ICraftingProvider> providers) {
        for (ICraftingProvider provider : providers) {
            if (provider instanceof PatternProviderLogic logic
                    && logic.getCraftingLockedReason() != LockCraftingMode.NONE) {
                return logic.getCraftingLockedReason();
            }
        }
        return null;
    }

    private static boolean hasBlockingProvider(List<ICraftingProvider> providers) {
        for (ICraftingProvider provider : providers) {
            if (provider instanceof PatternProviderLogic logic && logic.isBlocking()) {
                return true;
            }
        }
        return false;
    }

    private static String describeOutput(IPatternDetails details) {
        GenericStack output = details.getPrimaryOutput();
        if (output == null) {
            return "unknown output";
        }
        return output.amount() + "x " + output.what().getDisplayName().getString();
    }

    private static List<ICraftingProvider> collectProviders(IGrid grid, IPatternDetails details) {
        if (!(grid.getCraftingService() instanceof CraftingService craftingService)) {
            return List.of();
        }
        List<ICraftingProvider> providers = new ArrayList<>();
        for (ICraftingProvider provider : craftingService.getProviders(details)) {
            providers.add(provider);
        }
        return providers;
    }

    private static int countStuckProviders(List<ICraftingProvider> providers) {
        int stuck = 0;
        for (ICraftingProvider provider : providers) {
            if (provider.isBusy()) {
                stuck++;
            }
        }
        return stuck;
    }

    private static String describeLocations(List<ICraftingProvider> providers) {
        if (providers.isEmpty()) {
            return UNKNOWN_LOCATION;
        }
        return providers.stream()
                .limit(MAX_REPORTED_LOCATIONS)
                .map(BlockedPatternAnalyzer::describeLocation)
                .collect(Collectors.joining(", "));
    }

    private static String describeLocation(ICraftingProvider provider) {
        if (!(provider instanceof PatternProviderLogic logic)) {
            return UNKNOWN_LOCATION;
        }
        BlockEntity blockEntity = logic.host.getBlockEntity();
        if (blockEntity == null) {
            return UNKNOWN_LOCATION;
        }
        return blockEntity.getBlockPos().toShortString();
    }

    private static MissingIngredient findMissingIngredient(ListCraftingInventory inventory, IPatternDetails details) {
        for (IPatternDetails.IInput input : details.getInputs()) {
            long required = requiredAmount(input);
            if (canSatisfy(inventory, input, required)) {
                continue;
            }
            GenericStack[] possible = input.getPossibleInputs();
            if (possible.length == 0) {
                return new MissingIngredient(null, "an input with no valid candidates");
            }
            AEKey key = possible[0].what();
            return new MissingIngredient(key, required + "x " + key.getDisplayName().getString());
        }
        return null;
    }

    private static long requiredAmount(IPatternDetails.IInput input) {
        GenericStack[] possible = input.getPossibleInputs();
        if (possible.length == 0) {
            return 0L;
        }
        return possible[0].amount() * input.getMultiplier();
    }

    private static boolean canSatisfy(ListCraftingInventory inventory, IPatternDetails.IInput input, long required) {
        if (required <= 0L) {
            return true;
        }
        long available = 0L;
        for (GenericStack candidate : input.getPossibleInputs()) {
            for (AEKey template : inventory.findFuzzyTemplates(candidate.what())) {
                available += inventory.extract(template, required - available, Actionable.SIMULATE);
                if (available >= required) {
                    return true;
                }
            }
        }
        return false;
    }

    private record MissingIngredient(AEKey key, String description) {
    }
}

package com.ponchisao.aeopt.grid;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.GenericStack;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.me.service.CraftingService;
import com.ponchisao.aeopt.diagnostics.BlockedPattern;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class BlockedPatternAnalyzer {

    private static final int MAX_REPORTED_PATTERNS = 3;
    private static final int MAX_REPORTED_LOCATIONS = 3;
    private static final String UNKNOWN_LOCATION = "unknown";

    private BlockedPatternAnalyzer() {
    }

    public static List<BlockedPattern> analyze(IGrid grid, CraftingCpuLogic logic) {
        ExecutingCraftingJob job = logic.getJob();
        if (job == null) {
            return List.of();
        }
        List<BlockedPattern> blocked = new ArrayList<>();
        for (Map.Entry<IPatternDetails, ExecutingCraftingJob.TaskProgress> task : job.getTasks().entrySet()) {
            if (!hasRemainingRuns(task.getValue().value)) {
                continue;
            }
            blocked.add(inspectPattern(grid, logic.getInventory(), task.getKey(), task.getValue().value));
            if (blocked.size() >= MAX_REPORTED_PATTERNS) {
                break;
            }
        }
        return List.copyOf(blocked);
    }

    private static boolean hasRemainingRuns(long remainingRuns) {
        return remainingRuns > 0L;
    }

    private static BlockedPattern inspectPattern(IGrid grid,
                                                 ListCraftingInventory inventory,
                                                 IPatternDetails details,
                                                 long remainingRuns) {
        List<ICraftingProvider> providers = collectProviders(grid, details);
        return new BlockedPattern(
                describeOutput(details),
                remainingRuns,
                providers.size(),
                countStuckProviders(providers),
                describeLocations(providers),
                findMissingIngredient(inventory, details));
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

    private static String findMissingIngredient(ListCraftingInventory inventory, IPatternDetails details) {
        for (IPatternDetails.IInput input : details.getInputs()) {
            long required = requiredAmount(input);
            if (!canSatisfy(inventory, input, required)) {
                return describeInput(input, required);
            }
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
            available += inventory.extract(candidate.what(), required, Actionable.SIMULATE);
            if (available >= required) {
                return true;
            }
        }
        return false;
    }

    private static String describeInput(IPatternDetails.IInput input, long required) {
        GenericStack[] possible = input.getPossibleInputs();
        if (possible.length == 0) {
            return "an input with no valid candidates";
        }
        return required + "x " + possible[0].what().getDisplayName().getString();
    }
}

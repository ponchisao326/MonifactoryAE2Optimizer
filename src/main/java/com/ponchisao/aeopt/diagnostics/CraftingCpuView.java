package com.ponchisao.aeopt.diagnostics;

import appeng.api.networking.crafting.ICraftingCPU;

import java.util.List;
import java.util.stream.Collectors;

public record CraftingCpuView(ICraftingCPU cpu,
                              String name,
                              String position,
                              boolean hasJob,
                              long waitingForTotal,
                              int patternsPushedThisTick,
                              int coProcessors,
                              String jobOutput,
                              List<WaitedItem> waitingFor,
                              List<ItemAmount> storedItems,
                              List<BlockedPattern> blockedPatterns) {

    private static final int MAX_REPORTED_ITEMS = 4;
    private static final String NOTHING = "nothing";

    public boolean isIdleWhileHavingWork() {
        return hasJob && patternsPushedThisTick == 0;
    }

    public boolean isWaitingForMachineOutput() {
        return !waitingFor.isEmpty();
    }

    public boolean hasBlockedPatterns() {
        return !blockedPatterns.isEmpty();
    }

    public boolean hasOutputStrandedInNetwork() {
        return waitingFor.stream().anyMatch(WaitedItem::isAlreadyInNetwork);
    }

    public String describeWaitedItems() {
        if (waitingFor.isEmpty()) {
            return NOTHING;
        }
        return waitingFor.stream()
                .limit(MAX_REPORTED_ITEMS)
                .map(WaitedItem::describe)
                .collect(Collectors.joining(", "));
    }

    public String describeStoredItems() {
        if (storedItems.isEmpty()) {
            return NOTHING;
        }
        return storedItems.stream()
                .limit(MAX_REPORTED_ITEMS)
                .map(ItemAmount::describe)
                .collect(Collectors.joining(", "));
    }

    public String describeBlockedPatterns() {
        return blockedPatterns.stream()
                .map(BlockedPattern::describe)
                .collect(Collectors.joining(" | "));
    }
}

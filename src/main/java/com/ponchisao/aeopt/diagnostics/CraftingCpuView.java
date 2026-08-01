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
                              List<WaitingItem> waitingFor) {

    private static final int MAX_REPORTED_ITEMS = 4;

    public boolean isIdleWhileHavingWork() {
        return hasJob && patternsPushedThisTick == 0;
    }

    public boolean isWaitingForMachineOutput() {
        return !waitingFor.isEmpty();
    }

    public String describeWaitedItems() {
        return waitingFor.stream()
                .limit(MAX_REPORTED_ITEMS)
                .map(WaitingItem::describe)
                .collect(Collectors.joining(", "));
    }
}

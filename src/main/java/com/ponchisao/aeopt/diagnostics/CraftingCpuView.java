package com.ponchisao.aeopt.diagnostics;

import appeng.api.networking.crafting.ICraftingCPU;

public record CraftingCpuView(ICraftingCPU cpu,
                              String name,
                              boolean hasJob,
                              long remainingItemCount,
                              int patternsPushedThisTick,
                              int coProcessors,
                              String jobOutput) {

    public boolean isIdleWhileHavingWork() {
        return hasJob && patternsPushedThisTick == 0;
    }
}

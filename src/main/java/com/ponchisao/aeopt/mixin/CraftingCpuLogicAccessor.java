package com.ponchisao.aeopt.mixin;

import appeng.crafting.execution.CraftingCpuLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CraftingCpuLogic.class, remap = false)
public interface CraftingCpuLogicAccessor {

    @Accessor("usedOps")
    int[] aeopt$getUsedOps();
}

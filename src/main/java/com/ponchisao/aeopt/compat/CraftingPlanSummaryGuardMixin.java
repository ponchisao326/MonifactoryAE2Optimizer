package com.ponchisao.aeopt.compat;

import appeng.menu.me.crafting.CraftingPlanSummary;
import appeng.menu.me.crafting.CraftingPlanSummaryEntry;
import com.neuvillette.ae2ct.api.ICraftingPlanSummary;
import com.neuvillette.ae2ct.api.RecipeHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = CraftingPlanSummary.class, remap = false)
public abstract class CraftingPlanSummaryGuardMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aeopt$ensureCraftingTreeIsNeverNull(long usedBytes,
                                                     boolean simulation,
                                                     List<CraftingPlanSummaryEntry> entries,
                                                     CallbackInfo callbackInfo) {
        if (!(this instanceof ICraftingPlanSummary summary) || summary.getJob() != null) {
            return;
        }
        summary.setJob(new RecipeHelper(null, List.of()));
    }
}

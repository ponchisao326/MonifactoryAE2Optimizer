package com.ponchisao.aeopt.compat;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.CraftingPlan;
import com.neuvillette.ae2ct.api.RecipeHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = RecipeHelper.class, remap = false)
public abstract class RecipeHelperGuardMixin {

    @Inject(method = "fromCraftingPlan", at = @At("HEAD"), cancellable = true)
    private static void aeopt$skipPlansWithoutInputCandidates(CraftingPlan plan,
                                                              CallbackInfoReturnable<RecipeHelper> callbackInfo) {
        if (!hasPatternWithoutCandidates(plan)) {
            return;
        }
        callbackInfo.setReturnValue(new RecipeHelper(plan.finalOutput(), List.of()));
    }

    private static boolean hasPatternWithoutCandidates(CraftingPlan plan) {
        for (IPatternDetails details : plan.patternTimes().keySet()) {
            for (IPatternDetails.IInput input : details.getInputs()) {
                if (input.getPossibleInputs().length == 0) {
                    return true;
                }
            }
        }
        return false;
    }
}

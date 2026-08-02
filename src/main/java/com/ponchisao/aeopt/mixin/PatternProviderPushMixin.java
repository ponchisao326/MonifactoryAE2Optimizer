package com.ponchisao.aeopt.mixin;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderTarget;
import com.ponchisao.aeopt.config.AeOptConfig;
import com.ponchisao.aeopt.tracking.PatternPushTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class PatternProviderPushMixin {

    @Shadow
    @Final
    public PatternProviderLogicHost host;

    @Inject(method = "pushPattern", at = @At("RETURN"))
    private void aeopt$recordSuccessfulPush(IPatternDetails details,
                                            KeyCounter[] inputs,
                                            CallbackInfoReturnable<Boolean> callbackInfo) {
        if (!callbackInfo.getReturnValueZ()) {
            return;
        }
        PatternPushTracker.recordPush(details, this.host);
    }

    @Redirect(
            method = "adapterAcceptsAll",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/helpers/patternprovider/PatternProviderTarget;"
                            + "insert(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J"))
    private long aeopt$requireFullAcceptance(PatternProviderTarget target,
                                             AEKey what,
                                             long amount,
                                             Actionable mode) {
        long accepted = target.insert(what, amount, mode);
        if (!AeOptConfig.isStrictPatternPushEnabled() || accepted >= amount) {
            return accepted;
        }
        return 0L;
    }
}

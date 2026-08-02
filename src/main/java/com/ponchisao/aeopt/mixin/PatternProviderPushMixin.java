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
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class PatternProviderPushMixin {

    @Shadow
    @Final
    public PatternProviderLogicHost host;

    @Unique
    private int aeopt$machineRotation;

    @Inject(method = "pushPattern", at = @At("RETURN"))
    private void aeopt$recordSuccessfulPush(IPatternDetails details,
                                            KeyCounter[] inputs,
                                            CallbackInfoReturnable<Boolean> callbackInfo) {
        if (!callbackInfo.getReturnValueZ()) {
            return;
        }
        PatternPushTracker.recordPush(details, this.host);
        aeopt$machineRotation++;
    }

    @Redirect(
            method = "pushPattern",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/helpers/patternprovider/PatternProviderLogic;getActiveSides()Ljava/util/Set;"))
    private Set<Direction> aeopt$spreadAcrossMachines(PatternProviderLogic self) {
        Set<Direction> sides = ((PatternProviderLogicAccessor) self).aeopt$callGetActiveSides();
        if (!AeOptConfig.isMachineRoundRobinEnabled() || sides.size() < 2) {
            return sides;
        }
        return aeopt$rotatedSides(sides);
    }

    @Unique
    private Set<Direction> aeopt$rotatedSides(Set<Direction> sides) {
        List<Direction> ordered = new ArrayList<>(sides);
        int offset = Math.floorMod(aeopt$machineRotation, ordered.size());
        Set<Direction> rotated = new LinkedHashSet<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            rotated.add(ordered.get((offset + index) % ordered.size()));
        }
        return rotated;
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

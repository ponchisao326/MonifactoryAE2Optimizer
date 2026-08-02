package com.ponchisao.aeopt.diagnostics;

import appeng.helpers.patternprovider.PatternProviderLogic;
import net.minecraft.core.GlobalPos;

public record PatternProviderView(PatternProviderLogic logic,
                                  GlobalPos position,
                                  boolean busy,
                                  int pendingStackCount,
                                  boolean hasSendDirection) {

    public boolean isHoldingUndeliverableStacks() {
        return busy && !hasSendDirection;
    }

    public String describePosition() {
        return position.pos().toShortString() + " @ " + position.dimension().location();
    }
}

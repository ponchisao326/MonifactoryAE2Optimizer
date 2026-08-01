package com.ponchisao.aeopt.diagnostics;

import net.minecraft.core.GlobalPos;

public record PatternProviderView(GlobalPos position,
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

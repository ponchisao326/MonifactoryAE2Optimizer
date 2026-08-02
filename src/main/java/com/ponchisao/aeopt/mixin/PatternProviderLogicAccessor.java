package com.ponchisao.aeopt.mixin;

import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderLogic;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Set;

@Mixin(value = PatternProviderLogic.class, remap = false)
public interface PatternProviderLogicAccessor {

    @Accessor("sendList")
    List<GenericStack> aeopt$getSendList();

    @Accessor("sendDirection")
    Direction aeopt$getSendDirection();

    @Accessor("sendDirection")
    void aeopt$setSendDirection(Direction direction);

    @Invoker("getActiveSides")
    Set<Direction> aeopt$callGetActiveSides();
}

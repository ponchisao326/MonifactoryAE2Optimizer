package com.ponchisao.aeopt.compat;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AeOptCompatMixinPlugin implements IMixinConfigPlugin {

    private static final Map<String, String> REQUIRED_MOD_BY_MIXIN = Map.of(
            "com.ponchisao.aeopt.compat.RecipeHelperGuardMixin", "ae2ct",
            "com.ponchisao.aeopt.compat.CraftingPlanSummaryGuardMixin", "ae2ct");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String requiredMod = REQUIRED_MOD_BY_MIXIN.get(mixinClassName);
        return requiredMod == null || isModPresent(requiredMod);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private boolean isModPresent(String modId) {
        return LoadingModList.get().getMods().stream()
                .anyMatch(info -> info.getModId().equals(modId));
    }
}

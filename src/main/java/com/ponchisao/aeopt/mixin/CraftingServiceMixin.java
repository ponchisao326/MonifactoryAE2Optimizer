package com.ponchisao.aeopt.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.storage.IStorageService;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import com.ponchisao.aeopt.config.AeOptConfig;
import com.ponchisao.aeopt.fairness.CpuOrderRotator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

@Mixin(value = CraftingService.class, remap = false)
public abstract class CraftingServiceMixin {

    @Shadow
    @Final
    @Mutable
    private Set<CraftingCPUCluster> craftingCPUClusters;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aeopt$installOrderedCpuSet(IGrid grid,
                                            IStorageService storageService,
                                            IEnergyService energyService,
                                            CallbackInfo callbackInfo) {
        this.craftingCPUClusters = new LinkedHashSet<>(this.craftingCPUClusters);
    }

    @Inject(method = "onServerEndTick", at = @At("HEAD"))
    private void aeopt$rotateCpuTickOrder(CallbackInfo callbackInfo) {
        if (!AeOptConfig.isEnergyFairnessEnabled()) {
            return;
        }
        CpuOrderRotator.rotate(this.craftingCPUClusters);
    }
}

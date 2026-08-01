package com.ponchisao.aeopt;

import appeng.api.networking.GridServices;
import com.ponchisao.aeopt.command.AeOptCommand;
import com.ponchisao.aeopt.config.AeOptConfig;
import com.ponchisao.aeopt.grid.AeOptGridService;
import com.ponchisao.aeopt.startup.MixinVerification;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(AeOpt.MOD_ID)
public final class AeOpt {

    public static final String MOD_ID = "aeopt";

    public AeOpt() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AeOptConfig.SPEC);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            MixinVerification.verify();
            GridServices.register(AeOptGridService.class, AeOptGridService.class);
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        AeOptCommand.register(event.getDispatcher());
    }
}

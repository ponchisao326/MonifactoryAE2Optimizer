package com.ponchisao.aeopt.command;

import appeng.api.networking.IGrid;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ponchisao.aeopt.grid.AeOptGridService;
import com.ponchisao.aeopt.grid.ScanResult;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class AeOptCommand {

    private static final int REQUIRED_PERMISSION_LEVEL = 2;

    private AeOptCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("aeopt")
                .requires(source -> source.hasPermission(REQUIRED_PERMISSION_LEVEL))
                .then(Commands.literal("scan").executes(AeOptCommand::executeScan)));
    }

    private static int executeScan(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        IGrid grid = GridLocator.findGridPlayerIsLookingAt(player);
        if (grid == null) {
            sendFailure(source, "Look at a block that belongs to an ME network, then run the command again.");
            return 0;
        }

        AeOptGridService service = grid.getService(AeOptGridService.class);
        if (service == null) {
            sendFailure(source, "The diagnostics service is not attached to that network.");
            return 0;
        }

        ScanResult result = service.scanNow();
        sendReport(source, ScanReportFormatter.format(result));
        return result.findings().size();
    }

    private static void sendReport(CommandSourceStack source, List<Component> lines) {
        for (Component line : lines) {
            source.sendSuccess(() -> line, false);
        }
    }

    private static void sendFailure(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message).withStyle(ChatFormatting.RED));
    }
}

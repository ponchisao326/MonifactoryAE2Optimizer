package com.ponchisao.aeopt.command;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class GridLocator {

    private static final double LOOK_DISTANCE = 12.0D;

    private GridLocator() {
    }

    public static IGrid findGridPlayerIsLookingAt(ServerPlayer player) {
        BlockPos target = traceTargetBlock(player);
        if (target == null) {
            return null;
        }
        return resolveGridAt(player.serverLevel(), target);
    }

    private static BlockPos traceTargetBlock(ServerPlayer player) {
        HitResult hit = player.pick(LOOK_DISTANCE, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return null;
        }
        return blockHit.getBlockPos();
    }

    private static IGrid resolveGridAt(Level level, BlockPos position) {
        IInWorldGridNodeHost host = GridHelper.getNodeHost(level, position);
        if (host == null) {
            return null;
        }
        return firstConnectedGrid(host);
    }

    private static IGrid firstConnectedGrid(IInWorldGridNodeHost host) {
        IGrid grid = gridFromSide(host, null);
        if (grid != null) {
            return grid;
        }
        for (Direction side : Direction.values()) {
            grid = gridFromSide(host, side);
            if (grid != null) {
                return grid;
            }
        }
        return null;
    }

    private static IGrid gridFromSide(IInWorldGridNodeHost host, Direction side) {
        IGridNode node = host.getGridNode(side);
        return node == null ? null : node.getGrid();
    }
}

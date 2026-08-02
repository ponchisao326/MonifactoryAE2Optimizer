package com.ponchisao.aeopt.tracking;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.hooks.ticking.TickHandler;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PatternPushTracker {

    private static final int MAX_TRACKED_OUTPUTS = 4096;
    private static final String UNKNOWN_LOCATION = "unknown";

    private static final Map<AEKey, PushRecord> RECORDS = new ConcurrentHashMap<>();

    private PatternPushTracker() {
    }

    public static void recordPush(IPatternDetails details, PatternProviderLogicHost host) {
        String location = describeHost(host);
        long tick = TickHandler.instance().getCurrentTick();
        discardIfOversized();
        for (GenericStack output : details.getOutputs()) {
            RECORDS.put(output.what(), new PushRecord(location, tick));
        }
    }

    public static PushRecord lastPushOf(AEKey key) {
        return RECORDS.get(key);
    }

    public static long currentTick() {
        return TickHandler.instance().getCurrentTick();
    }

    private static void discardIfOversized() {
        if (RECORDS.size() >= MAX_TRACKED_OUTPUTS) {
            RECORDS.clear();
        }
    }

    private static String describeHost(PatternProviderLogicHost host) {
        BlockEntity blockEntity = host.getBlockEntity();
        if (blockEntity == null) {
            return UNKNOWN_LOCATION;
        }
        return blockEntity.getBlockPos().toShortString();
    }

    public record PushRecord(String location, long tick) {
    }
}

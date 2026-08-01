package com.ponchisao.aeopt.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class AeOptConfig {

    private static final boolean DEFAULT_DIAGNOSTICS_ENABLED = true;
    private static final boolean DEFAULT_ENERGY_FAIRNESS_ENABLED = false;
    private static final boolean DEFAULT_WARNING_LOGGING_ENABLED = true;
    private static final int DEFAULT_PROBE_INTERVAL_TICKS = 100;
    private static final int DEFAULT_STALLED_JOB_THRESHOLD_TICKS = 1200;
    private static final int DEFAULT_STUCK_PROVIDER_THRESHOLD_TICKS = 600;
    private static final double DEFAULT_STARVED_ENERGY_RATIO = 0.05D;
    private static final int DEFAULT_WARNING_COOLDOWN_TICKS = 6000;

    private static final Pair<Values, ForgeConfigSpec> BUILT = new ForgeConfigSpec.Builder().configure(Values::new);

    public static final ForgeConfigSpec SPEC = BUILT.getRight();

    private static final Values VALUES = BUILT.getLeft();

    private AeOptConfig() {
    }

    public static boolean isDiagnosticsEnabled() {
        return isReadable() ? VALUES.diagnosticsEnabled.get() : DEFAULT_DIAGNOSTICS_ENABLED;
    }

    public static boolean isEnergyFairnessEnabled() {
        return isReadable() ? VALUES.energyFairnessEnabled.get() : DEFAULT_ENERGY_FAIRNESS_ENABLED;
    }

    public static boolean isWarningLoggingEnabled() {
        return isReadable() ? VALUES.warningLoggingEnabled.get() : DEFAULT_WARNING_LOGGING_ENABLED;
    }

    public static int probeIntervalTicks() {
        return isReadable() ? VALUES.probeIntervalTicks.get() : DEFAULT_PROBE_INTERVAL_TICKS;
    }

    public static int stalledJobThresholdTicks() {
        return isReadable() ? VALUES.stalledJobThresholdTicks.get() : DEFAULT_STALLED_JOB_THRESHOLD_TICKS;
    }

    public static int stuckProviderThresholdTicks() {
        return isReadable() ? VALUES.stuckProviderThresholdTicks.get() : DEFAULT_STUCK_PROVIDER_THRESHOLD_TICKS;
    }

    public static double starvedEnergyRatio() {
        return isReadable() ? VALUES.starvedEnergyRatio.get() : DEFAULT_STARVED_ENERGY_RATIO;
    }

    public static int warningCooldownTicks() {
        return isReadable() ? VALUES.warningCooldownTicks.get() : DEFAULT_WARNING_COOLDOWN_TICKS;
    }

    private static boolean isReadable() {
        return SPEC.isLoaded();
    }

    private static final class Values {

        private final ForgeConfigSpec.BooleanValue diagnosticsEnabled;
        private final ForgeConfigSpec.BooleanValue energyFairnessEnabled;
        private final ForgeConfigSpec.BooleanValue warningLoggingEnabled;
        private final ForgeConfigSpec.IntValue probeIntervalTicks;
        private final ForgeConfigSpec.IntValue stalledJobThresholdTicks;
        private final ForgeConfigSpec.IntValue stuckProviderThresholdTicks;
        private final ForgeConfigSpec.DoubleValue starvedEnergyRatio;
        private final ForgeConfigSpec.IntValue warningCooldownTicks;

        private Values(ForgeConfigSpec.Builder builder) {
            builder.push("diagnostics");
            diagnosticsEnabled = builder.define("enabled", DEFAULT_DIAGNOSTICS_ENABLED);
            warningLoggingEnabled = builder.define("logWarnings", DEFAULT_WARNING_LOGGING_ENABLED);
            probeIntervalTicks = builder.defineInRange("probeIntervalTicks",
                    DEFAULT_PROBE_INTERVAL_TICKS, 20, 12000);
            stalledJobThresholdTicks = builder.defineInRange("stalledJobThresholdTicks",
                    DEFAULT_STALLED_JOB_THRESHOLD_TICKS, 100, 432000);
            stuckProviderThresholdTicks = builder.defineInRange("stuckProviderThresholdTicks",
                    DEFAULT_STUCK_PROVIDER_THRESHOLD_TICKS, 100, 432000);
            starvedEnergyRatio = builder.defineInRange("starvedEnergyRatio",
                    DEFAULT_STARVED_ENERGY_RATIO, 0.0D, 1.0D);
            warningCooldownTicks = builder.defineInRange("warningCooldownTicks",
                    DEFAULT_WARNING_COOLDOWN_TICKS, 100, 432000);
            builder.pop();

            builder.push("fairness");
            energyFairnessEnabled = builder.define("rotateCpuTickOrder", DEFAULT_ENERGY_FAIRNESS_ENABLED);
            builder.pop();
        }
    }
}

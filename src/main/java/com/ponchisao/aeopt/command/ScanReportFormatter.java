package com.ponchisao.aeopt.command;

import com.ponchisao.aeopt.diagnostics.Finding;
import com.ponchisao.aeopt.diagnostics.GridTickSample;
import com.ponchisao.aeopt.grid.ScanResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ScanReportFormatter {

    private ScanReportFormatter() {
    }

    public static List<Component> format(ScanResult result) {
        List<Component> lines = new ArrayList<>();
        lines.add(header());
        lines.add(summary(result.sample()));
        lines.addAll(findings(result));
        return lines;
    }

    private static Component header() {
        return Component.literal("AE2 Optimization scan").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
    }

    private static Component summary(GridTickSample sample) {
        String text = String.format(
                "CPUs: %d with a job / %d total | Providers busy: %d / %d | AE buffer: %.1f%% | Starved ticks: %.0f%%",
                sample.countCpusWithJob(),
                sample.cpus().size(),
                sample.countBusyProviders(),
                sample.providers().size(),
                sample.storedPowerRatio() * 100.0D,
                sample.starvedTickRatio() * 100.0D);
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    private static List<Component> findings(ScanResult result) {
        if (result.isHealthy()) {
            return List.of(Component.literal("No problems detected.").withStyle(ChatFormatting.GREEN));
        }
        List<Component> lines = new ArrayList<>();
        for (Finding finding : result.findings()) {
            lines.add(Component.literal("- " + finding.message()).withStyle(finding.severity().color()));
        }
        return lines;
    }
}

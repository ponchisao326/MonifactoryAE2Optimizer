package com.ponchisao.aeopt.diagnostics;

import net.minecraft.ChatFormatting;

public enum Severity {

    INFO(ChatFormatting.GRAY),
    WARNING(ChatFormatting.YELLOW),
    CRITICAL(ChatFormatting.RED);

    private final ChatFormatting color;

    Severity(ChatFormatting color) {
        this.color = color;
    }

    public ChatFormatting color() {
        return color;
    }
}

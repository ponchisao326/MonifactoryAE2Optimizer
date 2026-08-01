package com.ponchisao.aeopt.diagnostics;

public record Finding(Severity severity, String code, String message) {

    public static Finding critical(String code, String message) {
        return new Finding(Severity.CRITICAL, code, message);
    }

    public static Finding warning(String code, String message) {
        return new Finding(Severity.WARNING, code, message);
    }

    public static Finding info(String code, String message) {
        return new Finding(Severity.INFO, code, message);
    }
}

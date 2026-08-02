package com.ponchisao.aeopt.diagnostics;

import com.ponchisao.aeopt.diagnostics.probe.EnergyStarvationProbe;
import com.ponchisao.aeopt.diagnostics.probe.StalledJobProbe;
import com.ponchisao.aeopt.diagnostics.probe.StuckSendListProbe;

import java.util.ArrayList;
import java.util.List;

public final class DiagnosticsRegistry {

    private static final List<DiagnosticProbe> PROBES = List.of(
            new StuckSendListProbe(),
            new EnergyStarvationProbe(),
            new StalledJobProbe());

    private DiagnosticsRegistry() {
    }

    public static List<Finding> runAll(DiagnosticContext context) {
        List<Finding> findings = new ArrayList<>();
        for (DiagnosticProbe probe : PROBES) {
            findings.addAll(probe.inspect(context));
        }
        return List.copyOf(findings);
    }
}

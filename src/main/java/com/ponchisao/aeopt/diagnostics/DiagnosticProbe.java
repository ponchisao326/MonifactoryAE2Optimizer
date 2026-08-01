package com.ponchisao.aeopt.diagnostics;

import java.util.List;

public interface DiagnosticProbe {

    String id();

    List<Finding> inspect(DiagnosticContext context);
}

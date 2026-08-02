package com.ponchisao.aeopt.diagnostics;

public record BlockedPattern(String output,
                             long remainingRuns,
                             int providerCount,
                             int busyProviderCount,
                             String providerLocations,
                             String missingIngredient) {

    public boolean hasNoProvider() {
        return providerCount == 0;
    }

    public boolean areAllProvidersStuck() {
        return providerCount > 0 && busyProviderCount == providerCount;
    }

    public boolean isMissingIngredient() {
        return missingIngredient != null;
    }

    public String describe() {
        return output + " (x" + remainingRuns + " left): " + describeReason();
    }

    private String describeReason() {
        if (hasNoProvider()) {
            return "no pattern provider offers this recipe";
        }
        if (areAllProvidersStuck()) {
            return "ROOT CAUSE - every provider is stuck with undelivered stacks at " + providerLocations
                    + ". Clear the machine next to it and this chain resumes";
        }
        if (isMissingIngredient()) {
            return "CPU is missing " + missingIngredient
                    + ", produced upstream in this same job, so fixing the root cause below resolves it";
        }
        return providerCount + " provider(s) at " + providerLocations
                + " have the ingredients but refused the push (blocking mode, or a locked crafting provider)";
    }
}

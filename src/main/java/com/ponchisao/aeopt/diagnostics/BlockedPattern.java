package com.ponchisao.aeopt.diagnostics;

public record BlockedPattern(String output,
                             long remainingRuns,
                             int providerCount,
                             int busyProviderCount,
                             String providerLocations,
                             String missingIngredient,
                             boolean missingIngredientProducedByJob) {

    public boolean hasNoProvider() {
        return providerCount == 0;
    }

    public boolean areAllProvidersStuck() {
        return providerCount > 0 && busyProviderCount == providerCount;
    }

    public boolean isMissingIngredient() {
        return missingIngredient != null;
    }

    public boolean isUnrecoverable() {
        return isMissingIngredient() && !missingIngredientProducedByJob;
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
        if (isUnrecoverable()) {
            return "DEAD JOB - missing " + missingIngredient
                    + ", and no remaining step of this job produces it. It was lost after the job started, "
                    + "so the job can never finish. Cancel and re-request";
        }
        if (isMissingIngredient()) {
            return "waiting on " + missingIngredient + ", which an earlier step of this job still has to make";
        }
        return providerCount + " provider(s) at " + providerLocations
                + " have the ingredients but refused the push (blocking mode, or a locked crafting provider)";
    }
}

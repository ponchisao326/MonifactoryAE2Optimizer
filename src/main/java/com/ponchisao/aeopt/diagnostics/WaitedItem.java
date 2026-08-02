package com.ponchisao.aeopt.diagnostics;

public record WaitedItem(String name,
                         long amount,
                         long networkStock,
                         String lastPushLocation,
                         long minutesSinceLastPush) {

    public boolean isAlreadyInNetwork() {
        return networkStock >= amount;
    }

    public boolean hasKnownPushTarget() {
        return lastPushLocation != null;
    }

    public String describe() {
        String base = amount + "x " + name + " (network holds " + networkStock + ")";
        if (!hasKnownPushTarget()) {
            return base;
        }
        return base + " last pushed to the provider at " + lastPushLocation
                + " " + minutesSinceLastPush + " min ago";
    }
}

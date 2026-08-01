package com.ponchisao.aeopt.tracking;

public final class StarvationCounter {

    private long observedTicks;
    private long starvedTicks;

    public void observe(boolean starved) {
        observedTicks++;
        if (starved) {
            starvedTicks++;
        }
    }

    public double starvedRatio() {
        if (observedTicks == 0L) {
            return 0.0D;
        }
        return (double) starvedTicks / (double) observedTicks;
    }

    public void reset() {
        observedTicks = 0L;
        starvedTicks = 0L;
    }
}

package com.ponchisao.aeopt.diagnostics;

public record WaitedItem(String name, long amount, long networkStock) {

    public boolean isAlreadyInNetwork() {
        return networkStock >= amount;
    }

    public String describe() {
        return amount + "x " + name + " (network holds " + networkStock + ")";
    }
}

package com.ponchisao.aeopt.diagnostics;

public record WaitingItem(String name, long amount) {

    public String describe() {
        return amount + "x " + name;
    }
}

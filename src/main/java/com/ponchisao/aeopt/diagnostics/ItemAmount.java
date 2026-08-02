package com.ponchisao.aeopt.diagnostics;

public record ItemAmount(String name, long amount) {

    public String describe() {
        return amount + "x " + name;
    }
}

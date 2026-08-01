package com.ponchisao.aeopt.fairness;

import appeng.me.cluster.implementations.CraftingCPUCluster;

import java.util.Iterator;
import java.util.Set;

public final class CpuOrderRotator {

    private CpuOrderRotator() {
    }

    public static void rotate(Set<CraftingCPUCluster> clusters) {
        if (!canRotate(clusters)) {
            return;
        }
        Iterator<CraftingCPUCluster> iterator = clusters.iterator();
        CraftingCPUCluster head = iterator.next();
        iterator.remove();
        clusters.add(head);
    }

    private static boolean canRotate(Set<CraftingCPUCluster> clusters) {
        return clusters.size() > 1;
    }
}

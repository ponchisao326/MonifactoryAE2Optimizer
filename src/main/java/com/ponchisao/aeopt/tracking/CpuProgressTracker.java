package com.ponchisao.aeopt.tracking;

import appeng.api.networking.crafting.ICraftingCPU;
import com.ponchisao.aeopt.diagnostics.CraftingCpuView;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CpuProgressTracker {

    private final Map<ICraftingCPU, ProgressMark> marks = new ConcurrentHashMap<>();

    public void observe(CraftingCpuView view, long tick) {
        if (!view.hasJob()) {
            marks.remove(view.cpu());
            return;
        }
        ProgressMark previous = marks.get(view.cpu());
        if (shouldResetMark(previous, view)) {
            marks.put(view.cpu(), new ProgressMark(view.waitingForTotal(), tick));
        }
    }

    public long ticksSinceProgress(ICraftingCPU cpu, long tick) {
        ProgressMark mark = marks.get(cpu);
        if (mark == null) {
            return 0L;
        }
        return tick - mark.tick();
    }

    public void retainOnly(Set<ICraftingCPU> presentCpus) {
        marks.keySet().retainAll(presentCpus);
    }

    private boolean shouldResetMark(ProgressMark previous, CraftingCpuView view) {
        return previous == null || previous.waitingForTotal() != view.waitingForTotal();
    }

    private record ProgressMark(long waitingForTotal, long tick) {
    }
}

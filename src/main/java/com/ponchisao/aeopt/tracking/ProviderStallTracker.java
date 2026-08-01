package com.ponchisao.aeopt.tracking;

import com.ponchisao.aeopt.diagnostics.PatternProviderView;
import net.minecraft.core.GlobalPos;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ProviderStallTracker {

    private final Map<GlobalPos, StallMark> marks = new ConcurrentHashMap<>();

    public void observe(PatternProviderView view, long tick) {
        if (!view.busy()) {
            marks.remove(view.position());
            return;
        }
        StallMark previous = marks.get(view.position());
        if (shouldResetMark(previous, view)) {
            marks.put(view.position(), new StallMark(view.pendingStackCount(), tick));
        }
    }

    public long ticksStuck(GlobalPos position, long tick) {
        StallMark mark = marks.get(position);
        if (mark == null) {
            return 0L;
        }
        return tick - mark.tick();
    }

    public void retainOnly(Set<GlobalPos> presentProviders) {
        marks.keySet().retainAll(presentProviders);
    }

    private boolean shouldResetMark(StallMark previous, PatternProviderView view) {
        return previous == null || previous.pendingStackCount() != view.pendingStackCount();
    }

    private record StallMark(int pendingStackCount, long tick) {
    }
}

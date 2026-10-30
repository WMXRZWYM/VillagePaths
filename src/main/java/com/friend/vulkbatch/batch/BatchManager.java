package com.friend.vulkbatch.batch;

import com.friend.vulkbatch.config.Config;
import java.util.concurrent.atomic.AtomicInteger;

public final class BatchManager {
    private static final ThreadLocal<Boolean> DEFERRED = ThreadLocal.withInitial(() -> false);

    public static final AtomicInteger INTERCEPTED_BUILDS = new AtomicInteger(0);
    public static final AtomicInteger REUSED_ALLOCATIONS = new AtomicInteger(0);

    public static boolean shouldCoalesce() {
        return Config.optimizeBufferReuse && !DEFERRED.get();
    }

    public static void defer() {
        DEFERRED.set(true);
        INTERCEPTED_BUILDS.incrementAndGet();
    }

    public static void release() {
        DEFERRED.set(false);
        REUSED_ALLOCATIONS.incrementAndGet();
    }

    public static String getAndResetStats() {
        int builds = INTERCEPTED_BUILDS.getAndSet(0);
        int reused = REUSED_ALLOCATIONS.getAndSet(0);
        if (builds == 0 && reused == 0) return null;
        return "Past 5s: Intercepted " + builds + " builds, Optimized " + reused + " allocations.";
    }
}
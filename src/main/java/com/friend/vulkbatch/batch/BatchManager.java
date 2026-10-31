package com.friend.vulkbatch.batch;

import com.friend.vulkbatch.config.Config;
import net.minecraft.core.SectionPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class BatchManager {
    private static final ThreadLocal<Boolean> DEFERRED = ThreadLocal.withInitial(() -> false);

    public static final AtomicInteger INTERCEPTED_BUILDS = new AtomicInteger(0);
    public static final AtomicInteger REUSED_ALLOCATIONS = new AtomicInteger(0);
    public static final AtomicInteger SKIPPED_CHUNKS = new AtomicInteger(0);

    private static final Map<Long, Long> RECENT_CHUNKS = new ConcurrentHashMap<>();

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

    public static boolean shouldSkipChunkRebuild(SectionPos pos, long currentTick) {
        if (!Config.optimizeChunkRebuild) return false;

        long packedPos = pos.asLong();
        Long lastRebuild = RECENT_CHUNKS.get(packedPos);

        if (lastRebuild != null && (currentTick - lastRebuild) < 5) {
            SKIPPED_CHUNKS.incrementAndGet();
            return true;
        }

        RECENT_CHUNKS.put(packedPos, currentTick);
        return false;
    }

    public static String getAndResetStats() {
        int builds = INTERCEPTED_BUILDS.getAndSet(0);
        int reused = REUSED_ALLOCATIONS.getAndSet(0);
        int skipped = SKIPPED_CHUNKS.getAndSet(0);
        if (builds == 0 && reused == 0 && skipped == 0) return null;
        return "Past 5s: Intercepted " + builds + " builds, Optimized " + reused + " allocations, Skipped " + skipped + " chunk rebuilds.";
    }
}
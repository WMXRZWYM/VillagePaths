package com.friend.vulkbatch;

import com.friend.vulkbatch.batch.VulkanHints;
import com.friend.vulkbatch.config.Config;
import com.friend.vulkbatch.batch.BatchManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class VulkBatch implements ClientModInitializer {
    public static final String MOD_ID = "vulkbatch";
    private int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        Config.load();
        boolean vulkan = VulkanHints.isVulkanBackend();
        System.out.printf("[VulkBatch] Loaded. Backend=%s, TargetBatchSize=%d%n",
                vulkan ? "Vulkan" : "OpenGL", VulkanHints.preferredBatchSize());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.level != null) {
                tickCounter++;
                if (tickCounter >= 100) {
                    tickCounter = 0;
                    String stats = BatchManager.getAndResetStats();
                    if (stats != null) {
                        System.out.println("[VulkBatch Panel] " + stats);
                    }
                }
            }
        });
    }
}
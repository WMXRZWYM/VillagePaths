package com.friend.vulkbatch.batch;

import com.friend.vulkbatch.config.Config;

public final class VulkanHints {
    public static boolean isVulkanBackend() {
        if (Config.forceVulkanHint) return true;
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("getInstance").invoke(null);
            Object window = mcClass.getMethod("getWindow").invoke(mc);
            Object renderer = window.getClass().getMethod("getRenderer").invoke(window);
            Object device = renderer.getClass().getMethod("getDevice").invoke(renderer);
            Object info = device.getClass().getMethod("getDeviceInfo").invoke(device);
            Object name = info.getClass().getMethod("backendName").invoke(info);
            return name != null && name.toString().toLowerCase().contains("vulkan");
        } catch (Throwable t) {
            return false;
        }
    }

    public static int preferredBatchSize() {
        return isVulkanBackend() ? 1024 : 512;
    }
}
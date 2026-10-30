package com.friend.vulkbatch.config;

public final class Config {
    public static boolean optimizeBufferReuse = true;
    public static boolean forceVulkanHint = false;

    public static void load() {
        optimizeBufferReuse = Boolean.parseBoolean(
                System.getProperty("vulkbatch.bufferReuse", "true"));
        forceVulkanHint = Boolean.parseBoolean(
                System.getProperty("vulkbatch.forceVulkan", "false"));
    }
}
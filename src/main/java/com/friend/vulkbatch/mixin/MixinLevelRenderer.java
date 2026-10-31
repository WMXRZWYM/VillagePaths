package com.friend.vulkbatch.mixin;

import com.friend.vulkbatch.batch.BatchManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class MixinLevelRenderer {

    @Shadow private volatile long sectionNode;

    // 精确拦截 compileAsync，参数必须是 RenderSectionRegion
    @Inject(method = "compileAsync(Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;)V", at = @At("HEAD"), cancellable = true)
    private void vulkbatch$skipRedundantRebuild(RenderSectionRegion region, CallbackInfo ci) {
        if (Minecraft.getInstance().level == null) return;

        SectionPos pos = SectionPos.of(this.sectionNode);
        long currentTick = Minecraft.getInstance().level.getGameTime();

        // 如果 5 帧内重复请求重建，直接掐断
        if (BatchManager.shouldSkipChunkRebuild(pos, currentTick)) {
            ci.cancel();
        }
    }
}
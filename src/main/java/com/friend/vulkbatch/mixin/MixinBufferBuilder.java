package com.friend.vulkbatch.mixin;

import com.friend.vulkbatch.batch.BatchManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder {

    @Inject(method = "build", at = @At("HEAD"), cancellable = true)
    private void vulkbatch$interceptBuild(CallbackInfoReturnable<MeshData> cir) {
        if (BatchManager.shouldCoalesce()) {
            BatchManager.defer();
        }
    }

    @Inject(method = "build", at = @At("RETURN"))
    private void vulkbatch$afterBuild(CallbackInfoReturnable<MeshData> cir) {
        BatchManager.release();
    }
}
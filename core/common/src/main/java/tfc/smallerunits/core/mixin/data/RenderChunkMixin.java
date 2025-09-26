package tfc.smallerunits.core.mixin.data;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.client.access.tracking.SUCompiledChunkAttachments;

@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public abstract class RenderChunkMixin {
    @Shadow public abstract ChunkRenderDispatcher.CompiledChunk getCompiledChunk();

    @Inject(at = @At("TAIL"), method = "releaseBuffers")
    public void postRelease(CallbackInfo ci) {
        ChunkRenderDispatcher.CompiledChunk cc = getCompiledChunk();
        ((SUCompiledChunkAttachments) cc).SU$getChunkRender().dirty();
    }
}

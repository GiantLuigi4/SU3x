package tfc.smallerunits.core.mixin.data.access;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import tfc.smallerunits.core.client.access.tracking.CompiledChunkAccessor;

import java.lang.ref.WeakReference;

@Mixin(ChunkRenderDispatcher.CompiledChunk.class)
public class CompiledChunkMixin implements CompiledChunkAccessor {
	@Unique
	WeakReference<ChunkRenderDispatcher.RenderChunk> mixinIShouldNotHaveToDoThis;
	
	@Override
	public void SU$setRenderChunk(ChunkRenderDispatcher.RenderChunk chunk) {
		mixinIShouldNotHaveToDoThis = new WeakReference<>(chunk);
	}
	
	@Override
	public ChunkRenderDispatcher.RenderChunk SU$getRenderChunk() {
		return mixinIShouldNotHaveToDoThis.get();
	}
}

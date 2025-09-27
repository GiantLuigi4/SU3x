package tfc.smallerunits.core.utils;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

import static tfc.smallerunits.plat.internal.ToolProvider.currentRenderChunk;

// NOT UNUSED
@SuppressWarnings("unused")
public class RenderChunkSetter {
	public static ChunkRenderDispatcher.RenderChunk updateRenderChunk(ChunkRenderDispatcher.RenderChunk chunk) {
		currentRenderChunk.set(chunk);
		return chunk;
	}
}

package tfc.smallerunits.sodium.render;

import net.minecraft.world.level.ChunkPos;
import tfc.smallerunits.core.client.access.tracking.SUCompiledChunkAttachments;

import java.util.HashMap;

public interface SodiumGridAttachments {
	HashMap<ChunkPos, SUCompiledChunkAttachments> getRenderChunks();
	HashMap<ChunkPos, SUCompiledChunkAttachments> renderChunksWithUnits();
}

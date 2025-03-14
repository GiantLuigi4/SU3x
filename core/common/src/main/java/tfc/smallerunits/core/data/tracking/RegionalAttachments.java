package tfc.smallerunits.core.data.tracking;

import net.minecraft.world.level.ChunkPos;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.data.storage.RegionPos;

import java.util.Map;
import java.util.function.BiConsumer;

public interface RegionalAttachments {
	Region SU$getRegion(RegionPos pos);
	
	void SU$findChunk(int y, ChunkPos flag, BiConsumer<RegionPos, Region> regionHandler);
	
	Map<RegionPos, Region> SU$getRegionMap();
}

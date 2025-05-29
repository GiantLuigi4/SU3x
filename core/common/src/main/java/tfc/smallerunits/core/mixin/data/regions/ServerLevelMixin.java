package tfc.smallerunits.core.mixin.data.regions;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.data.storage.RegionPos;
import tfc.smallerunits.core.data.tracking.RegionClosable;
import tfc.smallerunits.core.data.tracking.RegionalAttachments;

import java.util.Map;
import java.util.function.BiConsumer;

@Mixin(ServerLevel.class)
public class ServerLevelMixin implements RegionalAttachments, RegionClosable {
	@Shadow
	public ServerChunkCache chunkSource;
	
	@Override
	public Region SU$getRegion(RegionPos pos) {
		return ((RegionalAttachments) chunkSource.chunkMap).SU$getRegion(pos);
	}
	
	@Override
	public void SU$findChunk(int y, ChunkPos flag, BiConsumer<RegionPos, Region> regionHandler) {
		((RegionalAttachments) chunkSource.chunkMap).SU$findChunk(y, flag, regionHandler);
	}
	
	@Override
	public Map<RegionPos, Region> SU$getRegionMap() {
		return ((RegionalAttachments) chunkSource.chunkMap).SU$getRegionMap();
	}

	@Override
	public void closeSURegions() {
		((RegionClosable) chunkSource.chunkMap).closeSURegions();;
	}
}

package tfc.smallerunits.core.mixin.data.regions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.data.storage.RegionPos;
import tfc.smallerunits.core.data.tracking.RegionClosable;
import tfc.smallerunits.core.data.tracking.RegionalAttachments;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@Mixin(ClientLevel.class)
public class ClientLevelMixin implements RegionalAttachments, RegionClosable {
	@Unique
	private final HashMap<RegionPos, Region> regionMap = new HashMap<>();
	
	@Inject(at = @At("HEAD"), method = "unload")
	public void preUnloadChunk(LevelChunk pChunk, CallbackInfo ci) {
		ChunkPos pos = pChunk.getPos();
		int min = pChunk.getMinBuildHeight();
		int max = pChunk.getMaxBuildHeight();
		for (int y = min; y < max; y += 16)
			findChunk(y, pos, (rp, r) -> {
				if (r.subtractRef(rp) <= 0) {
					Region region = regionMap.remove(rp);
					if (region != null) region.close();
				}
			});
	}
	
	@Inject(at = @At("HEAD"), method = "onChunkLoaded")
	public void onLoadChunk(ChunkPos pChunkPos, CallbackInfo ci) {
		int min = ((Level) (Object) this).getMinBuildHeight();
		int max = ((Level) (Object) this).getMaxBuildHeight();
		for (int y = min; y < max; y += 16)
			findChunk(y, pChunkPos, (rp, r) -> {
				r.addRef(rp);
			});
	}
	
	@Override
	public Region SU$getRegion(RegionPos pos) {
		return regionMap.getOrDefault(pos, null);
	}
	
	@Override
	public Map<RegionPos, Region> SU$getRegionMap() {
		return regionMap;
	}
	
	@Override
	public void SU$findChunk(int y, ChunkPos flag, BiConsumer<RegionPos, Region> regionHandler) {
		findChunk(y, flag, regionHandler);
	}
	
	@Unique
	private void findChunk(int y, ChunkPos flag, BiConsumer<RegionPos, Region> regionHandler) {
		RegionPos pos = new RegionPos(flag.getMinBlockX() >> 9, y >> 9, flag.getMinBlockZ() >> 9);
		Region r = regionMap.getOrDefault(pos, null);
		if (r == null) regionMap.put(pos, r = new Region(pos));
		regionHandler.accept(pos, r);
	}

	@Unique
	boolean SU$closed = false;

	@Override
	public void closeSURegions() {
		if (!SU$closed) {
			SU$closed = true;
			for (Region value : regionMap.values()) {
				value.close();
			}
			regionMap.clear();
		}
	}
}

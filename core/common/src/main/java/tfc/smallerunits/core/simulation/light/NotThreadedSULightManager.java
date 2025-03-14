package tfc.smallerunits.core.simulation.light;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LightChunkGetter;

import java.util.concurrent.CompletableFuture;

// TODO: figure out why I can't get mojang's one to work
public class NotThreadedSULightManager extends ThreadedLevelLightEngine {
	public NotThreadedSULightManager(LightChunkGetter level, ChunkMap map, boolean sky) {
		super(level, map, sky, null, null);
	}
	
	@Override
	public void updateSectionStatus(BlockPos p_75835_, boolean pIsEmpty) {
		this.updateSectionStatus(SectionPos.of(p_75835_), pIsEmpty);
	}
	
	public void updateSectionStatus(SectionPos pPos, boolean pIsEmpty) {
		if (this.blockEngine != null) {
			this.blockEngine.updateSectionStatus(pPos, pIsEmpty);
		}
		
		if (this.skyEngine != null) {
			this.skyEngine.updateSectionStatus(pPos, pIsEmpty);
		}
	}
	
	@Override
	public boolean hasLightWork() {
		if (this.skyEngine != null && this.skyEngine.hasLightWork()) {
			return true;
		} else {
			return this.blockEngine != null && this.blockEngine.hasLightWork();
		}
	}
	
	@Override
	public int runLightUpdates() {
		boolean block = blockEngine != null && blockEngine.hasLightWork();
		boolean sky = skyEngine != null && skyEngine.hasLightWork();
		if (block && sky) {
			int l = this.skyEngine.runLightUpdates();
			return l > 0 ? this.blockEngine.runLightUpdates() : l;
		} else if (block) {
			return this.blockEngine.runLightUpdates();
		} else if (sky) {
			return this.skyEngine.runLightUpdates();
		}
		return 2000;
	}
	
	@Override
	public void checkBlock(BlockPos pos) {
		if (this.blockEngine != null) this.blockEngine.checkBlock(pos);
		if (this.skyEngine != null) this.skyEngine.checkBlock(pos);
	}
	
	@Override
	public void tryScheduleUpdate() {
	}

	@Override
	public CompletableFuture<ChunkAccess> lightChunk(ChunkAccess pChunk, boolean p_9355_) {
		return CompletableFuture.completedFuture(pChunk);
	}
}

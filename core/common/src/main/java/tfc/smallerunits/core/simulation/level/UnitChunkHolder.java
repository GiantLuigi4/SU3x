package tfc.smallerunits.core.simulation.level;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jetbrains.annotations.Nullable;
import tfc.smallerunits.core.data.access.ChunkHolderAccessor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UnitChunkHolder extends ChunkHolder {
	LevelChunk chunk;
	int yPos;
	
	public UnitChunkHolder(ChunkPos p_142986_, int p_142987_, LevelHeightAccessor p_142988_, LevelLightEngine p_142989_, LevelChangeListener p_142990_, PlayerProvider p_142991_, LevelChunk chunk, int yPos) {
		super(p_142986_, p_142987_, p_142988_, p_142989_, p_142990_, p_142991_);
		this.chunk = chunk;
		this.yPos = yPos;
	}
	
	@Override
	public CompletableFuture<Either<ChunkAccess, ChunkLoadingFailure>> getFutureIfPresentUnchecked(ChunkStatus p_140048_) {
		return getFutureIfPresent(null);
	}
	
	@Override
	public CompletableFuture<Either<ChunkAccess, ChunkLoadingFailure>> getFutureIfPresent(ChunkStatus p_140081_) {
		return new CompletableFuture<>() {
			@Override
			public boolean isDone() {
				return true;
			}
			
			@Override
			public Either<ChunkAccess, ChunkLoadingFailure> get() {
				return Either.left(chunk);
			}
		};
	}
	
	@Override
	public CompletableFuture<Either<LevelChunk, ChunkLoadingFailure>> getTickingChunkFuture() {
		return getFullChunkFuture();
	}
	
	@Override
	public CompletableFuture<Either<LevelChunk, ChunkLoadingFailure>> getEntityTickingChunkFuture() {
		return getFullChunkFuture();
	}
	
	@Override
	public CompletableFuture<Either<LevelChunk, ChunkLoadingFailure>> getFullChunkFuture() {
		return new CompletableFuture<>() {
			@Override
			public boolean isDone() {
				return true;
			}
			
			@Override
			public Either<LevelChunk, ChunkLoadingFailure> get() {
				return Either.left(chunk);
			}
		};
	}
	
	@Nullable
	@Override
	public LevelChunk getTickingChunk() {
		return chunk;
	}
	
	@Nullable
	@Override
	public LevelChunk getFullChunk() {
		return chunk;
	}
	
	public void setBlockDirty(BlockPos pos) {
		blockChanged(pos);
	}
	
	// TODO: modernize
//	@Override
//	public void broadcastChanges(LevelChunk pChunk) {
//		if (this.hasChangedSections) {
//			List list1 = this.playerProvider.getPlayers(this.pos, false);
//
//			for (int j = 0; j < this.changedBlocksPerSection.length; ++j) {
//				ShortSet shortset = this.changedBlocksPerSection[j];
//				if (shortset != null) {
//					this.changedBlocksPerSection[j] = null;
//					if (!list1.isEmpty()) {
//						int i = this.levelHeightAccessor.getSectionYFromSectionIndex(j);
//						SectionPos sectionpos = SectionPos.of(pChunk.getPos(), i);
//						if (shortset.size() == 1) {
//							BlockPos blockpos = sectionpos.relativeToBlockPos(shortset.iterator().nextShort());
//							BlockState blockstate = level.getBlockState(blockpos);
//							((ChunkHolderAccessor) this).SU$call_broadcast(list1, new ClientboundBlockUpdatePacket(blockpos, blockstate));
//							((ChunkHolderAccessor) this).SU$call_broadcastBlockEntityIfNeeded(list1, level, blockpos, blockstate);
//						} else {
//							LevelChunkSection levelchunksection = pChunk.getSection(j);
//							ClientboundSectionBlocksUpdatePacket clientboundsectionblocksupdatepacket = new ClientboundSectionBlocksUpdatePacket(
//									sectionpos, shortset, levelchunksection
//							);
//							((ChunkHolderAccessor) this).SU$call_broadcast(list1, clientboundsectionblocksupdatepacket);
//							clientboundsectionblocksupdatepacket.runUpdates((var3x, var4x) -> ((ChunkHolderAccessor) this).SU$call_broadcastBlockEntityIfNeeded(list1, level, var3x, var4x));
//						}
//					}
//				}
//			}
//
//			this.hasChangedSections = false;
//		}
//	}
	
	@Override
	public void broadcastChanges(LevelChunk pChunk) {
		if (this.hasChangedSections) {
			Level level = pChunk.getLevel();
			
			List<ServerPlayer> players = ((ServerChunkCache) pChunk.getLevel().getChunkSource()).chunkMap.getPlayers(chunk.getPos(), false);
			
			for (int l = 0; l < this.changedBlocksPerSection.length; ++l) {
				ShortSet shortset = this.changedBlocksPerSection[l];
				if (shortset != null) {
					int k = this.levelHeightAccessor.getSectionYFromSectionIndex(yPos);
					SectionPos sectionpos = SectionPos.of(pChunk.getPos(), k);
					if (shortset.size() == 1) {
						BlockPos blockpos = sectionpos.relativeToBlockPos(shortset.iterator().nextShort());
						BlockState blockstate = level.getBlockState(blockpos);
						((ChunkHolderAccessor) this).SU$call_broadcast(players, new ClientboundBlockUpdatePacket(blockpos, blockstate));
						((ChunkHolderAccessor) this).SU$call_broadcastBlockEntityIfNeeded(players, level, blockpos, blockstate);
					} else {
						LevelChunkSection levelchunksection = pChunk.getSection(yPos);
						ClientboundSectionBlocksUpdatePacket clientboundsectionblocksupdatepacket = new ClientboundSectionBlocksUpdatePacket(
								sectionpos, shortset, levelchunksection
						);
						((ChunkHolderAccessor) this).SU$call_broadcast(players, clientboundsectionblocksupdatepacket);
						clientboundsectionblocksupdatepacket.runUpdates((var3x, var4x) -> ((ChunkHolderAccessor) this).SU$call_broadcastBlockEntityIfNeeded(players, level, var3x, var4x));
					}
					
					this.changedBlocksPerSection[l] = null;
				}
			}
			
			this.hasChangedSections = false;
		}
	}
}

package tfc.smallerunits.forge.level.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.Nullable;
import tfc.smallerunits.core.simulation.level.server.AbstractTickerServerLevel;
import tfc.smallerunits.plat.internal.ToolProvider;
import tfc.smallerunits.storage.IRegion;

import java.util.List;

public class ForgeTickerServerLevel extends AbstractTickerServerLevel {
	public ForgeTickerServerLevel(MinecraftServer server, ServerLevelData data, ResourceKey<Level> p_8575_, DimensionType dimType, ChunkProgressListener progressListener, ChunkGenerator generator, boolean p_8579_, long p_8580_, List<CustomSpawner> spawners, boolean p_8582_, Level parent, int upb, IRegion region) {
		super(server, data, p_8575_, dimType, progressListener, generator, p_8579_, p_8580_, spawners, p_8582_, parent, upb, region);
	}
	
	
	// set block state pretty much completely changes between platforms
	// TODO: try to optimize or shrink this?
	@Override
	public boolean setBlock(BlockPos pPos, BlockState pState, int pFlags, int pRecursionLeft) {
		if (this.isOutsideBuildHeight(pPos)) {
			return false;
		} else if (!this.isClientSide && this.isDebug()) {
			return false;
		} else {
			LevelChunk levelchunk = this.getChunkAt(pPos);
			
			BlockPos actualPos = pPos;
			pPos = new BlockPos(pPos.getX() & 15, pPos.getY(), pPos.getZ() & 15);
			net.minecraftforge.common.util.BlockSnapshot blockSnapshot = null;
			if (this.captureBlockSnapshots && !this.isClientSide) {
				blockSnapshot = net.minecraftforge.common.util.BlockSnapshot.create(this.dimension(), this, actualPos, pFlags);
				this.capturedBlockSnapshots.add(blockSnapshot);
			}
			
			BlockState old = levelchunk.getBlockState(pPos);
			int oldLight = old.getLightEmission(this, actualPos);
			int oldOpacity = old.getLightBlock(this, actualPos);
			
			BlockState blockstate = levelchunk.setBlockState(pPos, pState, (pFlags & 64) != 0);
			if (blockstate == null) {
				if (blockSnapshot != null) this.capturedBlockSnapshots.remove(blockSnapshot);
				return false;
			} else {
				BlockState blockstate1 = levelchunk.getBlockState(pPos);
				if ((pFlags & 128) == 0 && blockstate1 != blockstate && (blockstate1.getLightBlock(this, pPos) != oldOpacity || blockstate1.getLightEmission(this, pPos) != oldLight || blockstate1.useShapeForLightOcclusion() || blockstate.useShapeForLightOcclusion())) {
					this.getProfiler().push("queueCheckLight");
					this.getChunkSource().getLightEngine().checkBlock(actualPos);
					this.getProfiler().pop();
				}
				
				if (blockSnapshot == null) // Don't notify clients or update physics while capturing blockstates
					this.markAndNotifyBlock(actualPos, levelchunk, blockstate, pState, pFlags, pRecursionLeft);
				
				return true;
			}
		}
	}
	
	// sounds
	@Override
	public void playSound(@Nullable Player pPlayer, Entity pEntity, SoundEvent pEvent, SoundSource pCategory, float pVolume, float pPitch) {
		this.playSound(pPlayer, pEntity.getX(), pEntity.getY(), pEntity.getZ(), pEvent, pCategory, pVolume, pPitch);
	}
	
	@Override
	public void playSound(@Nullable Player pPlayer, double pX, double pY, double pZ, SoundEvent pSound, SoundSource pCategory, float pVolume, float pPitch) {
		double scl = 1f / upb;
		BlockPos pos = getRegion().pos.toBlockPos();
		pX *= scl;
		pY *= scl;
		pZ *= scl;
		pX += pos.getX();
		pY += pos.getY();
		pZ += pos.getZ();
		double finalPX = pX;
		double finalPY = pY;
		double finalPZ = pZ;
		Level lvl = parent.get();
		if (lvl == null) return;
		completeOnTick.add(() -> {
			for (Player player : lvl.players()) {
				if (player == pPlayer) continue;
				
				double fScl = scl;
				if (ToolProvider.RESIZING.isResizingModPresent())
					fScl *= 1 / ToolProvider.RESIZING.getSize(player);
				if (fScl > 1) fScl = 1 / fScl;
				parent.get().playSound(
						pPlayer,
						finalPX, finalPY, finalPZ,
						pSound, pCategory, (float) (pVolume * fScl),
						pPitch
				);
			}
		});
	}
	
	@Override
	public void playLocalSound(double pX, double pY, double pZ, SoundEvent pSound, SoundSource pCategory, float pVolume, float pPitch, boolean pDistanceDelay) {
		double scl = 1f / upb;
		BlockPos pos = getRegion().pos.toBlockPos();
		pX *= scl;
		pY *= scl;
		pZ *= scl;
		pX += pos.getX();
		pY += pos.getY();
		pZ += pos.getZ();
		double finalPX = pX;
		double finalPY = pY;
		double finalPZ = pZ;
		Level lvl = parent.get();
		if (lvl == null) return;
		completeOnTick.add(() -> {
			for (Player player : lvl.players()) {
				double fScl = scl;
				if (ToolProvider.RESIZING.isResizingModPresent())
					fScl *= 1 / ToolProvider.RESIZING.getSize(player);
				if (fScl > 1) fScl = 1 / fScl;
				parent.get().playLocalSound(
						finalPX, finalPY, finalPZ,
						pSound, pCategory, (float) (pVolume * fScl),
						pPitch, pDistanceDelay
				);
			}
		});
	}
	
	/* forge specific */
	
	// TODO: modify this?
	@Override
	public void playSeededSound(@javax.annotation.Nullable Player p_215017_, double pX, double pY, double pZ, SoundEvent pSound, SoundSource pSource, float pVolume, float pPitch, long pSeed) {
		net.minecraftforge.event.PlayLevelSoundEvent.AtPosition event = net.minecraftforge.event.ForgeEventFactory.onPlaySoundAtPosition(this, pX, pY, pZ, Holder.direct(pSound), pSource, pVolume, pPitch);
		if (event.isCanceled() || event.getSound() == null) return;
		pSound = event.getSound().value();
		pSource = event.getSource();
		pVolume = event.getNewVolume();
		pPitch = event.getNewPitch();
		double scl = 1f / upb;
		BlockPos pos = getRegion().pos.toBlockPos();
		double ox = pX, oy = pY, oz = pZ;
		
		pX *= scl;
		pY *= scl;
		pZ *= scl;
		pX += pos.getX();
		pY += pos.getY();
		pZ += pos.getZ();
		double finalPX = pX;
		double finalPY = pY;
		double finalPZ = pZ;
		SoundEvent finalPSound = pSound;
		float finalPVolume = pVolume;
		float finalPPitch = pPitch;
		SoundSource finalPSource = pSource;
		Level lvl = parent.get();
		if (lvl == null) return;
		completeOnTick.add(() -> {
			for (Player player : lvl.players()) {
				double fScl = scl;
				if (ToolProvider.RESIZING.isResizingModPresent())
					fScl *= 1 / ToolProvider.RESIZING.getSize(player);
				if (fScl > 1) fScl = 1 / fScl;
				broadcastTo(p_215017_, ox, oy, oz, (double) finalPSound.getRange(finalPVolume), this.dimension(), new ClientboundSoundPacket(Holder.direct(finalPSound), finalPSource, finalPX, finalPY, finalPZ, (finalPVolume * (float) fScl), finalPPitch, pSeed));
			}
		});
	}
	
	@Override
	protected Object __platformCapabilities() {
		return getCapabilities();
	}
}

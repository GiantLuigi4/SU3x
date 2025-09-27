package tfc.smallerunits.fabric.level.server;

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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.Nullable;
import tfc.smallerunits.core.simulation.level.server.AbstractTickerServerLevel;
import tfc.smallerunits.plat.internal.ToolProvider;
import tfc.smallerunits.storage.IRegion;

import java.util.List;

public class FabricTickerServerLevel extends AbstractTickerServerLevel {
	public FabricTickerServerLevel(MinecraftServer server, ServerLevelData data, ResourceKey<Level> p_8575_, DimensionType dimType, ChunkProgressListener progressListener, ChunkGenerator generator, boolean p_8579_, long p_8580_, List<CustomSpawner> spawners, boolean p_8582_, Level parent, int upb, IRegion region) {
		super(server, data, p_8575_, dimType, progressListener, generator, p_8579_, p_8580_, spawners, p_8582_, parent, upb, region);
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
	
	// TODO: modify this?
	@Override
	public void playSeededSound(@javax.annotation.Nullable Player p_215017_, double pX, double pY, double pZ, SoundEvent pSound, SoundSource pSource, float pVolume, float pPitch, long pSeed) {
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
		return this;
	}
}
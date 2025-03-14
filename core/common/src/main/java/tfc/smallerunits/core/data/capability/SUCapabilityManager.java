package tfc.smallerunits.core.data.capability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.client.access.tracking.FastCapabilityHandler;
import tfc.smallerunits.plat.net.PacketTarget;
import tfc.smallerunits.plat.util.PlatformProvider;

// so I mostly just abandoned any documentation that I was given and write this
// CCA's readme is actually extremely good
// way better than anything I've ever had on forge
// https://github.com/OnyxStudios/Cardinal-Components-API/blob/1.18/README.md
// this whole class is basically just a wrapper around forge's method of storing additional data to a chunk
// on fabric, it should either be pretty similar or simpler
public class SUCapabilityManager {
	/**
	 * Idk what there is to say about this
	 * Imo, the name says it all
	 *
	 * @param chunk the chunk in question
	 * @return the corresponding ISUCapability
	 */
	public static ISUCapability getCapability(LevelChunk chunk) {
		if (chunk instanceof FastCapabilityHandler)
			return ((FastCapabilityHandler) chunk).getSUCapability();
		return (ISUCapability) PlatformProvider.UTILS.getSuCap(chunk);
	}
	
	public static ISUCapability getCapability(Level lvl, ChunkAccess chunk) {
		if (chunk instanceof LevelChunk) return getCapability((LevelChunk) chunk);
		return getCapability(lvl.getChunkAt(chunk.getPos().getWorldPosition()));
	}
	
	/**
	 * basically; I'm too lazy to fully port unimportant stuff sometimes
	 * This will likely be slower than calling the overload which takes a chunk
	 * Reason: this runs validation even if it doesn't need to, and if you already have the chunk, then you don't need to get the chunk again
	 * This method will get the chunk, validate it, return it's capability
	 */
	public static ISUCapability getCapability(Level world, ChunkPos pos) {
		ChunkAccess access = world.getChunk(/* CC safety */ pos.getWorldPosition());
		if (!(access instanceof LevelChunk)) return getCapability(world.getChunkAt(pos.getWorldPosition()));
		if (access instanceof FastCapabilityHandler chunk) return chunk.getSUCapability();
		return (ISUCapability) PlatformProvider.UTILS.getSuCap((LevelChunk) access);
	}
	
	public static void onChunkLoad(LevelChunk chunk) {
		ISUCapability capability = SUCapabilityManager.getCapability(chunk);
		for (UnitSpace unit : capability.getUnits()) unit.tick();
	}
	
	public static void onChunkWatch(LevelChunk chunk, ServerPlayer player) {
		if (player != null) {
			ISUCapability capability = SUCapabilityManager.getCapability(chunk);
			if (capability == null) return;
			for (UnitSpace unit : capability.getUnits()) {
				if (unit == null) continue;
				unit.sendSync(PacketTarget.player(player));
			}
		}
	}
	
	public static void ip$onChunkWatch(LevelChunk chunk, ServerPlayer player) {
		if (player != null) {
			ISUCapability capability = SUCapabilityManager.getCapability(chunk);
			if (capability == null) return;
			for (UnitSpace unit : capability.getUnits()) {
				if (unit == null) continue;
				unit.sendSync(PacketTarget.player(player));
			}
		}
	}
}

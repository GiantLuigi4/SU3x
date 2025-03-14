package tfc.smallerunits.mixin.data.access;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import tfc.smallerunits.data.access.ChunkHolderAccessor;

import java.util.List;

// https://github.com/iPortalTeam/ImmersivePortalsMod/blob/1.19/imm_ptl_core/src/main/java/qouteall/imm_ptl/core/mixin/common/chunk_sync/MixinChunkHolder.java
// AT doesn't work here if I wanna be compatible with IP
@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin implements ChunkHolderAccessor {
	@Shadow protected abstract void broadcast(List<ServerPlayer> $$0, Packet<?> $$1);
	
	@Shadow protected abstract void broadcastBlockEntityIfNeeded(List<ServerPlayer> $$0, Level $$1, BlockPos $$2, BlockState $$3);
	
	@Override
	public void SU$call_broadcastBlockEntityIfNeeded(List<ServerPlayer> players, Level level, BlockPos blockpos, BlockState blockstate) {
		broadcastBlockEntityIfNeeded(players, level, blockpos, blockstate);
	}
	
	@Override
	public void SU$call_broadcast(List<ServerPlayer> players, Packet<?> packet) {
		broadcast(players, packet);
	}
}

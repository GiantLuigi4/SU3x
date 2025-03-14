package tfc.smallerunits.data.access;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public interface ChunkHolderAccessor {
	void SU$call_broadcast(List<ServerPlayer> players, Packet<?> packet);
	
	void SU$call_broadcastBlockEntityIfNeeded(List<ServerPlayer> players, Level level, BlockPos blockpos, BlockState blockstate);
}

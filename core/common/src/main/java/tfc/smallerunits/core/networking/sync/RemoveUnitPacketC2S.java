package tfc.smallerunits.core.networking.sync;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.core.data.capability.SUCapabilityManager;
import tfc.smallerunits.core.logging.Loggers;
import tfc.smallerunits.plat.net.NetCtx;
import tfc.smallerunits.plat.net.Packet;
import tfc.smallerunits.plat.util.PlatformProvider;

public class RemoveUnitPacketC2S extends Packet {
	BlockPos position;
	
	public RemoveUnitPacketC2S(BlockPos position) {
		this.position = position;
	}
	
	public RemoveUnitPacketC2S(FriendlyByteBuf buf) {
		super(buf);
		this.position = buf.readBlockPos();
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		super.write(buf);
		buf.writeBlockPos(position);
	}
	
	@Override
	public void handle(NetCtx ctx) {
		super.handle(ctx);
		ctx.enqueueWork(() -> {
			if (checkServer(ctx)) {
				double reach = PlatformProvider.UTILS.getReach(ctx.getSender());
				reach *= 1.1; // help account for lag
				reach += 1; // TODO: do this a bit better, helps account for player scaling
				Vec3 pos = ctx.getSender().getPosition(0);
				if (Math.sqrt(pos.distanceToSqr(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5)) < reach) {
					Level lvl = ctx.getSender().level();
					LevelChunk chunk = lvl.getChunkAt(position);
					//noinspection ConstantConditions
					if (chunk == null || chunk instanceof EmptyLevelChunk) {
						Loggers.SU_LOGGER.warn("No chunk exists at " + position + " in world " + lvl);
						return;
					}
					ISUCapability cap = SUCapabilityManager.getCapability(chunk);
					if (cap == null) {
						Loggers.SU_LOGGER.warn("Capability at chunk " + position + " is null in world " + lvl);
						return;
					}
					UnitSpace space = cap.getUnit(position);
					if (space != null && space.isEmpty())
						ctx.getSender().level().removeBlock(position, false);
					else
						Loggers.SU_LOGGER.warn(ctx.getSender().getName().getString() + " tried to remove a non-empty unit space");
				} else {
					Loggers.SU_LOGGER.warn(ctx.getSender().getName().getString() + " tried to remove a unit space from to far away " + position.distToCenterSqr(pos));
				}
			}
		});
		ctx.setPacketHandled(true);
	}
}

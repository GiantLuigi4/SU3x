package tfc.smallerunits.core.networking.core;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import tfc.smallerunits.core.Registry;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.core.data.capability.SUCapabilityManager;
import tfc.smallerunits.core.utils.selection.UnitHitResult;
import tfc.smallerunits.plat.net.NetCtx;
import tfc.smallerunits.plat.net.Packet;

public class UnitInteractionPacket extends Packet {
	UnitHitResult result;
	
	public UnitInteractionPacket(UnitHitResult result) {
		this.result = result;
	}
	
	public UnitInteractionPacket(FriendlyByteBuf buf) {
		result = new UnitHitResult(
				new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
				buf.readEnum(Direction.class),
				buf.readBlockPos(), buf.readBoolean(),
				buf.readBlockPos(), null
		);
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeDouble(result.getLocation().x);
		buf.writeDouble(result.getLocation().y);
		buf.writeDouble(result.getLocation().z);
		buf.writeEnum(result.getDirection());
		buf.writeBlockPos(result.getBlockPos());
		buf.writeBoolean(result.isInside());
		buf.writeBlockPos(result.geetBlockPos());
	}
	
	@Override
	public void handle(NetCtx ctx) {
		if (checkServer(ctx)) {
			Player player = ctx.getSender();
			Level lvl = ctx.getSender().level();
			
			ChunkAccess access = Minecraft.getInstance().level.getChunk(result.getBlockPos());
			if (access instanceof EmptyLevelChunk) return;
			if (!(access instanceof LevelChunk chunk)) return;
			ISUCapability cap = SUCapabilityManager.getCapability(chunk);
			UnitSpace space = cap.getOrMakeUnit(result.getBlockPos());
			
			BlockHitResult result1 = new BlockHitResult(
					result
							.getLocation()
							.subtract(result.getBlockPos().getX(), result.getBlockPos().getY(), result.getBlockPos().getZ())
							.add(result.geetBlockPos().getX(), result.geetBlockPos().getY(), result.geetBlockPos().getZ())
//							.scale(space.unitsPerBlock)
					,
					result.getDirection(),
					space.getOffsetPos(result.geetBlockPos()), result.isInside()
			);
			
			InteractionResult res = Registry.UNIT_SPACE.get().use(
					lvl.getBlockState(result1.getBlockPos()),
					lvl, result1.getBlockPos(), player,
					InteractionHand.MAIN_HAND, result1
			);
			InteractionHand hand = InteractionHand.MAIN_HAND;
			if (!res.consumesAction() && res != InteractionResult.SUCCESS) {
				hand = InteractionHand.OFF_HAND;
				res = Registry.UNIT_SPACE.get().use(
						lvl.getBlockState(result1.getBlockPos()),
						lvl, result1.getBlockPos(), player,
						InteractionHand.MAIN_HAND, result1
				);
			}
			if (res.shouldSwing()) {
				player.swing(hand);
			}
		}
	}
}

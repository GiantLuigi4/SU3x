package tfc.smallerunits.plat.net;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketListener;
import net.minecraft.world.entity.player.Player;
import tfc.smallerunits.common.logging.Loggers;
import tfc.smallerunits.plat.util.PlatformProvider;

public class FabricNetCtx extends NetCtx {
	public FabricNetCtx(PacketListener handler, PacketSender responseSender, Player player, NetworkDirection direction) {
		super(handler, responseSender, player, direction);
	}
	
	public Player getSender() {
		return sender;
	}
	
	public void respond(Packet packet) {
		responseSender.send(packet);
	}
	
	public PacketListener getHandler() {
		return handler;
	}
	
	public void setPacketHandled(boolean b) {
	}
	
	public NetworkDirection getDirection() {
		return direction;
	}
	
	public void enqueueWork(Runnable r) {
		if (PlatformProvider.UTILS.isClient() && (sender == null || sender.level().isClientSide)) {
			Minecraft.getInstance().tell(r);
		} else {
			if (sender != null) {
				sender.level().getServer().execute(r);
			} else {
				r.run(); // whar
				Loggers.SU_LOGGER.warn("A null sender on server???");
			}
		}
	}
}

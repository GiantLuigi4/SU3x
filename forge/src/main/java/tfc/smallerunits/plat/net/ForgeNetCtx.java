package tfc.smallerunits.plat.net;

import net.minecraft.network.PacketListener;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public class ForgeNetCtx extends NetCtx {
	Player sender;
	PacketListener handler;
	PacketSender responseSender;
	NetworkDirection direction;
	
	NetworkEvent.Context context;
	
	public ForgeNetCtx(PacketListener handler, PacketSender responseSender, Player player, NetworkDirection direction, NetworkEvent.Context context) {
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
		context.setPacketHandled(b);
	}
	
	public NetworkDirection getDirection() {
		return direction;
	}
	
	public void enqueueWork(Runnable r) {
		context.enqueueWork(r);
	}
}

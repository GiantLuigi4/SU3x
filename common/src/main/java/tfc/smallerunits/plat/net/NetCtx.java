package tfc.smallerunits.plat.net;

import net.minecraft.network.PacketListener;
import net.minecraft.world.entity.player.Player;

public abstract class NetCtx {
	Player sender;
	PacketListener handler;
	PacketSender responseSender;
	NetworkDirection direction;

	public NetCtx(PacketListener handler, PacketSender responseSender, Player player, NetworkDirection direction) {
		this.handler = handler;
		this.responseSender = responseSender;
		this.sender = player;
		this.direction = direction;
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
		throw new RuntimeException();
	}
}
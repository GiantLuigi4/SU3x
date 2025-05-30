package tfc.smallerunits.plat.net;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class PacketRegister {
	public final ResourceLocation channel;

	protected PacketRegister(
			ResourceLocation name,
			String networkVersion,
			Predicate<String> clientChecker,
			Predicate<String> serverChecker
	) {
		this.channel = name;
	}

    public static PacketRegister of(
			ResourceLocation name,
			String networkVersion,
			Predicate<String> clientChecker,
			Predicate<String> serverChecker
	) {
		throw new RuntimeException("Check platform module self-impl mixins");
    }

    public net.minecraft.network.protocol.Packet<?> toVanillaPacket(Packet wrapperPacket, NetworkDirection toClient) {
		throw new RuntimeException();
	}

	public <T extends Packet> void registerMessage(int indx, Class<T> clazz, BiConsumer<Packet, FriendlyByteBuf> writer, Function<FriendlyByteBuf, T> fabricator, BiConsumer<Packet, NetCtx> handler) {
		throw new RuntimeException();
	}
	
	public void send(PacketTarget target, Packet pkt) {
		throw new RuntimeException();
	}
	
	public int getId(Packet packet) {
		throw new RuntimeException();
	}
	
	public FriendlyByteBuf encode(Packet pkt) {
		throw new RuntimeException();
	}
	
	private static FriendlyByteBuf newByteBuf() {
		return new FriendlyByteBuf(Unpooled.buffer());
	}
}

package tfc.smallerunits.core.mixin.compat.ip;

//import net.minecraft.network.protocol.Packet;
//import net.minecraft.network.protocol.game.ClientGamePacketListener;
//import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
//import net.minecraft.resources.ResourceKey;
//import net.minecraft.world.level.Level;
//import org.spongepowered.asm.mixin.Final;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import qouteall.imm_ptl.core.network.PacketRedirectionClient;
//import tfc.smallerunits.core.networking.hackery.NetworkContext;
//import tfc.smallerunits.core.networking.hackery.NetworkHandlingContext;
//import tfc.smallerunits.core.networking.hackery.NetworkingHacks;
//import tfc.smallerunits.plat.util.PlatformUtils;

//@Mixin(value = PacketRedirectionClient.class, remap = false)
//public class PacketRedirectionClientMixin {
//	@Shadow
//	@Final
//	public static ThreadLocal<ResourceKey<Level>> clientTaskRedirection;
//
//	@Inject(at = @At("HEAD"), method = "handleRedirectedPacketFromNetworkingThread", cancellable = true)
//	private static void preHandle(ResourceKey<Level> dimension, Packet<ClientGamePacketListener> packet, ClientGamePacketListener handler, CallbackInfo ci) {
//		NetworkHandlingContext ctx;
//
//		if ((ctx = NetworkingHacks.currentContext.get()) != null) {
//			NetworkContext context = ctx.netContext;
//
//			if (packet instanceof ClientboundCustomPayloadPacket clientboundCustomPayloadPacket) {
//				ResourceKey<Level> oldTaskRedirection = clientTaskRedirection.get();
//				clientTaskRedirection.set(dimension);
//
//				PlatformUtils.customPayload(clientboundCustomPayloadPacket, context, handler);
//
//				clientTaskRedirection.set(oldTaskRedirection);
//
//				ci.cancel();
//			}
//		}
//	}
//}

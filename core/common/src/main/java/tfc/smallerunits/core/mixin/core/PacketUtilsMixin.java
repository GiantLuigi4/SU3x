package tfc.smallerunits.core.mixin.core;

import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.networking.hackery.PacketUtilMess;

@Mixin(PacketUtils.class)
public class PacketUtilsMixin {
	@Inject(at = @At("HEAD"), method = {"lambda$ensureRunningOnSameThread$0"}, require = 0)
	private static <T extends PacketListener> void preHandlePacket(PacketListener listener, Packet packet, CallbackInfo ci) {
		PacketUtilMess.preHandlePacket(listener, packet);
	}
	
	@Inject(at = @At("RETURN"), method = {"lambda$ensureRunningOnSameThread$0"}, require = 0)
	private static <T extends PacketListener> void postHandlePacket(PacketListener listener, Packet packet, CallbackInfo ci) {
		PacketUtilMess.postHandlePacket(listener, packet);
	}

	// TODO: does this still work
	// should?
	// optifine moment
	// for some reason, optifine renames this lambda and reverses the parameter order
	@SuppressWarnings("MixinAnnotationTarget")
	@Inject(at = @At("HEAD"), method = {"lambda$ensureRunningOnSameThread$0"}, require = 0, remap = false)
	private static <T extends PacketListener> void preHandlePacketOF(PacketListener listener, Packet packet, CallbackInfo ci) {
		PacketUtilMess.preHandlePacket(listener, packet);
	}
	
	@SuppressWarnings("MixinAnnotationTarget")
	@Inject(at = @At("RETURN"), method = {"lambda$ensureRunningOnSameThread$0"}, require = 0, remap = false)
	private static <T extends PacketListener> void postHandlePacketOF(PacketListener listener, Packet packet, CallbackInfo ci) {
		PacketUtilMess.postHandlePacket(listener, packet);
	}
}

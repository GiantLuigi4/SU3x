package tfc.smallerunits.core.mixin.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfc.smallerunits.core.networking.SUNetworkRegistry;
import tfc.smallerunits.core.networking.core.DestroyUnitPacket;
import tfc.smallerunits.core.utils.selection.UnitHitResult;
import tfc.smallerunits.plat.net.PacketTarget;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Shadow
	@Final
	private Minecraft minecraft;
	
	@Inject(at = @At("HEAD"), method = "destroyBlock", cancellable = true)
	public void preDestroyBlock(BlockPos flag, CallbackInfoReturnable<Boolean> cir) {
		HitResult result = minecraft.hitResult;
		if (result instanceof UnitHitResult) {
			DestroyUnitPacket packet = new DestroyUnitPacket((UnitHitResult) result);
			SUNetworkRegistry.NETWORK_INSTANCE.send(PacketTarget.SERVER, packet);
			cir.setReturnValue(false);
		}
	}
	
	@Inject(at = @At("HEAD"), method = "startPrediction", cancellable = true)
	public void preSendAction(ClientLevel p_233730_, PredictiveAction p_233731_, CallbackInfo ci) {
		HitResult result = minecraft.hitResult;
		if (result instanceof UnitHitResult) {
			BlockStatePredictionHandler blockStatePredictionHandler = p_233730_.getBlockStatePredictionHandler();
			int i = blockStatePredictionHandler.currentSequence() + 1;
			if (p_233731_.predict(i) instanceof ServerboundPlayerActionPacket)
				// don't send the packet?
				ci.cancel();
		}
	}
}

package tfc.smallerunits.core.mixin.core.gui.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.data.access.SUScreenAttachments;
import tfc.smallerunits.core.utils.PositionalInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"), method = "tick")
	public void preCheckContainer(CallbackInfo ci) {
		SUScreenAttachments screenAttachments = ((SUScreenAttachments) ((Player) (Object) this).containerMenu);
		PositionalInfo info = screenAttachments.getPositionalInfo();
		if (info != null) {
			screenAttachments.update((Player) (Object) this);
			info.scalePlayerReach(((Player) (Object) this), screenAttachments.getDescriptor().getReachScale());
			info.adjust((Player) (Object) this, ((Player) (Object) this).level(), screenAttachments.getDescriptor(), true);
		}
	}
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z", shift = At.Shift.AFTER), method = "tick")
	public void postCheckContainer(CallbackInfo ci) {
		SUScreenAttachments screenAttachments = ((SUScreenAttachments) ((Player) (Object) this).containerMenu);
		PositionalInfo info = screenAttachments.getPositionalInfo();
		if (info != null) {
			info.reset((Player) (Object) this);
		}
	}
}

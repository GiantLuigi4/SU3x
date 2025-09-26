package tfc.smallerunits.core.mixin.core.gui.server.dist;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfc.smallerunits.common.UUIDs;
import tfc.smallerunits.level.SimpleTickerLevel;
import tfc.smallerunits.plat.util.PlatformProvider;

@Mixin(RandomizableContainerBlockEntity.class)
public class RandomizableContainerBlockEntityMixin {
	@Inject(at = @At("HEAD"), method = "stillValid", cancellable = true)
	public void scale(Player $$0, CallbackInfoReturnable<Boolean> cir) {
		if ($$0.level() instanceof SimpleTickerLevel) {
			AttributeInstance instance = PlatformProvider.UTILS.getReachAttrib($$0);
			if (instance == null) return;
			AttributeModifier modifier = instance.getModifier(UUIDs.SU_REACH_UUID);
			if (modifier == null) return;
			
			BlockEntity be = (BlockEntity) (Object) this;
			if (be.getLevel().getBlockEntity(be.worldPosition) != be) {
				cir.setReturnValue(false);
			} else {
				cir.setReturnValue(!($$0.distanceToSqr((double)be.worldPosition.getX() + 0.5D, (double)be.worldPosition.getY() + 0.5D, (double)be.worldPosition.getZ() + 0.5D) > (64.0D * modifier.getAmount())));
			}
		}
	}
}

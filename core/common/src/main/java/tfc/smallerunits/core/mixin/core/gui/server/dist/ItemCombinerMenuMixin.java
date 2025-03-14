package tfc.smallerunits.core.mixin.core.gui.server.dist;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfc.smallerunits.core.utils.PositionalInfo;
import tfc.smallerunits.level.SimpleTickerLevel;
import tfc.smallerunits.plat.util.PlatformProvider;

@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin {
	@Shadow @Final protected ContainerLevelAccess access;
	
	@Shadow protected abstract boolean isValidBlock(BlockState blockState);
	
	@Inject(at = @At("HEAD"), method = "stillValid", cancellable = true)
	private void scale(Player $$0, CallbackInfoReturnable<Boolean> cir) {
		if ($$0.level() instanceof SimpleTickerLevel) {
			AttributeInstance instance = PlatformProvider.UTILS.getReachAttrib($$0);
			if (instance == null) return;
			AttributeModifier modifier = instance.getModifier(PositionalInfo.SU_REACH_UUID);
			if (modifier == null) return;
			
			cir.setReturnValue(this.access
					.evaluate(
							(var2, var3) -> this.isValidBlock(var2.getBlockState(var3)) && $$0.distanceToSqr((double) var3.getX() + 0.5, (double) var3.getY() + 0.5, (double) var3.getZ() + 0.5) <= (64.0 * modifier.getAmount()),
							true
					));
		}
	}
}

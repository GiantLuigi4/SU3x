package tfc.smallerunits.forge.mixin.quality;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfc.smallerunits.level.SimpleTickerLevel;

@Mixin(Player.class)
public abstract class PlayerMixin {
	@Shadow
	public abstract void increaseScore(int pScore);
	
	@Inject(at = @At("TAIL"), method = "getDigSpeed", remap = false, cancellable = true)
	public void afflictMiningSpeed(BlockState pState, BlockPos pos, CallbackInfoReturnable<Float> cir) {
		//noinspection ConstantConditions
		if (!((Object) this instanceof FakePlayer)) {
			if (((Player) (Object) this).level() instanceof SimpleTickerLevel tickerWorld) {
				cir.setReturnValue(cir.getReturnValue() * tickerWorld.getUPB());
			}
		}
	}
}

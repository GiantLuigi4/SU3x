package tfc.smallerunits.core.mixin.dangit;

import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfc.smallerunits.plat.util.PlatformProvider;

@Mixin(Util.class)
public class MojangWhy {
	@Inject(at = @At("TAIL"), method = "getMaxThreads", cancellable = true, require = 0)
	private static void preGetThreads(CallbackInfoReturnable<Integer> cir) {
		// 255 is too much as a default
		if (PlatformProvider.UTILS.isDevEnv())
			cir.setReturnValue(3);
	}
}

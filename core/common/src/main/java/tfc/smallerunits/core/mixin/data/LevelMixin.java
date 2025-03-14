package tfc.smallerunits.core.mixin.data;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.client.access.tracking.SUCapableWorld;
import tfc.smallerunits.core.client.render.SUVBOEmitter;
import tfc.smallerunits.core.logging.Loggers;
import tfc.smallerunits.plat.util.PlatformProvider;

@Mixin(Level.class)
public class LevelMixin implements SUCapableWorld {
	@Unique
	private SUVBOEmitter emitter = new SUVBOEmitter();
	
	@Override
	public SUVBOEmitter getVBOEmitter() {
		return emitter;
	}
	
	@Inject(at = @At("HEAD"), method = "close")
	public void preClose(CallbackInfo ci) {
		if (emitter != null) emitter.free();
		if (PlatformProvider.UTILS.isDevEnv()) Loggers.WORLD_LOGGER.info("World " + toString() + " offloaded!");
	}
}

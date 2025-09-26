package tfc.smallerunits.core.mixin.data;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.common.logging.Loggers;
import tfc.smallerunits.core.client.access.tracking.SUCapableWorld;
import tfc.smallerunits.core.client.access.tracking.SUVBOEmittingWorld;
import tfc.smallerunits.core.client.render.SUVBOEmitter;
import tfc.smallerunits.plat.util.PlatformProvider;

import java.util.function.Supplier;

@Mixin(Level.class)
public class LevelMixin implements SUCapableWorld, SUVBOEmittingWorld {
	@Unique
	private SUVBOEmitter emitter;

	@Inject(at = @At("TAIL"), method = "<init>")
	public void postInit(WritableLevelData levelData, ResourceKey dimension, RegistryAccess registryAccess, Holder dimensionTypeRegistration, Supplier profiler, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates, CallbackInfo ci) {
		if ((Object) this instanceof ClientLevel) {
			emitter = new SUVBOEmitter();
		}
	}

	@Override
	public SUVBOEmitter getVBOEmitter() {
		return emitter;
	}

	@Inject(at = @At("HEAD"), method = "close")
	public void preClose(CallbackInfo ci) {
		if (emitter != null) {
			emitter.free();
			emitter = null;
			if (PlatformProvider.UTILS.isDevEnv()) Loggers.WORLD_LOGGER.info("World " + toString() + " emitter freed!");
		}
		if (PlatformProvider.UTILS.isDevEnv()) Loggers.WORLD_LOGGER.info("World " + toString() + " offloaded!");
	}

	@Override
	public void freeSUEmitter() {
		if (emitter != null) {
			emitter.free();
			emitter = null;
			if (PlatformProvider.UTILS.isDevEnv()) Loggers.WORLD_LOGGER.info("World " + toString() + " emitter freed!");
		}
	}
}

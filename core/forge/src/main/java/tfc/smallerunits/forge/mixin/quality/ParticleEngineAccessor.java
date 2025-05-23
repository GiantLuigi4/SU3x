package tfc.smallerunits.forge.mixin.quality;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.client.render.compat.UnitParticleEngine;

import java.util.Map;

@Mixin(ParticleEngine.class)
public class ParticleEngineAccessor implements tfc.smallerunits.plat.itf.access.ParticleEngineAccessor {
	@Final
	@Shadow
	@Mutable
	private Map<ResourceLocation, ParticleProvider<?>> providers;
	
	@Inject(at = @At("HEAD"), method = "registerProviders", cancellable = true)
	public void preRegisterProviders(CallbackInfo ci) {
		//noinspection ConstantConditions
		if (((Object) this) instanceof UnitParticleEngine)
			ci.cancel();
	}
	
	@Override
	public void copyProviders(ParticleEngine source) {
		providers = ((tfc.smallerunits.plat.itf.access.ParticleEngineAccessor) source).getProviders();
	}
	
	@Override
	public Map<ResourceLocation, ParticleProvider<?>> getProviders() {
		return providers;
	}
}

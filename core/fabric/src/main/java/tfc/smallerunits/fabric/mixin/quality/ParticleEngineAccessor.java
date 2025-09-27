package tfc.smallerunits.fabric.mixin.quality;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
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
	private Int2ObjectMap<ParticleProvider<?>> providers;
	
	@Inject(at = @At("HEAD"), method = "registerProviders", cancellable = true)
	public void preRegisterProviders(CallbackInfo ci) {
		//noinspection ConstantConditions
		if (((Object) this) instanceof UnitParticleEngine)
			ci.cancel();
	}
	
	@Override
	public void copyProviders(ParticleEngine source) {
		//noinspection unchecked
		providers = (Int2ObjectMap<ParticleProvider<?>>) ((tfc.smallerunits.plat.itf.access.ParticleEngineAccessor) source).getProviders();
	}
	
	@Override
	public Map getProviders() {
		return providers;
	}
}

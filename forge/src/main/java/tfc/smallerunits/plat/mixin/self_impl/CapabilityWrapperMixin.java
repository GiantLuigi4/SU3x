package tfc.smallerunits.plat.mixin.self_impl;

import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import tfc.smallerunits.plat.CapabilityWrapper;
import tfc.smallerunits.plat.ForgeCapabilityWrapper;

@Mixin(value = CapabilityWrapper.class, remap = false)
public class CapabilityWrapperMixin {
	/**
	 * @author GiantLuigi4
	 */
	@Overwrite
	public static CapabilityWrapper of(Object par1) {
		return new ForgeCapabilityWrapper((CapabilityDispatcher) par1);
	}
}

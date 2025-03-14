package tfc.smallerunits.plat.mixin.self_impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import tfc.smallerunits.plat.ForgePlatformRegistry;
import tfc.smallerunits.plat.PlatformRegistry;

@Mixin(value = PlatformRegistry.class, remap = false)
public class PlatformRegistryMixin {
    /**
     * @author GiantLuigi4
     */
    @Overwrite
    public static <T> PlatformRegistry<T> makeRegistry(Class<T> cls, String modid) {
        return new ForgePlatformRegistry<>(cls, modid);
    }
}

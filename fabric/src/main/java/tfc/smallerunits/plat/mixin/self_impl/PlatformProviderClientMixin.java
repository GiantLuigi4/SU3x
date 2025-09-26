package tfc.smallerunits.plat.mixin.self_impl;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import tfc.smallerunits.plat.util.FabricPlatformUtilsClient;
import tfc.smallerunits.plat.util.PlatformProviderClient;
import tfc.smallerunits.plat.util.PlatformUtilsClient;

@Mixin(PlatformProviderClient.class)
public class PlatformProviderClientMixin {
    @Final
    @Shadow
    @Mutable
    public static PlatformUtilsClient UTILS;

    static {
        UTILS = new FabricPlatformUtilsClient();
    }
}

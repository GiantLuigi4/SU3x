package tfc.smallerunits.plat.mixin.self_impl;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import tfc.smallerunits.plat.util.PlatformProvider;
import tfc.smallerunits.plat.util.ForgePlatformUtils;
import tfc.smallerunits.plat.util.ForgePlatformUtilsClient;
import tfc.smallerunits.plat.util.ForgeSUTabBuilder;

import java.util.function.Supplier;

@Mixin(PlatformProvider.class)
public class PlatformProviderMixin {
    @Shadow
    @Mutable
    public static ForgePlatformUtils UTILS;
    @Shadow
    @Mutable
    public static ForgePlatformUtilsClient UTILS_CLIENT;

    static {
        UTILS = new ForgePlatformUtils();
        UTILS_CLIENT = new ForgePlatformUtilsClient();
    }

    /**
     * @author GiantLuigi4
     * @reason implement
     */
    @Overwrite
    public static ForgeSUTabBuilder makeTabBuilder(String name, Supplier<ItemStack> icon) {
        return new ForgeSUTabBuilder(name, icon);
    }
}

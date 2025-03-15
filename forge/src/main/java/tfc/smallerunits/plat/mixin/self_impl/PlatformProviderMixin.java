package tfc.smallerunits.plat.mixin.self_impl;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import tfc.smallerunits.plat.util.*;

import java.util.function.Supplier;

@Mixin(value = PlatformProvider.class, remap = false)
public class PlatformProviderMixin {
    @Final
    @Shadow
    @Mutable
    public static PlatformUtils UTILS;
    @Final
    @Shadow
    @Mutable
    public static PlatformUtilsClient UTILS_CLIENT;

    static {
        UTILS = new ForgePlatformUtils();
        UTILS_CLIENT = new ForgePlatformUtilsClient();
    }

    /**
     * @author GiantLuigi4
     */
    @Overwrite
    public static SUTabBuilder makeTabBuilder(String name, Supplier<ItemStack> icon) {
        return new ForgeSUTabBuilder(name, icon);
    }
}

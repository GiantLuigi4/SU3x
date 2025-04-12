package tfc.smallerunits.plat.util;

import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class PlatformProvider {
    public static final PlatformUtils UTILS = null;
//    public static final PlatformUtilsClient UTILS_CLIENT = null;

    public static SUTabBuilder makeTabBuilder(String name, Supplier<ItemStack> icon) {
        throw new RuntimeException("Check platform mixins");
    }

    static {
        // no-op dummy method
    }
}

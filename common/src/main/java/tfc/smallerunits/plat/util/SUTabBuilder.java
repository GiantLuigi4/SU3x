package tfc.smallerunits.plat.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class SUTabBuilder {
    public abstract SUTabBuilder setTitle(Component component);

    public abstract SUTabBuilder addItem(Supplier<ItemStack> supplier);

    public abstract CreativeModeTab build();

    public abstract SUTabBuilder addItems(Consumer<Consumer<ItemStack>> populator);
}

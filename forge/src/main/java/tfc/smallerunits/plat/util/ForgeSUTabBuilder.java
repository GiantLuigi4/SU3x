package tfc.smallerunits.plat.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ForgeSUTabBuilder extends SUTabBuilder {
    String name;
    Supplier<ItemStack> icon;
    Component title;
    List<Consumer<Consumer<ItemStack>>> populators = new ArrayList<>();

    public ForgeSUTabBuilder(String name, Supplier<ItemStack> icon) {
        this.name = name;
        this.icon = icon;
    }

    public ForgeSUTabBuilder setTitle(Component component) {
        this.title = component;
        return this;
    }

    public ForgeSUTabBuilder addItem(Supplier<ItemStack> supplier) {
        populators.add(out -> out.accept(supplier.get()));
        return this;
    }

    public ForgeSUTabBuilder addItems(Consumer<Consumer<ItemStack>> populator) {
        this.populators.add(populator);
        return this;
    }

    public CreativeModeTab build() {
        return CreativeModeTab.builder()
                .icon(icon)
                .title(title)
                .displayItems((params, out) -> {
                    for (Consumer<Consumer<ItemStack>> item : populators) {
                        item.accept(out::accept);
                    }
                })
                .build();
    }
}

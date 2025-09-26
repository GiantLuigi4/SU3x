package tfc.smallerunits.plat.util;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FabricSUTabBuilder extends SUTabBuilder {
    String name;
    Supplier<ItemStack> icon;
    Component title;
    List<Consumer<Consumer<ItemStack>>> populators = new ArrayList<>();

    public FabricSUTabBuilder(String name, Supplier<ItemStack> icon) {
        this.name = name;
        this.icon = icon;
    }

    public FabricSUTabBuilder setTitle(Component component) {
        this.title = component;
        return this;
    }

    public FabricSUTabBuilder addItem(Supplier<ItemStack> supplier) {
        populators.add(out -> out.accept(supplier.get()));
        return this;
    }

    public FabricSUTabBuilder addItems(Consumer<Consumer<ItemStack>> populator) {
        this.populators.add(populator);
        return this;
    }

    public CreativeModeTab build() {
        return FabricItemGroup.builder()
                .icon(() -> icon.get()).displayItems((list, output) -> {
                })
                .title(title)
                .displayItems((i, d) -> {
                    for (Consumer<Consumer<ItemStack>> populator : populators) {
                        populator.accept(d::accept);
                    }
                }).build();
    }
}

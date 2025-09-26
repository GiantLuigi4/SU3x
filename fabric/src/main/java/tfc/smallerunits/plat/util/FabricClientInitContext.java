package tfc.smallerunits.plat.util;

import net.fabricmc.fabric.impl.client.rendering.BuiltinItemRendererRegistryImpl;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

import java.util.function.Supplier;

public class FabricClientInitContext extends ClientInitContext {
    tfc.smallerunits.plat.util.AbstractItem item;

    public FabricClientInitContext(AbstractItem item) {
        this.item = item;
    }

    public void registerRenderer(Supplier<BlockEntityWithoutLevelRenderer> renderer) {
        BuiltinItemRendererRegistryImpl.INSTANCE.register(item, renderer.get()::renderByItem);
    }
}

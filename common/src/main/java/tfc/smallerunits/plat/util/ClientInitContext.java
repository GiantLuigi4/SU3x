package tfc.smallerunits.plat.util;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

import java.util.function.Supplier;

public abstract class ClientInitContext {
    abstract void registerRenderer(Supplier<BlockEntityWithoutLevelRenderer> renderer);
}

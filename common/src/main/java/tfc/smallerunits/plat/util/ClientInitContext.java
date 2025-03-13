package tfc.smallerunits.plat.util;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

import java.util.function.Supplier;

public class ClientInitContext {
	public void registerRenderer(Supplier<BlockEntityWithoutLevelRenderer> renderer) {
		throw new RuntimeException();
	}
}

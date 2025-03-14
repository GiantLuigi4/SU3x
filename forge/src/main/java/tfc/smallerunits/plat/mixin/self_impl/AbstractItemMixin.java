package tfc.smallerunits.plat.mixin.self_impl;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import tfc.smallerunits.plat.util.ForgeClientInitContext;

import java.util.function.Consumer;

@Mixin(AbstractItem.class)
public class AbstractItemMixin {
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ForgeClientInitContext ctx = new ForgeClientInitContext(consumer);
        ((AbstractItem) (Object) (this)).initializeClient(ctx);
        ctx.finish();
    }
}

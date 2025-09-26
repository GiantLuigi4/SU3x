package tfc.smallerunits.plat.mixin.self_impl;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.plat.util.AbstractItem;
import tfc.smallerunits.plat.util.ClientInitContext;
import tfc.smallerunits.plat.util.FabricClientInitContext;

@Mixin(AbstractItem.class)
public abstract class AbstractItemMixin {
    @Shadow
    public abstract void initializeClient(ClientInitContext ctx);

    @Inject(at = @At("TAIL"), method = "<init>")
    public void postInit(Item.Properties properties, CallbackInfo ci) {
        initializeClient(new FabricClientInitContext((AbstractItem) (Object) this));
    }
}

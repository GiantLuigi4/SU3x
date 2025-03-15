package tfc.smallerunits.forge.mixin.self_impl;

import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import tfc.smallerunits.forge.CapabilityProvider;
import tfc.smallerunits.plat.internal.IResizingUtil;
import tfc.smallerunits.plat.internal.ToolProvider;

import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(value = ToolProvider.class, remap = false)
public class ToolProviderMixin {
    @Shadow @Final @Mutable public static IResizingUtil RESIZING;
    @Shadow @Final @Mutable public static Supplier<Object> CAPABILITY;
    @Shadow @Final @Mutable public static Function<Object, Connection> ACTIVE_CONTEXT;

    static {
        CAPABILITY = () -> CapabilityProvider.SU_CAPABILITY_TOKEN;
        ACTIVE_CONTEXT = (context) -> ((tfc.smallerunits.core.networking.hackery.NetworkContext) context).connection;
    }
}

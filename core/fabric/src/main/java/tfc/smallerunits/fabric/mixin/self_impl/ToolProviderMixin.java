package tfc.smallerunits.fabric.mixin.self_impl;

import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import tfc.smallerunits.fabric.ComponentRegistry;
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
        CAPABILITY = () -> ComponentRegistry.SU_CAPABILITY_COMPONENT_KEY;
        ACTIVE_CONTEXT = (context) -> ((tfc.smallerunits.core.networking.hackery.NetworkContext) context).connection;
    }
}

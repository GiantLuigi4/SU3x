package tfc.smallerunits.resizing.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.plat.internal.IResizingUtil;
import tfc.smallerunits.plat.internal.ToolProvider;
import tfc.smallerunits.resizing.ResizingSetup;

@Mixin(ToolProvider.class)
public class ToolProviderMixin {
    @Shadow
    @Final
    @Mutable
    public static IResizingUtil RESIZING;

    @Inject(at = @At("TAIL"), method = "<clinit>")
    private static void postInit(CallbackInfo ci) {
        IResizingUtil override = ResizingSetup.makeUtils();
        if (override != null) RESIZING = override;
    }
}

package tfc.smallerunits.general_compat.mixin.distant_horizons;

import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.simulation.level.ITickerLevel;

/**
 * When something happens to a small world, distant horizons should not acknowledge it
 * that is what this mixin does
 */
@Mixin(value = ClientApi.class, remap = false)
public class ClientAPIMixin {
    @Inject(at = @At("HEAD"), method = "clientTickEvent", cancellable = true)
    public void preTick(CallbackInfo ci) {
        if (Minecraft.getInstance().level instanceof ITickerLevel) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "clientLevelLoadEvent", cancellable = true)
    public void preLoadWorld(IClientLevelWrapper levelWrapper, CallbackInfo ci) {
        if (levelWrapper.getWrappedMcObject() instanceof ITickerLevel) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "clientLevelUnloadEvent", cancellable = true)
    public void preUnloadWorld(IClientLevelWrapper level, CallbackInfo ci) {
        if (level.getWrappedMcObject() instanceof ITickerLevel) {
            ci.cancel();
        }
    }
}

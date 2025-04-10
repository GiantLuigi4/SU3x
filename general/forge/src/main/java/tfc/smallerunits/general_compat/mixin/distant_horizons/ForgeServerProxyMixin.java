package tfc.smallerunits.general_compat.mixin.distant_horizons;

import com.seibel.distanthorizons.forge.ForgeServerProxy;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.simulation.level.ITickerLevel;

@Mixin(value = ForgeServerProxy.class, remap = false)
public class ForgeServerProxyMixin {
    // TODO
    @Inject(at = @At("HEAD"), method = "serverLevelLoadEvent", cancellable = true)
    public void preLoadWorld(LevelEvent.Load event, CallbackInfo ci) {
        if (event.getLevel() instanceof ITickerLevel) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "serverLevelUnloadEvent", cancellable = true)
    public void preUnloadWorld(LevelEvent.Unload event, CallbackInfo ci) {
        if (event.getLevel() instanceof ITickerLevel) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "serverChunkLoadEvent", cancellable = true)
    public void preLoadChunk(ChunkEvent.Load event, CallbackInfo ci) {
        if (event.getLevel() instanceof ITickerLevel) {
            ci.cancel();
        }
    }
}

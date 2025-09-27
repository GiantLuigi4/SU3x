package tfc.smallerunits.general_compat.mixin.distant_horizons;

import com.seibel.distanthorizons.core.api.internal.ServerApi;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.simulation.level.ITickerLevel;

@Mixin(value = ServerApi.class, remap = false)
public class ServerAPIMixin {
	@Inject(at = @At("HEAD"), method = "serverLevelLoadEvent", cancellable = true)
	public void preLoadLevel(IServerLevelWrapper level, CallbackInfo ci) {
		if (level.getWrappedMcObject() instanceof ITickerLevel) {
			ci.cancel();
		}
	}

    @Inject(at = @At("HEAD"), method = "serverLevelUnloadEvent", cancellable = true)
    public void preUnloadWorld(IServerLevelWrapper level, CallbackInfo ci) {
        if (level.getWrappedMcObject() instanceof ITickerLevel) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "serverChunkLoadEvent", cancellable = true)
    public void preLoadChunk(IChunkWrapper chunkWrapper, ILevelWrapper level, CallbackInfo ci) {
	    if (level.getWrappedMcObject() instanceof ITickerLevel) {
		    ci.cancel();
	    }
    }
	
	@Inject(at = @At("HEAD"), method = "serverChunkSaveEvent", cancellable = true)
	public void preChunkSave(IChunkWrapper chunkWrapper, ILevelWrapper level, CallbackInfo ci) {
		if (level.getWrappedMcObject() instanceof ITickerLevel) {
			ci.cancel();
		}
	}
}

package tfc.smallerunits.general_compat.mixin.distant_horizons;

import com.seibel.distanthorizons.forge.ForgeClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.simulation.level.ITickerLevel;

/**
 * When something happens to a small world, distant horizons should not acknowledge it
 * that is what this mixin does
 */
@Mixin(value = ForgeClientProxy.class, remap = false)
public class ForgeClientProxyMixin {
    @Inject(at = @At("HEAD"), method = "clientTickEvent", cancellable = true)
    public void preTick(TickEvent.ClientTickEvent event, CallbackInfo ci) {
        if (Minecraft.getInstance().level instanceof ITickerLevel) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "clientLevelLoadEvent", cancellable = true)
    public void preLoadWorld(LevelEvent.Load event, CallbackInfo ci) {
        if (event.getLevel() instanceof ITickerLevel) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "clientLevelUnloadEvent", cancellable = true)
    public void preUnloadWorld(LevelEvent.Unload event, CallbackInfo ci) {
        if (event.getLevel() instanceof ITickerLevel) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "clientChunkLoadEvent", cancellable = true)
    public void preChunkLoad(ChunkEvent.Load event, CallbackInfo ci) {
        if (event.getLevel() instanceof ITickerLevel) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "onBlockChangeEvent", cancellable = true)
    public void preBlockChange(LevelAccessor level, ChunkAccess chunk, CallbackInfo ci) {
        if (level instanceof ITickerLevel) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "leftClickBlockEvent", cancellable = true)
    public void preLeftClick(PlayerInteractEvent.LeftClickBlock event, CallbackInfo ci) {
        if (event.getLevel() instanceof ITickerLevel) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "rightClickBlockEvent", cancellable = true)
    public void preRightClick(PlayerInteractEvent.RightClickBlock event, CallbackInfo ci) {
        if (event.getLevel() instanceof ITickerLevel) {
            ci.cancel();
        }
    }
}

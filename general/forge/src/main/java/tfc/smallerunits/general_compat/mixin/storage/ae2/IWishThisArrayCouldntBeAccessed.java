package tfc.smallerunits.general_compat.mixin.storage.ae2;

import appeng.server.services.compass.CompassService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.simulation.chunk.BasicVerticalChunk;

@Mixin(value = CompassService.class, remap = false)
public class IWishThisArrayCouldntBeAccessed {
    @Inject(at = @At("HEAD"), method = "updateArea(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;)V", cancellable = true)
    private static void updateArea(ServerLevel level, ChunkAccess chunk, CallbackInfo ci) {
        if (chunk instanceof BasicVerticalChunk)
            ci.cancel();
    }
}

package tfc.smallerunits.flywheel.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.backend.BackendManager;
import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.client.abstraction.IFrustum;
import tfc.smallerunits.core.utils.asm.ModCompatClient;

@Mixin(value = ModCompatClient.class, remap = false)
public class CModCompatMixin {
    @Inject(at = @At("HEAD"), method = "drawBE", cancellable = true)
    private static <T extends BlockEntity> void preDrawBE(T be, BlockPos origin, IFrustum frustum, PoseStack stk, float tickDelta, CallbackInfo ci) {
        if (BackendManager.isBackendOn()) {
            BlockEntityVisualizer<? super T> visualizer = (BlockEntityVisualizer<? super T>) VisualizerRegistry.getVisualizer(be.getType());
            // if there is no visualizer, then the render shouldn't be skipped
            if (visualizer == null) return;

            // elsewise, skip if the visualizer wants to skip
            if (visualizer.skipVanillaRender(be)) ci.cancel();
        }
    }
}

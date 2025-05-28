package tfc.smallerunits.flywheel.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.event.RenderContextImpl;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.client.abstraction.IFrustum;
import tfc.smallerunits.core.utils.asm.ModCompatClient;

@Mixin(value = ModCompatClient.class, remap = false)
public class CModCompatMixin {
    @Inject(at = @At("HEAD"), method = "postRenderLayer")
    private static void afterRenderLayer(RenderType type, PoseStack poseStack, double camX, double camY, double camZ, Level level, CallbackInfo ci) {
//        for (Region value : ((RegionalAttachments) level).SU$getRegionMap().values()) {
//            BlockPos rp = value.pos.toBlockPos();
//            for (Level valueLevel : value.getLevels()) {
//                if (valueLevel != null) {
//                    poseStack.pushPose();
//                    poseStack.scale(
//                            1f / ((ITickerLevel) valueLevel).getUPB(),
//                            1f / ((ITickerLevel) valueLevel).getUPB(),
//                            1f / ((ITickerLevel) valueLevel).getUPB()
//                    );
//                    int mul = ((ITickerLevel) valueLevel).getUPB();
//                    RenderLayerEvent event = new RenderLayerEvent(
//                            (ClientLevel) valueLevel,
//                            type, poseStack, Minecraft.getInstance().renderBuffers(),
//                            (camX - rp.getX()) * mul, (camY - rp.getY()) * mul, (camZ - rp.getZ()) * mul
//                    );
//                    MinecraftForge.EVENT_BUS.post(event);
//                    poseStack.popPose();
//                }
//            }
//        }
        VisualizationManager manager = VisualizationManagerImpl.get(level);
        RenderContextImpl ctx = new RenderContextImpl(
                Minecraft.getInstance().levelRenderer,
                (ClientLevel) level,
                Minecraft.getInstance().renderBuffers(),
                poseStack,
                RenderSystem.getProjectionMatrix(),
                RenderSystem.getModelViewMatrix(),
                Minecraft.getInstance().getEntityRenderDispatcher().camera,
                0
        );

        manager.renderDispatcher().afterEntities(ctx);
    }

    @Inject(at = @At("HEAD"), method = "drawBE", cancellable = true)
    private static void preDrawBE(BlockEntity be, BlockPos origin, IFrustum frustum, PoseStack stk, float tickDelta, CallbackInfo ci) {
        VisualizationManager manager = VisualizationManagerImpl.get(be.getLevel());

//        if (Backend.isOn())
//            if (Backend.canUseInstancing(be.getLevel()))
//                if (InstancedRenderRegistry.canInstance(be.getType()))
//                    ci.cancel();
    }
}

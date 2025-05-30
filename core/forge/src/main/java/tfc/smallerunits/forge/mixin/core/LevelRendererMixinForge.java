package tfc.smallerunits.forge.mixin.core;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.client.render.TileRendererHelper;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.data.tracking.RegionalAttachments;
import tfc.smallerunits.core.simulation.level.client.AbstractTickerClientLevel;

@Mixin(LevelRenderer.class)
public class LevelRendererMixinForge {
	@Shadow @Final private RenderBuffers renderBuffers;
	
	@Shadow @Nullable public ClientLevel level;
	
	// ok that's a long injection target
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V"), method = "renderLevel")
	public void preRenderParticles(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, org.joml.Matrix4f pProjectionMatrix, CallbackInfo ci) {
		for (Region value : ((RegionalAttachments) level).SU$getRegionMap().values()) {
			// TODO: frustum checks
			for (Level valueLevel : value.getLevels()) {
				if (valueLevel != null) {
					if (valueLevel instanceof AbstractTickerClientLevel) {
						pPoseStack.pushPose();
						TileRendererHelper.drawParticles(pPoseStack, pPartialTick, pFinishNanoTime, pRenderBlockOutline, pCamera, pGameRenderer, pLightTexture, pProjectionMatrix, value, valueLevel, renderBuffers, ci);
						pPoseStack.popPose();
					}
				}
			}
		}
//		SphereBoxTesting.render(pPoseStack, pPartialTick, renderBuffers, level.getGameTime(), pProjectionMatrix);
	}
}

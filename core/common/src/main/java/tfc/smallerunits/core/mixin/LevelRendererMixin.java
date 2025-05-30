package tfc.smallerunits.core.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.client.render.SURenderManager;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.data.tracking.RegionalAttachments;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.core.utils.asm.AssortedQol;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Shadow
	public ClientLevel level;
	@Unique
	PoseStack stk;
	
	@Unique
	double pCamX, pCamY, pCamZ;
	
	@Inject(at = @At("HEAD"), method = "renderChunkLayer")
	public void preStartDraw(RenderType j, PoseStack d0, double d1, double d2, double i, Matrix4f k, CallbackInfo ci) {
		pCamX = d1;
		pCamY = d2;
		pCamZ = i;
	}
	
	@Shadow
	@Final
	private RenderBuffers renderBuffers;
	
	@Shadow
	private static native void renderShape(PoseStack pPoseStack, VertexConsumer pConsumer, VoxelShape pShape, double pX, double pY, double pZ, float pRed, float pGreen, float pBlue, float pAlpha);
	
	@Shadow
	public abstract void tick();
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;"), method = "renderLevel")
	public void beforeRenderEntities(PoseStack stack, float i, long j, boolean k, Camera l, GameRenderer i1, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
		for (Region value : ((RegionalAttachments) level).SU$getRegionMap().values()) {
			if (value == null) continue;
			value.forEachLevel((lvl) -> {
				renderEntities(lvl, stack, l, i, this.renderBuffers.bufferSource());
			});
		}
	}
	
	@Unique
	public void renderEntities(Level lvl, PoseStack stk, Camera cam, float pct, MultiBufferSource buffers) {
		Iterable<Entity> entities;
		if (lvl instanceof ClientLevel) entities = ((ClientLevel) lvl).entitiesForRendering();
		else entities = ((ITickerLevel) lvl).getAllEntities();
		stk.pushPose();
		stk.translate(
				-cam.getPosition().x,
				-cam.getPosition().y,
				-cam.getPosition().z
		);
		stk.scale(1f / ((ITickerLevel) lvl).getUPB(), 1f / ((ITickerLevel) lvl).getUPB(), 1f / ((ITickerLevel) lvl).getUPB());
		for (Entity entity : entities)
			SURenderManager.drawEntity((LevelRenderer) (Object) this, lvl, stk, cam, pct, buffers, entity);
		stk.popPose();
	}
	
	@Inject(at = @At("HEAD"), method = "renderHitOutline", cancellable = true)
	public void preRenderOutline(PoseStack pPoseStack, VertexConsumer pConsumer, Entity pEntity, double pCamX, double pCamY, double pCamZ, BlockPos pPos, BlockState pState, CallbackInfo ci) {
		AssortedQol.handleRenderOutline((shape1) -> {
			renderShape(
					pPoseStack,
					pConsumer,
					shape1,
					0, 0, 0,
					0.0F, 0.0F, 0.0F, 0.4F
			);
		}, this.level, pPoseStack, pConsumer, pEntity, pCamX, pCamY, pCamZ, pPos, pState, ci);
	}
	
	@Inject(at = @At("HEAD"), method = "checkPoseStack")
	public void preCheckMatrices(PoseStack pPoseStack, CallbackInfo ci) {
		stk = pPoseStack;
	}
}

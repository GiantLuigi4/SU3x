package tfc.smallerunits.core.mixin;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.client.abstraction.VanillaFrustum;
import tfc.smallerunits.core.client.access.tracking.SUCapableChunk;
import tfc.smallerunits.core.client.access.tracking.SUCompiledChunkAttachments;
import tfc.smallerunits.core.client.render.SURenderManager;
import tfc.smallerunits.core.client.render.TileRendererHelper;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.core.data.capability.SUCapabilityManager;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.core.utils.BreakData;
import tfc.smallerunits.core.utils.IHateTheDistCleaner;
import tfc.smallerunits.core.utils.asm.AssortedQol;
import tfc.smallerunits.core.utils.asm.ModCompatClient;
import tfc.smallerunits.plat.internal.ToolProvider;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixinBlocks {
	@Shadow
	public ClientLevel level;
	@Unique
	PoseStack stk;
	@Shadow
	@Final
	private BlockEntityRenderDispatcher blockEntityRenderDispatcher;

	@Shadow
	private @Nullable Frustum capturedFrustum;
	@Shadow
	private Frustum cullingFrustum;
	@Shadow
	@Final
	private RenderBuffers renderBuffers;
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	protected abstract void renderChunkLayer(RenderType p_172994_, PoseStack p_172995_, double p_172996_, double p_172997_, double p_172998_, Matrix4f p_172999_);

	@Shadow @Final private ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum;
	@Unique
	double pCamX, pCamY, pCamZ;

	@Inject(at = @At("HEAD"), method = "renderChunkLayer")
	public void preStartDraw(RenderType j, PoseStack d0, double d1, double d2, double i, Matrix4f k, CallbackInfo ci) {
		pCamX = d1;
		pCamY = d2;
		pCamZ = i;
	}

	@Unique
	VanillaFrustum SU$Frustum = new VanillaFrustum();
	@Unique
	float pct;

	@Inject(at = @At("HEAD"), method = "renderLevel")
	public void preDrawLevel(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci) {
		pct = pPartialTick;
	}

	@Inject(at = @At("HEAD"), method = "checkPoseStack")
	public void preCheckMatrices(PoseStack pPoseStack, CallbackInfo ci) {
		stk = pPoseStack;
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;getCompiledChunk()Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$CompiledChunk;"))
	public ChunkRenderDispatcher.CompiledChunk preGetCompiledChunk(ChunkRenderDispatcher.RenderChunk instance) {
		BlockPos origin = instance.getOrigin();
		ChunkRenderDispatcher.CompiledChunk chunk = instance.compiled.get();
		SUCapableChunk capable = ((SUCompiledChunkAttachments) chunk).getSUCapable();

		if (capable == null)
			((SUCompiledChunkAttachments) chunk).setSUCapable(origin.getY(), capable = ((SUCapableChunk) level.getChunk(origin)));

		ISUCapability capability = SUCapabilityManager.getCapability((LevelChunk) capable);
		if (capability == null) return instance.getCompiledChunk();

		UnitSpace[] spaces = capability.getUnits();
		// no reason to do SU related rendering in chunks where SU has not been used
		if (spaces.length == 0) return instance.getCompiledChunk();

		Frustum frustum = capturedFrustum != null ? capturedFrustum : cullingFrustum;
		SU$Frustum.set(frustum);

		stk.pushPose();
		Vec3 cam = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
		stk.translate(origin.getX() - cam.x, origin.getY() - cam.y, origin.getZ() - cam.z);

		/* draw indicators */
		RenderType.solid().setupRenderState();
		ShaderInstance shader = GameRenderer.getPositionColorShader();
		shader.apply();
		RenderSystem.setShader(() -> shader);
		BufferUploader.reset();
		RenderSystem.setupShaderLights(shader);
		if (shader.PROJECTION_MATRIX != null) {
			shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
			shader.PROJECTION_MATRIX.upload();
		}
		TileRendererHelper.markNewFrame();

		boolean hammerHeld = IHateTheDistCleaner.isHammerHeld();
		for (UnitSpace unit : spaces) {
			if (AssortedQol.isInSection(unit, origin)) {
				if (unit != null) {
					TileRendererHelper.drawUnit(
							SU$Frustum,
							unit.pos, unit.unitsPerBlock, unit.isNatural,
							hammerHeld, unit.isEmpty(), null, stk,
//							LightTexture.pack(level.getBrightness(LightLayer.BLOCK, unit.pos), level.getBrightness(LightLayer.SKY, unit.pos)),
							LightTexture.pack(0, 0),
							origin.getX(), origin.getY(), origin.getZ()
					);
				}
			}
		}
		for (UnitSpace space : spaces) {
			if (AssortedQol.isInSection(space, origin)) {
				stk.pushPose();
				AssortedQol.setupMatrix(space, stk);
				for (UnitSpace[] nestedSpace : space.getPotentiallyNestedSpaces()) {
					for (UnitSpace unitSpace : nestedSpace) {
						if (space.contains(unitSpace)) {
							AssortedQol.drawIndicatorsRecursive(unitSpace, origin, hammerHeld, stk, SU$Frustum);
						}
					}
				}
				stk.popPose();
			}
		}

		shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
		shader.COLOR_MODULATOR.upload();

		VertexBuffer.unbind();
		shader.clear();
		RenderType.solid().clearRenderState();

		/* breaking overlays */
		for (UnitSpace unit : capability.getUnits()) {
			if (unit != null) {
				ITickerLevel world = (ITickerLevel) unit.getMyLevel();
				for (BreakData integer : world.getBreakData().values()) {
					BlockPos minPos = unit.getOffsetPos(new BlockPos(0, 0, 0));
					BlockPos maxPos = unit.getOffsetPos(new BlockPos(unit.unitsPerBlock, unit.unitsPerBlock, unit.unitsPerBlock));
					BlockPos posInQuestion = integer.pos;
					if (
							maxPos.getX() > posInQuestion.getX() && posInQuestion.getX() >= minPos.getX() &&
									maxPos.getY() > posInQuestion.getY() && posInQuestion.getY() >= minPos.getY() &&
									maxPos.getZ() > posInQuestion.getZ() && posInQuestion.getZ() >= minPos.getZ()
					)
						TileRendererHelper.drawBreakingOutline(integer.prog, renderBuffers, stk, unit.getMyLevel(), integer.pos, ((Level) world).getBlockState(integer.pos), minecraft);
				}
			}
		}

		((SUCompiledChunkAttachments) chunk).SU$getChunkRender().drawBEs(origin, stk, SU$Frustum, pct);
		stk.popPose();

		return instance.getCompiledChunk();
	}

	@Redirect(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$CompiledChunk;isEmpty(Lnet/minecraft/client/renderer/RenderType;)Z"))
	public boolean preDrawLayer(ChunkRenderDispatcher.CompiledChunk instance, RenderType pRenderType) {
		ShaderInstance shaderinstance = RenderSystem.getShader();
		Uniform uniform = shaderinstance.CHUNK_OFFSET;

		BlockPos origin = ToolProvider.currentRenderChunk.get().getOrigin();
		ChunkRenderDispatcher.CompiledChunk chunk = ToolProvider.currentRenderChunk.get().compiled.get();
		SUCapableChunk capable = ((SUCompiledChunkAttachments) chunk).getSUCapable();

		if (capable == null)
			((SUCompiledChunkAttachments) chunk).setSUCapable(origin.getY(), capable = ((SUCapableChunk) level.getChunk(origin)));

//		if (!capable.SU$getChunkRender().hasBuffers()) return instance.isEmpty(pRenderType);

		if (uniform != null)
			uniform.set((float) ((double) origin.getX() - pCamX), (float) ((double) origin.getY() - pCamY), (float) ((double) origin.getZ() - pCamZ));

		SU$Frustum.set(capturedFrustum != null ? capturedFrustum : cullingFrustum);
		SURenderManager.drawChunk(((SUCompiledChunkAttachments) chunk), ((LevelChunk) capable), level, ToolProvider.currentRenderChunk.get().getOrigin(), pRenderType, SU$Frustum, pCamX, pCamY, pCamZ, uniform);
		return instance.isEmpty(pRenderType);
	}

	@Inject(at = @At("TAIL"), method = "applyFrustum")
	private void postApply(Frustum $$0, CallbackInfo ci) {
		for (LevelRenderer.RenderChunkInfo renderChunkInfo : renderChunksInFrustum) {
			ChunkRenderDispatcher.RenderChunk rc = renderChunkInfo.chunk;
			SUCompiledChunkAttachments cc = ((SUCompiledChunkAttachments) rc.getCompiledChunk());
			cc.markForCull();
		}
	}

	@Inject(at = @At("TAIL"), method = "renderChunkLayer")
	public void postRenderLayer(RenderType renderType, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix, CallbackInfo ci) {
		ModCompatClient.postRenderLayer(renderType, poseStack, camX, camY, camZ, level);
	}
}

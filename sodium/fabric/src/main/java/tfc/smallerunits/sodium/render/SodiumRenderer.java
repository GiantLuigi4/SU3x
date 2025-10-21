package tfc.smallerunits.sodium.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.util.iterator.ByteIterator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.common.logging.Loggers;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.client.abstraction.IFrustum;
import tfc.smallerunits.core.client.access.tracking.SUCapableChunk;
import tfc.smallerunits.core.client.access.tracking.SUCompiledChunkAttachments;
import tfc.smallerunits.core.client.render.SUChunkRender;
import tfc.smallerunits.core.client.render.SURenderManager;
import tfc.smallerunits.core.client.render.TileRendererHelper;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.core.data.capability.SUCapabilityManager;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.core.utils.BreakData;
import tfc.smallerunits.core.utils.IHateTheDistCleaner;
import tfc.smallerunits.core.utils.asm.ModCompatClient;
import tfc.smallerunits.sodium.ChunkBuildResults;
import tfc.smallerunits.sodium.RenderListAttachments;

import java.util.Iterator;
import java.util.SortedSet;

public class SodiumRenderer {
	public static void render(RenderType type, PoseStack poseStack, double camX, double camY, double camZ, CallbackInfo ci, SodiumFrustum frustum, Minecraft client, ClientLevel level, RenderSectionManager renderSectionManager) {
		if (SodiumRenderMode.VANILLA == SodiumRenderMode.VANILLA) {
			renderVanilla(type, frustum, level, poseStack, camX, camY, camZ, renderSectionManager);
		} else {
			throw new RuntimeException("Sodium renderer not implemented yet");
		}
		
		ModCompatClient.postRenderLayer(type, poseStack, camX, camY, camZ, level);
	}
	
	public static void renderVanilla(RenderType type, IFrustum su$Frustum, ClientLevel level, PoseStack poseStack, double camX, double camY, double camZ, RenderSectionManager renderSectionManager) {
		type.setupRenderState();
		
		ShaderInstance instance = RenderSystem.getShader();
		// I don't want to know
		instance.setSampler("Sampler0", RenderSystem.getShaderTexture(0));
		instance.setSampler("Sampler2", RenderSystem.getShaderTexture(2));
		if (instance.MODEL_VIEW_MATRIX != null) instance.MODEL_VIEW_MATRIX.set(poseStack.last().pose());
		if (instance.PROJECTION_MATRIX != null) instance.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
		instance.apply();
		if (instance.MODEL_VIEW_MATRIX != null) instance.MODEL_VIEW_MATRIX.upload();
		if (instance.PROJECTION_MATRIX != null) instance.PROJECTION_MATRIX.upload();
		
		Uniform uniform = instance.CHUNK_OFFSET;
		
		SortedRenderLists renderLists = renderSectionManager.getRenderLists();
		Iterator<ChunkRenderList> renderListIterator = renderLists.iterator();
		renderListIterator.forEachRemaining(renderList -> {
			ByteIterator iterator = ((RenderListAttachments) renderList).smallerUnits$sectionsWithUnitSpacesIterator(false);
			if (iterator == null) return;
			
			while (iterator.hasNext()) {
				int element = iterator.nextByteAsInt();
				RenderSection section = renderList.getRegion().getSection(element);
				
				int y = section.getChunkY();
				
				SUCapableChunk capable = ((ChunkBuildResults) section).getCapable();
				SUCompiledChunkAttachments attachments = (SUCompiledChunkAttachments) section;
				SUChunkRender render = attachments.SU$getChunkRender();
				if (render == null) {
					attachments.setSUCapable(y, capable);
					render = attachments.SU$getChunkRender();
				}
				
				LevelChunk chunk = (LevelChunk) capable;
				
				BlockPos pos = new BlockPos(
						chunk.getPos().getMinBlockX(),
						SectionPos.sectionToBlockCoord(y),
						chunk.getPos().getMinBlockZ()
				);
				
				instance.CHUNK_OFFSET.set(
						(float) (pos.getX() - camX),
						(float) (pos.getY() - camY),
						(float) (pos.getZ() - camZ)
				);
				
				for (UnitSpace space : ((ChunkBuildResults) section).smallerUnits$getAll()) {
					SURenderManager.drawChunk(
							render, attachments,
							chunk, level, space.pos,
							type, su$Frustum,
							camX, camY, camZ,
							uniform
					);
				}
			}
		});
		
		instance.CHUNK_OFFSET.set(0f, 0, 0);
		
		instance.setSampler("Sampler0", null);
		instance.setSampler("Sampler2", null);
		instance.clear();
		type.clearRenderState();
	}
	
	public static void renderSection(BlockPos origin, RenderSection instance, PoseStack stk, RenderBuffers bufferBuilders, Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions, float tickDelta, CallbackInfo ci, SodiumFrustum frustum, Minecraft client, ClientLevel level) {
		SUCapableChunk capable = ((SUCompiledChunkAttachments) instance).getSUCapable();
		
		if (capable == null)
			((SUCompiledChunkAttachments) instance).setSUCapable(origin.getY(), capable = ((SUCapableChunk) level.getChunk(origin)));
		
		ISUCapability capability = SUCapabilityManager.getCapability((LevelChunk) capable);
		if (capability == null) {
			Loggers.SU_LOGGER.debug("Null capability received?");
			Loggers.SU_LOGGER.debug(capable.toString());
			Loggers.SU_LOGGER.debug(instance.toString());
			Loggers.SU_LOGGER.debug(origin.toString());
			return;
		}
		
		UnitSpace[] spaces = capability.getUnits();
		// no reason to do SU related rendering in chunks where SU has not been used
		if (spaces.length == 0) return;
		
		stk.pushPose();
		stk.translate(origin.getX(), origin.getY(), origin.getZ());
		
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
			int y = unit.pos.getY();
			
			if (y < origin.getY() + 16 &&
					y >= origin.getY()) {
				if (unit != null) {
					TileRendererHelper.drawUnit(
							frustum,
							unit.pos, unit.unitsPerBlock, unit.isNatural,
							hammerHeld, unit.isEmpty(), null, stk,
							LightTexture.pack(0, 0),
							origin.getX(), origin.getY(), origin.getZ()
					);
				}
			}
		}
		
		if (shader.COLOR_MODULATOR != null) {
			shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
			shader.COLOR_MODULATOR.upload();
		}
		
		VertexBuffer.unbind();
		shader.clear();
		RenderType.solid().clearRenderState();
		
		/* breaking overlays */
		for (UnitSpace unit : capability.getUnits()) {
			if (unit != null) {
				ITickerLevel world = (ITickerLevel) unit.getMyLevel();
				if (world != null) {
					for (BreakData integer : world.getBreakData().values()) {
						BlockPos minPos = unit.getOffsetPos(new BlockPos(0, 0, 0));
						BlockPos maxPos = unit.getOffsetPos(new BlockPos(unit.unitsPerBlock, unit.unitsPerBlock, unit.unitsPerBlock));
						BlockPos posInQuestion = integer.pos;
						if (
								maxPos.getX() > posInQuestion.getX() && posInQuestion.getX() >= minPos.getX() &&
										maxPos.getY() > posInQuestion.getY() && posInQuestion.getY() >= minPos.getY() &&
										maxPos.getZ() > posInQuestion.getZ() && posInQuestion.getZ() >= minPos.getZ()
						)
							TileRendererHelper.drawBreakingOutline(integer.prog, bufferBuilders, stk, unit.getMyLevel(), integer.pos, ((Level) world).getBlockState(integer.pos), client);
					}
				}
			}
		}
		stk.popPose();
		
		SUCompiledChunkAttachments attachments = (SUCompiledChunkAttachments) instance;
		SUChunkRender render = attachments.SU$getChunkRender();
		
		if (render == null) {
			attachments.setSUCapable(instance.getChunkY(), capable);
			render = attachments.SU$getChunkRender();
		}
		
		render.drawBEs(
				origin, stk,
				frustum, tickDelta,
				true
		);
	}
	
	public static void renderTEs(PoseStack matrices, RenderBuffers bufferBuilders, Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions, Camera camera, float tickDelta, CallbackInfo ci, SodiumFrustum frustum, Minecraft client, ClientLevel level, RenderSectionManager renderSectionManager) {
		BlockPos.MutableBlockPos origin = new BlockPos.MutableBlockPos();
		Iterator<ChunkRenderList> lists = renderSectionManager.getRenderLists().iterator(false);
		PoseStack stk = matrices;
		
		stk.pushPose();
		stk.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
		
		SortedRenderLists renderLists = renderSectionManager.getRenderLists();
		Iterator<ChunkRenderList> renderListIterator = renderLists.iterator();
		renderListIterator.forEachRemaining(renderList -> {
			ByteIterator iterator = ((RenderListAttachments) renderList).smallerUnits$sectionsWithUnitSpacesIterator(false);
			if (iterator == null) return;
			
			while (iterator.hasNext()) {
				int element = iterator.nextByteAsInt();
				RenderSection section = renderList.getRegion().getSection(element);
				
				origin.set(section.getChunkX() << 4, section.getChunkY() << 4, section.getChunkZ() << 4);
				
				renderSection(
						origin, section, stk,
						bufferBuilders, blockBreakingProgressions,
						tickDelta, ci, frustum,
						client, level
				);
			}
		});
		stk.popPose();
	}
}

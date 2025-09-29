package tfc.smallerunits.sodium.mixin;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.util.task.CancellationToken;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import tfc.smallerunits.core.Registry;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.client.access.tracking.FastCapabilityHandler;
import tfc.smallerunits.core.client.access.tracking.SUCapableChunk;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.sodium.ChunkBuildResults;
import tfc.smallerunits.sodium.WorldSliceAccessor;

@Mixin(value = ChunkBuilderMeshingTask.class)
public class ChunkBuilderMeshingTaskMixin {
	@Shadow
	@Final
	private ChunkRenderContext renderContext;
	@Shadow
	@Final
	private RenderSection render;
	@Unique
	private LevelChunk __chunk;
	@Unique
	private ISUCapability __capability;
	@Unique
	private ChunkBuildResults __results;
	@Unique
	private Block SPACE; // avoid lambda overhead
	
	@SuppressWarnings({
			"RedundantSuppression",
			"InvalidInjectorMethodSignature",
			"UnresolvedMixinReference"
	})
	@Inject(
			method = "execute(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lme/jellysquid/mods/sodium/client/util/task/CancellationToken;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
			at = @At(
					value = "INVOKE",
					target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;<init>(Lme/jellysquid/mods/sodium/client/world/WorldSlice;)V"
			),
			locals = LocalCapture.CAPTURE_FAILHARD,
			remap = false
	)
	public void preExecute(
			ChunkBuildContext buildContext, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir,
			BuiltSectionInfo.Builder renderData, VisGraph occluder, ChunkBuildBuffers buffers, BlockRenderCache cache, WorldSlice slice,
			int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
			BlockPos.MutableBlockPos blockPos, BlockPos.MutableBlockPos modelOffset
	) {
		__chunk = ((WorldSliceAccessor) (Object) cache.getWorldSlice()).getLevel().getChunk(
				render.getChunkX(),
				render.getChunkZ()
		);
		__capability = ((FastCapabilityHandler) __chunk).getSUCapability();
		
		__results = ((ChunkBuildResults) renderData);
		__results.smallerUnits$setCapability(__capability);
		__results.setCapable((SUCapableChunk) __chunk);
		
		SPACE = Registry.UNIT_SPACE.get();
	}
	
	// shut up the mcdev plugin
	@SuppressWarnings({
			"RedundantSuppression",
			"InvalidInjectorMethodSignature",
			"UnresolvedMixinReference"
	})
	@Inject(
			method = "execute(Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lme/jellysquid/mods/sodium/client/util/task/CancellationToken;)Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"
			),
			locals = LocalCapture.CAPTURE_FAILHARD
	)
	public void addUnitSpace(
			ChunkBuildContext buildContext, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir,
			BuiltSectionInfo.Builder renderData, VisGraph occluder, ChunkBuildBuffers buffers, BlockRenderCache cache, WorldSlice slice,
			int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
			BlockPos.MutableBlockPos blockPos, BlockPos.MutableBlockPos modelOffset,
			BlockRenderContext context,
			int y, int z, int x,
			BlockState blockState
	) {
		if (blockState.is(SPACE)) {
			UnitSpace space = __capability.getUnit(blockPos);
			if (space != null) {
				__results.smallerUnits$addUnitSpace(space);
			}
		}
	}
}
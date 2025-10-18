package tfc.smallerunits.sodium.mixin;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import tfc.smallerunits.core.client.access.tracking.SUCapableChunk;
import tfc.smallerunits.core.client.access.tracking.SUCompiledChunkAttachments;
import tfc.smallerunits.core.client.render.SUChunkRender;
import tfc.smallerunits.sodium.ChunkBuildResults;

@Mixin(value = RenderSection.class)
public class RenderSectionMixin1 implements SUCompiledChunkAttachments {
	@Unique
	private SUChunkRender compChunk;
	
	@Override
	public SUCapableChunk getSUCapable() {
		return ((ChunkBuildResults) this).getCapable();
	}
	
	@Override
	public void setSUCapable(int yCoord, SUCapableChunk chunk) {
		compChunk = getSUCapable().SU$getRenderer(yCoord);
	}
	
	@Override
	public void markForCull() {
		throw new RuntimeException("TODO");
	}
	
	@Override
	public boolean needsCull() {
		return true;
	}
	
	public void markCulled()  {
		throw new RuntimeException("TODO");
	}
	
	@Override
	public SUChunkRender SU$getChunkRender() {
		return compChunk;
	}
}
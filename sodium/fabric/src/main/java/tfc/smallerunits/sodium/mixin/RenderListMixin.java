package tfc.smallerunits.sodium.mixin;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.util.iterator.ByteIterator;
import me.jellysquid.mods.sodium.client.util.iterator.ReversibleByteArrayIterator;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.sodium.RenderListAttachments;
import tfc.smallerunits.sodium.RenderSectionAttachments;

@Mixin(value = ChunkRenderList.class)
public class RenderListMixin implements RenderListAttachments {
	@Unique
	private final byte[] sectionsWithUnitSpaces = new byte[256];
	@Unique
	private int sectionsWithUnitSpacesCount = 0;
	
	public @Nullable ByteIterator smallerUnits$sectionsWithUnitSpacesIterator(boolean reverse) {
		return this.sectionsWithUnitSpacesCount == 0 ? null : new ReversibleByteArrayIterator(this.sectionsWithUnitSpaces, this.sectionsWithUnitSpacesCount, reverse);
	}
	
	@Inject(at = @At("TAIL"), method = "add", remap = false)
	public void postAdd(RenderSection render, CallbackInfo ci) {
		this.sectionsWithUnitSpaces[this.sectionsWithUnitSpacesCount] = (byte)render.getSectionIndex();
		this.sectionsWithUnitSpacesCount += ((RenderSectionAttachments) render).smallerUnits$hasUnitSpaces() ? 1 : 0;
	}
	
	@Inject(at = @At("TAIL"), method = "reset", remap = false)
	public void postRest(int frame, CallbackInfo ci) {
		sectionsWithUnitSpacesCount = 0;
	}
}
package tfc.smallerunits.sodium.mixin;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.client.access.tracking.SUCapableChunk;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.sodium.ChunkBuildResults;
import tfc.smallerunits.sodium.RenderSectionAttachments;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = RenderSection.class)
public class RenderSectionMixin implements ChunkBuildResults, RenderSectionAttachments {
	@Shadow
	private int flags;
	@Unique
	public List<UnitSpace> spaces = new ArrayList<>();
	@Unique
	public ISUCapability capability;
	@Unique
	public SUCapableChunk chunk;
	
	@Override
	public void smallerUnits$addUnitSpace(UnitSpace be) {
		throw new RuntimeException("Unsupported");
	}
	
	@Override
	public List<UnitSpace> smallerUnits$getAll() {
		return spaces;
	}
	
	@Override
	public void smallerUnits$addAll(List<UnitSpace> all) {
		this.spaces = all;
	}
	
	@Override
	public void smallerUnits$setCapability(ISUCapability capability) {
		this.capability = capability;
	}
	
	public boolean smallerUnits$hasUnitSpaces() {
		return !spaces.isEmpty();
	}
	
	@Inject(at = @At("RETURN"), method = "setRenderState", remap = false)
	public void postSetRenderState(BuiltSectionInfo info, CallbackInfo ci) {
		spaces = ((ChunkBuildResults) info).smallerUnits$getAll();
		capability = ((ChunkBuildResults) info).smallerUnits$getCapability();
		chunk = ((ChunkBuildResults) info).getCapable();
		if (!spaces.isEmpty()) {
			flags |= -1;
		}
	}
	
	@Override
	public ISUCapability smallerUnits$getCapability() {
		return capability;
	}
	
	@Override
	public SUCapableChunk getCapable() {
		return chunk;
	}
	
	@Override
	public void setCapable(SUCapableChunk chunk) {
		this.chunk = chunk;
	}
}
package tfc.smallerunits.sodium.mixin;

import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.client.access.tracking.SUCapableChunk;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.sodium.ChunkBuildResults;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = BuiltSectionInfo.Builder.class)
public class SectionBuilderMixin implements ChunkBuildResults {
	@Unique
	List<UnitSpace> spaces = new ArrayList<>();
	@Unique
	public ISUCapability capability;
	@Unique
	public SUCapableChunk chunk;
	
	@Override
	public void smallerUnits$addUnitSpace(UnitSpace be) {
		spaces.add(be);
	}
	
	@Override
	public List<UnitSpace> smallerUnits$getAll() {
		return spaces;
	}
	
	@Override
	public void smallerUnits$addAll(List<UnitSpace> all) {
		spaces.addAll(all);
	}
	
	@Inject(at = @At("RETURN"), method = "build", remap = false)
	public void postBuild(CallbackInfoReturnable<BuiltSectionInfo> cir) {
		((ChunkBuildResults) cir.getReturnValue()).smallerUnits$addAll(spaces);
		((ChunkBuildResults) cir.getReturnValue()).smallerUnits$setCapability(capability);
		((ChunkBuildResults) cir.getReturnValue()).setCapable(chunk);
	}
	
	@Override
	public void smallerUnits$setCapability(ISUCapability capability) {
		this.capability = capability;
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
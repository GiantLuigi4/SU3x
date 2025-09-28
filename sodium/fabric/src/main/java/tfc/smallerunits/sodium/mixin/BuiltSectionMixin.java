package tfc.smallerunits.sodium.mixin;

import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.client.access.tracking.SUCapableChunk;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.sodium.ChunkBuildResults;

import java.util.ArrayList;
import java.util.List;

@Mixin(BuiltSectionInfo.class)
public class BuiltSectionMixin implements ChunkBuildResults {
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
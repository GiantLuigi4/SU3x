package tfc.smallerunits.sodium;

import org.jetbrains.annotations.ApiStatus;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.client.access.tracking.SUCapableChunk;
import tfc.smallerunits.core.data.capability.ISUCapability;

import java.util.List;

/**
 * Idk how you would end up using this, lol
 */
@ApiStatus.Internal
@Deprecated(forRemoval = false)
public interface ChunkBuildResults {
	void smallerUnits$addUnitSpace(UnitSpace be);
	
	List<UnitSpace> smallerUnits$getAll();
	
	void smallerUnits$addAll(List<UnitSpace> all);
	
	void smallerUnits$setCapability(ISUCapability capability);
	
	ISUCapability smallerUnits$getCapability();
	
	SUCapableChunk getCapable();
	void setCapable(SUCapableChunk chunk);
}

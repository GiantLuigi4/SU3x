package tfc.smallerunits.core.data.access;

import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;

public interface DimensionDataStorageAccessor {
	Map<String, SavedData> getStorage();
}

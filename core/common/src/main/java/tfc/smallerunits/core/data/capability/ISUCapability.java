package tfc.smallerunits.core.data.capability;

import net.minecraft.core.BlockPos;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.plat.itf.CapabilityLike;

public interface ISUCapability extends CapabilityLike {
	void removeUnit(BlockPos pos);
	
	void makeUnit(BlockPos pos);
	
	UnitSpace getOrMakeUnit(BlockPos pos);
	
	UnitSpace[] getUnits();
	
	void setUnit(BlockPos realPos, UnitSpace space);
	
	UnitSpace getUnit(BlockPos pos);
}

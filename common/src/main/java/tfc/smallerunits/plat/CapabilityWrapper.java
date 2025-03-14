package tfc.smallerunits.plat;

import net.minecraft.nbt.CompoundTag;

public abstract class CapabilityWrapper {
	public void deserializeNBT(CompoundTag capabilities) {
		throw new RuntimeException();
	}
	
	public CompoundTag serializeNBT() {
		throw new RuntimeException();
	}
}

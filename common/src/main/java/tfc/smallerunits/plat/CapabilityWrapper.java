package tfc.smallerunits.plat;

import net.minecraft.nbt.CompoundTag;

public abstract class CapabilityWrapper {
    public static CapabilityWrapper of(Object capabilities) {
		throw new RuntimeException("Check self-impl mixins for corresponding platform");
    }

    public void deserializeNBT(CompoundTag capabilities) {
		throw new RuntimeException();
	}
	
	public CompoundTag serializeNBT() {
		throw new RuntimeException();
	}
}

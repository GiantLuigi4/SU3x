package tfc.smallerunits.plat;

import net.minecraft.nbt.CompoundTag;

public abstract class CapabilityWrapper {
    public static CapabilityWrapper of(Object capabilities) {
		throw new RuntimeException("Check self-impl mixins for corresponding platform");
    }

    public abstract void deserializeNBT(CompoundTag capabilities);
	
	public abstract CompoundTag serializeNBT();
	
	public abstract boolean isInvalid();
}

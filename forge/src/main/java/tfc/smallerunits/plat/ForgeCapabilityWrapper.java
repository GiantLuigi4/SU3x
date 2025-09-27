package tfc.smallerunits.plat;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;

public class ForgeCapabilityWrapper extends CapabilityWrapper {
	CapabilityDispatcher dispatcher;
	
	public ForgeCapabilityWrapper(CapabilityDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}
	
	public void deserializeNBT(CompoundTag capabilities) {
		dispatcher.deserializeNBT(capabilities);
	}
	
	public CompoundTag serializeNBT() {
		return dispatcher.serializeNBT();
	}
	
	@Override
	public boolean isInvalid() {
		return dispatcher == null;
	}
}

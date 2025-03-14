package tfc.smallerunits.plat.config.annoconfg;

import tfc.smallerunits.plat.config.annoconfg.handle.UnsafeHandle;

import java.util.function.Supplier;

public class ConfigEntry {
	UnsafeHandle handle;
	Supplier<?> supplier;
	
	public ConfigEntry(UnsafeHandle handle, Supplier<?> supplier) {
		this.handle = handle;
		this.supplier = supplier;
	}
	
	public void update() {
		handle.set(supplier.get());
	}
}

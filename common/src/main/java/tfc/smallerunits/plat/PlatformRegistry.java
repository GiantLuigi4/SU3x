package tfc.smallerunits.plat;

import java.util.function.Supplier;

public abstract class PlatformRegistry<T> {
	public static <T> PlatformRegistry<T> makeRegistry(Class<T> cls, String modid) {
		throw new RuntimeException("Check platform self_impl mixins");
	}

	protected PlatformRegistry(Class<T> cls, String modid) {
	}

	public abstract void register();
	
	public abstract Supplier<T> register(String name, Supplier<T> value);
}

package tfc.smallerunits.core.utils.config.compat;

import tfc.smallerunits.core.client.render.compat.sodium.SodiumRenderMode;
import tfc.smallerunits.plat.config.annoconfg.ConfigSide;
import tfc.smallerunits.plat.config.annoconfg.annotation.format.*;
import tfc.smallerunits.plat.config.annoconfg.annotation.value.Default;

@Config(type = ConfigSide.CLIENT, namespace = "smallerunits", extra = "compat")
public class ClientCompatConfig {
	private static boolean getFalse() {
		return false;
	}
	
//	private static final AnnoCFG CFG = new AnnoCFG(FMLJavaModLoadingContext.get().getModEventBus(), ClientCompatConfig.class);
	
	protected static int get(int v) {
		return v;
	}
	
	protected static double get(double v) {
		return v;
	}
	
	public static <T extends Enum<T>> T get(T value) {
		return value;
	}
	
	@Comment(
			"Compat options for rendering mods"
	)
	@CFGSegment("rendering")
	public static class RenderCompatOptions {
		@Skip // TODO: implement this option
		@Name("sodium_render_mode")
		@Comment(
				{
						"What render mode should be used for sodium",
						"Choices:",
						"vanilla: a renderer which uses vanilla rendering to draw small chunks (slow)",
						"sodium: a renderer which uses multidraw (NYI, fast)"
				}
		)
		@Translation("config.smaller_units.sodium_render_mode")
		@Default(valueI = 0) // integer value representing the ordinal for the enum
		public static final SodiumRenderMode sodiumRenderMode = get(SodiumRenderMode.VANILLA);
	}
	
	public static void init() {
	}
}

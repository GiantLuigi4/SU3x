package tfc.smallerunits.core.utils.config;

import tfc.smallerunits.plat.config.AnnoCFG;
import tfc.smallerunits.plat.config.annoconfg.ConfigSide;
import tfc.smallerunits.plat.config.annoconfg.annotation.format.*;
import tfc.smallerunits.plat.config.annoconfg.annotation.value.Default;

@Config(type = ConfigSide.CLIENT, namespace = "smallerunits")
public class ClientConfig {
	private static boolean getFalse() {
		return false;
	}
	
	private static final AnnoCFG CFG = AnnoCFG.of(ClientConfig.class);
	
	protected static int get(int v) {
		return v;
	}
	
	protected static double get(double v) {
		return v;
	}
	
	@Comment(
			"Debug options for SU"
	)
	@CFGSegment("debug")
	public static class DebugOptions {
		@Name("fast_f3")
		@Comment("Whether or not F3 should use a small sample or a full sample\nSetting this to true makes F3 perform better but not necessarily be as accurate")
		@Translation("config.smaller_units.fast_f3")
		@Default(valueBoolean = true)
		public static final boolean fastF3 = !getFalse();
	}
	
	public static void init() {
	}
}

package tfc.smallerunits.resizing;

import net.fabricmc.api.ModInitializer;

public class ResizingMod implements ModInitializer {
    public ResizingMod() {
    }
	
	@Override
	public void onInitialize() {
		ResizingSetup.init();
	}
}

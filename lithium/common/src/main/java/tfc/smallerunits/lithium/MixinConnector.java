package tfc.smallerunits.lithium;

import tfc.smallerunits.plat.asm.BasePlugin;

public class MixinConnector extends BasePlugin {
    public MixinConnector() {
        pkgLookup.add("tfc.smallerunits.lithium.");
    }
}

package tfc.smallerunits.general_compat;

import tfc.smallerunits.plat.asm.BasePlugin;

public class MixinConnector extends BasePlugin {
    public MixinConnector() {
        pkgLookup.add("tfc.smallerunits.general_compat.mixin.");
    }
}

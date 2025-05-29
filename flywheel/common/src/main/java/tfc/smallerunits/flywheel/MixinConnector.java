package tfc.smallerunits.flywheel;

import tfc.smallerunits.plat.asm.BasePlugin;

public class MixinConnector extends BasePlugin {
    public MixinConnector() {
        pkgLookup.add("tfc.smallerunits.flywheel.mixin.");
        dependencies.put("tfc.smallerunits.flywheel.mixin.RegionMixin", "dev.engine_room.flywheel.api.visualization.VisualEmbedding");
        dependencies.put("tfc.smallerunits.flywheel.mixin.CModCompatMixin", "dev.engine_room.flywheel.api.visualization.VisualEmbedding");
        dependencies.put("tfc.smallerunits.flywheel.mixin.ModCompatMixin", "dev.engine_room.flywheel.api.visualization.VisualEmbedding");
    }
}

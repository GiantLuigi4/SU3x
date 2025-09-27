package tfc.smallerunits.sodium;

import tfc.smallerunits.plat.asm.BasePlugin;

public class MixinConnector extends BasePlugin {
    public MixinConnector() {
        dependencies.put("tfc.smallerunits.sodium.mixin.LevelMixin", "me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
        dependencies.put("tfc.smallerunits.sodium.mixin.RenderSectionMixin", "me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
        dependencies.put("tfc.smallerunits.sodium.mixin.SodiumDebugOverlayMixin", "me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
        dependencies.put("tfc.smallerunits.sodium.mixin.SodiumLevelRendererMixin", "me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
        dependencies.put("tfc.smallerunits.sodium.mixin.UnitCapabilityHandlerMixin", "me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
    }
}

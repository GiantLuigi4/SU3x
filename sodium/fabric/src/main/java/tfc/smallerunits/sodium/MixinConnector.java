package tfc.smallerunits.sodium;

import tfc.smallerunits.plat.asm.BasePlugin;

public class MixinConnector extends BasePlugin {
    public MixinConnector() {
	    dependencies.put("tfc.smallerunits.sodium.mixin.BuiltSectionMixin", "me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
	    dependencies.put("tfc.smallerunits.sodium.mixin.RenderListMixin", "me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
	    dependencies.put("tfc.smallerunits.sodium.mixin.RenderSectionMixin", "me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
	    dependencies.put("tfc.smallerunits.sodium.mixin.RenderSectionMixin1", "me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
	    dependencies.put("tfc.smallerunits.sodium.mixin.SectionBuilderMixin", "me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
	    dependencies.put("tfc.smallerunits.sodium.mixin.SodiumRendererMixin", "me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
	    dependencies.put("tfc.smallerunits.sodium.mixin.ChunkBuilderMeshingTaskMixin", "me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
	    dependencies.put("tfc.smallerunits.sodium.mixin.WorldSliceMixin", "me.jellysquid.mods.sodium.mixin.core.render.world.WorldRendererMixin");
    }
}

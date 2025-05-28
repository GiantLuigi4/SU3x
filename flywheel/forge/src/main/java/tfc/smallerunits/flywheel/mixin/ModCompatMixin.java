package tfc.smallerunits.flywheel.mixin;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.utils.asm.ModCompat;

@Mixin(value = ModCompat.class, remap = false)
public class ModCompatMixin {
	@Inject(at = @At("HEAD"), method = "onAddBE")
	private static void preAddBE(BlockEntity be, CallbackInfo ci) {
//		if (Backend.canUseInstancing(be.getLevel())) {// 40
//			if (InstancedRenderRegistry.canInstance(be.getType())) {// 47
//				InstancedRenderDispatcher.getBlockEntities(be.getLevel()).add(be);// 56
//				InstancedRenderDispatcher.getBlockEntities(be.getLevel()).update(be);// 75 76
//			}
//		}
		VisualizationManager manager = VisualizationManagerImpl.get(be.getLevel());
		manager.blockEntities().queueAdd(be);
	}
	
	@Inject(at = @At("HEAD"), method = "onRemoveBE")
	private static void preRemoveBE(BlockEntity be, CallbackInfo ci) {
		VisualizationManager manager = VisualizationManagerImpl.get(be.getLevel());
		manager.blockEntities().queueRemove(be);
//		if (Backend.canUseInstancing(be.getLevel())) {// 40
//			if (InstancedRenderRegistry.canInstance(be.getType())) {// 47
//				InstancedRenderDispatcher.getBlockEntities(be.getLevel()).remove(be);
//				InstancedRenderDispatcher.getBlockEntities(be.getLevel()).update(be);// 75 76
//			}
//		}
	}
}

//package tfc.smallerunits.flywheel.mixin;
//
//import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
//import net.minecraft.world.level.LevelAccessor;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//import tfc.smallerunits.flywheel.visual.VisualContainer;
//
//@Mixin(VisualizationManagerImpl.class)
//public class VisManagerMixin {
//    @Inject(at = @At("RETURN"), method = "get")
//    private static void postGet(LevelAccessor level, CallbackInfoReturnable<VisualizationManagerImpl> cir) {
//        if (level instanceof VisualContainer) {
//            cir.setReturnValue(((VisualContainer) level).getEffect().getManager());
//        }
//    }
//}

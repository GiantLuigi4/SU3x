package tfc.smallerunits.flywheel.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.utils.asm.ModCompat;
import tfc.smallerunits.flywheel.visual.VisualContainer;

@Mixin(value = ModCompat.class, remap = false)
public class ModCompatMixin {
    @Inject(at = @At("HEAD"), method = "onAddBE")
    private static void onAdded(BlockEntity be, CallbackInfo ci) {
        Level lvl = be.getLevel();
        ((VisualContainer) lvl).getEffect().add(be);
    }

    @Inject(at = @At("HEAD"), method = "onRemoveBE")
    private static void onRemoved(BlockEntity be, CallbackInfo ci) {
        Level lvl = be.getLevel();
        ((VisualContainer) lvl).getEffect().remove(be);
    }
}

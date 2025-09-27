package tfc.smallerunits.fabric.mixin.self_impl;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import tfc.smallerunits.plat.itf.CapabilityLike;

@Mixin(CapabilityLike.class)
public interface CapabilityInjector extends ComponentV3 {
    @Shadow
    CompoundTag serializeNBT(CompoundTag tag);

    @Shadow
    void deserializeNBT(CompoundTag nbt);

    @Override
    default void readFromNbt(CompoundTag tag) {
        deserializeNBT(tag);
    }

    @Override
    default void writeToNbt(CompoundTag tag) {
        serializeNBT(tag);
    }
}

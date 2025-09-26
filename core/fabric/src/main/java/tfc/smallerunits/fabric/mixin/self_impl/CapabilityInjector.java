package tfc.smallerunits.fabric.mixin.self_impl;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import tfc.smallerunits.plat.itf.CapabilityLike;

@Mixin(CapabilityLike.class)
public abstract class CapabilityInjector implements ComponentV3 {
    @Shadow
    public abstract CompoundTag serializeNBT(CompoundTag tag);

    @Shadow
    public abstract void deserializeNBT(CompoundTag nbt);

    @Override
    public void readFromNbt(CompoundTag tag) {
        deserializeNBT(tag);
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        serializeNBT(tag);
    }
}

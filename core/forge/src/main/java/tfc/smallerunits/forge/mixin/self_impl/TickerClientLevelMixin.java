package tfc.smallerunits.forge.mixin.self_impl;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import tfc.smallerunits.core.simulation.level.client.AbstractTickerClientLevel;
import tfc.smallerunits.forge.level.client.ForgeTickerClientLevel;
import tfc.smallerunits.storage.IRegion;

import java.util.function.Supplier;

@Mixin(value = AbstractTickerClientLevel.class, remap = false)
public class TickerClientLevelMixin {
    /**
     * @author GiantLuigi4
     */
    @Overwrite
    public static AbstractTickerClientLevel createClientLevel(ClientLevel parent, ClientPacketListener p_205505_, ClientLevel.ClientLevelData p_205506_, ResourceKey<Level> p_205507_, Holder<DimensionType> p_205508_, int p_205509_, int p_205510_, Supplier<ProfilerFiller> p_205511_, LevelRenderer p_205512_, boolean p_205513_, long p_205514_, int upb, IRegion region) {
        return new ForgeTickerClientLevel(parent, p_205505_, p_205506_, p_205507_, p_205508_, p_205509_, p_205510_, p_205511_, p_205512_, p_205513_, p_205514_, upb, region);
    }
}

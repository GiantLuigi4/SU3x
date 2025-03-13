package tfc.smallerunits.plat.mixin.self_impl;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import tfc.smallerunits.data.storage.IRegion;
import tfc.smallerunits.simulation.level.client.ForgeTickerClientLevel;
import tfc.smallerunits.simulation.level.server.AbstractTickerServerLevel;
import tfc.smallerunits.simulation.level.server.ForgeTickerServerLevel;

import java.util.List;
import java.util.function.Supplier;

@Mixin(AbstractTickerServerLevel.class)
public class TickerServerLevelMixin {
    /**
     * @author GiantLuigi4
     * @reason implement method
     */
    @Overwrite
    public static AbstractTickerServerLevel createServerLevel(MinecraftServer server, ServerLevelData data, ResourceKey<Level> p_8575_, DimensionType dimType, ChunkProgressListener progressListener, ChunkGenerator generator, boolean p_8579_, long p_8580_, List<CustomSpawner> spawners, boolean p_8582_, Level parent, int upb, IRegion region) {
        return new ForgeTickerServerLevel(server, data, p_8575_, dimType, progressListener, generator, p_8579_, p_8580_, spawners, p_8582_, parent, upb, region);
    }
}

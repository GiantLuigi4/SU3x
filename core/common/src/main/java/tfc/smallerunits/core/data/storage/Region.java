package tfc.smallerunits.core.data.storage;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.game.ServerPacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.storage.ServerLevelData;
import tfc.smallerunits.core.SmallerUnits;
import tfc.smallerunits.core.logging.Loggers;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.core.simulation.level.client.AbstractTickerClientLevel;
import tfc.smallerunits.core.simulation.level.server.AbstractTickerServerLevel;
import tfc.smallerunits.core.simulation.level.server.LevelSourceProviderProvider;
import tfc.smallerunits.core.utils.IHateTheDistCleaner;
import tfc.smallerunits.core.utils.threading.ThreadLocals;
import tfc.smallerunits.plat.util.PlatformProvider;
import tfc.smallerunits.storage.IRegion;
import tfc.smallerunits.storage.IRegionPos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Consumer;

public class Region extends IRegion {
    public final RegionPos pos;
    int chunksLoaded = 0;
    Level[] levels;

    public Region(RegionPos pos) {
        this.pos = pos;
        // TODO: config
        this.levels = new Level[SmallerUnits.ABS_MIN + 1];
    }

    public int subtractRef(RegionPos regionPos) {
        chunksLoaded--;
        return chunksLoaded;
    }

    public void addRef(RegionPos regionPos) {
        chunksLoaded++;
    }

    protected void onAddServerLevel(Level parent, Level added, int upb) {
        // no-op; mixin target
    }

    public AbstractTickerServerLevel getServerWorld(MinecraftServer srv, ServerLevel parent, int upb) {
        if (levels[upb] == null) {
            try {
                ThreadLocals.levelLocal.set(parent);
                levels[upb] = AbstractTickerServerLevel.createServerLevel(
                        srv,
                        // TODO: wrap level data
                        (ServerLevelData) parent.getLevelData(),
                        // TODO:
                        parent.dimension(), parent.dimensionType(),
                        new ChunkProgressListener() {
                            @Override
                            public void updateSpawnPos(ChunkPos pCenter) {
                            }

                            @Override
                            public void onStatusChange(ChunkPos pChunkPosition, ChunkStatus pNewStatus) {
                            }

                            @Override
                            public void start() {
                            }

                            @Override
                            public void stop() {
                            }
                        },
                        LevelSourceProviderProvider.createGenerator(srv.getServerVersion(), parent, pos.toBlockPos()),
                        false, 0, new ArrayList<>(), false,
                        parent, upb, this
                );
                onAddServerLevel(parent, levels[upb], upb);
            } catch (Throwable e) {
                RuntimeException ex = new RuntimeException(e.getMessage(), e);
                ex.setStackTrace(e.getStackTrace());
                Loggers.UNITSPACE_LOGGER.error("", e);
                throw ex;
            }
        }

        return (AbstractTickerServerLevel) levels[upb];
    }

    protected void onAddClientLevel(Level parent, Level added, int upb) {
        // no-op; mixin target
    }

    public Level getClientWorld(Level parent, int upb) {
//		if (!(parent instanceof ClientLevel)) return null;
        if (levels[upb] == null) {
            try {
                ThreadLocals.levelLocal.set(parent);
                levels[upb] = AbstractTickerClientLevel.createClientLevel(
                        (ClientLevel) parent,
                        IHateTheDistCleaner.getConnection((ClientLevel) parent), ((ClientLevel) parent).getLevelData(),
                        parent.dimension(), Holder.direct(parent.dimensionType()),
                        0, 0, parent.getProfilerSupplier(),
                        null, true, 0,
                        upb, this
                );
                levels[upb].isClientSide = true;
                onAddClientLevel(parent, levels[upb], upb);
//				TickerServerWorld lvl = ((TickerServerWorld) levels[upb]);
//				lvl.lookup = pos -> {
//					BlockPos bp = lvl.region.pos.toBlockPos().offset(
//							// TODO: double check this
//							Math.floor(pos.getX() / (double) upb),
//							Math.floor(pos.getY() / (double) upb),
//							Math.floor(pos.getZ() / (double) upb)
//					);
//					Map<BlockPos, BlockState> cache = lvl.cache;
//					if (cache.containsKey(bp)) return cache.get(bp);
//					BlockState state;
////					if (!parent.isLoaded(bp)) return Blocks.VOID_AIR.defaultBlockState();
//					ChunkPos cp = new ChunkPos(bp);
//					if (parent.getChunk(cp.x, cp.z, ChunkStatus.FULL, false) == null)
//						return Blocks.VOID_AIR.defaultBlockState();
//					cache.put(bp, state = parent.getBlockState(bp));
//					return state;
//				};
            } catch (Throwable e) {
                RuntimeException ex = new RuntimeException(e.getMessage(), e);
                ex.setStackTrace(e.getStackTrace());
                Loggers.UNITSPACE_LOGGER.error("", e);
                throw ex;
            }
        }

        return levels[upb];
    }

    public void updateWorlds(BlockPos pos) {
        for (Level level : levels) {
            if (level != null) {
                ((ITickerLevel) level).invalidateCache(pos);
            }
        }
    }

    public void tickWorlds() {
        for (Level level : levels) {
            if (level == null) continue;
            if (!level.isClientSide) {
                if (level instanceof ServerLevel) {
                    ((ServerLevel) level).tick(() -> true);
                }
            } else {
                if (IHateTheDistCleaner.isClientLevel(level)) {
                    IHateTheDistCleaner.tickLevel(level);
                }
            }
        }
    }

    public void forEachLevel(Consumer<Level> func) {
        for (Level level : levels) {
            if (level == null) continue;
            func.accept(level);
        }
    }

    public Level getLevel(PacketListener listener, Player player, int upb) {
        if (listener instanceof ServerPacketListener) {
            return getServerWorld(player.level().getServer(), (ServerLevel) player.level(), upb);
        } else {
            return getClientWorld(player.level(), upb);
        }
    }

    public Level[] getLevels() {
        return levels;
    }

    protected void onServerLevelClosed(Level level) {
        // no-op; mixin target
    }

    protected void onClientLevelClosed(Level level) {
        // no-op; mixin target
    }

    public void close() {
        for (Level level : levels) {
            try {
                if (level != null) {
                    if (level instanceof AbstractTickerServerLevel) {
                        ((AbstractTickerServerLevel) level).saveWorld.saveLevel();
                        ((AbstractTickerServerLevel) level).saveWorld.saveAllChunks();
                        onServerLevelClosed(level);
                    } else {
                        onClientLevelClosed(level);
                    }
                    PlatformProvider.UTILS.unloadLevel(level);
                    level.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                // TODO: probably should handle this
            }
        }
    }

    public Level getExistingLevel(int upb) {
        return levels[upb];
    }

    @Override
    public IRegionPos pos() {
        return pos;
    }
}

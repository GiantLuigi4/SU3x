package tfc.smallerunits.core.simulation.level.server;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashBigSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.Nullable;
import tfc.smallerunits.core.api.PositionUtils;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.data.storage.RegionPos;
import tfc.smallerunits.core.data.tracking.RegionalAttachments;
import tfc.smallerunits.core.networking.hackery.NetworkingHacks;
import tfc.smallerunits.core.simulation.WorldStitcher;
import tfc.smallerunits.core.simulation.block.ParentLookup;
import tfc.smallerunits.core.simulation.chunk.BasicVerticalChunk;
import tfc.smallerunits.core.simulation.chunk.VChunkLookup;
import tfc.smallerunits.core.simulation.level.ITickerChunkCache;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.core.simulation.level.UnitChunkHolder;
import tfc.smallerunits.core.simulation.level.server.saving.SUDimStorage;
import tfc.smallerunits.core.simulation.light.NotThreadedSULightManager;
import tfc.smallerunits.plat.util.PlatformProvider;

import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class TickerChunkCache extends ServerChunkCache implements ITickerChunkCache {
    public final BasicVerticalChunk[][] columns;
    private final EmptyLevelChunk empty;
    // TODO: make this not needed
    int upb;


    protected final ObjectOpenHashBigSet<ChunkHolder> holders = new ObjectOpenHashBigSet<>();

    public TickerChunkCache(ServerLevel p_214982_, LevelStorageSource.LevelStorageAccess p_214983_, DataFixer p_214984_, StructureTemplateManager p_214985_, Executor p_214986_, ChunkGenerator p_214987_, int p_214988_, int p_214989_, boolean p_214990_, ChunkProgressListener p_214991_, ChunkStatusUpdateListener p_214992_, Supplier<DimensionDataStorage> p_214993_, int upb) {
        super(p_214982_, p_214983_, p_214984_, p_214985_, p_214986_, p_214987_, p_214988_, p_214989_, p_214990_, p_214991_, p_214992_, p_214993_);
        this.chunkMap = new UnitChunkMap(p_214982_, p_214983_, p_214984_, p_214985_, p_214986_, this.mainThreadProcessor, this, p_214987_, p_214991_, p_214992_, p_214993_, p_214988_, p_214990_);
        this.upb = upb;
        columns = new BasicVerticalChunk[33 * 33 * upb * upb][];
        empty = new EmptyLevelChunk(this.level, new ChunkPos(0, 0), p_214982_.registryAccess().registry(Registries.BIOME).get().getHolder(Biomes.THE_VOID).get());
        lightEngine = new NotThreadedSULightManager(this, this.chunkMap, true);
        this.dataStorage = new SUDimStorage(null, p_214984_);
    }

    @Override
    public boolean hasChunk(int pX, int pZ) {
        // TODO:
        return (!(getChunk(pX, pZ, ChunkStatus.FULL, false) instanceof EmptyLevelChunk));
    }

    @Override
    public void broadcastAndSend(Entity pEntity, Packet<?> pPacket) {
        NetworkingHacks.LevelDescriptor descriptor = maybeRemoveUnitPos(pEntity, pPacket);
        super.broadcastAndSend(pEntity, pPacket);
        NetworkingHacks.setPos(descriptor);
    }

    public NetworkingHacks.LevelDescriptor maybeRemoveUnitPos(Entity pEntity, Packet<?> pPacket) {
        NetworkingHacks.LevelDescriptor descriptor = NetworkingHacks.unitPos.get();
        if (pPacket instanceof ClientboundAnimatePacket) {
            // TODO: check if the player should actually be in the unit space or not
            if (pEntity instanceof ServerPlayer) {
                NetworkingHacks.unitPos.remove();
            }
        }
        return descriptor;
    }

    @Override
    public void broadcast(Entity pEntity, Packet<?> pPacket) {
        NetworkingHacks.LevelDescriptor descriptor = maybeRemoveUnitPos(pEntity, pPacket);
        super.broadcast(pEntity, pPacket);
        NetworkingHacks.setPos(descriptor);
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int pChunkX, int pChunkZ, ChunkStatus pRequiredStatus, boolean pLoad) {
        return getChunk(pChunkX, 0, pChunkZ, pRequiredStatus, pLoad);
    }

    private final ObjectOpenHashBigSet<LevelChunk> allChunks = new ObjectOpenHashBigSet<>();
    private final ObjectOpenHashBigSet<LevelChunk> newChunks = new ObjectOpenHashBigSet<>();

    @Override
    public void removeEntity(Entity pEntity) {
        super.removeEntity(pEntity);
        ((ITickerLevel) level).SU$removeEntity(pEntity);
    }

    @Override
    public void tick(BooleanSupplier pHasTimeLeft, boolean p_201914_ /* what? */) {
        int tickCount = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        synchronized (newChunks) {
            try {
                ObjectOpenHashBigSet<LevelChunk> copy = new ObjectOpenHashBigSet<>(newChunks);
                newChunks.clear();
                allChunks.addAll(copy);
            } catch (Throwable ignored) {
            }
        }
        synchronized (allChunks) {
            // TODO: new chunks set
            for (LevelChunk allChunk : allChunks) {
                level.tickChunk(allChunk, tickCount);
                ((BasicVerticalChunk) allChunk).randomTick();
            }
        }

        super.tick(pHasTimeLeft, false);
        ((UnitChunkMap) chunkMap).tick();

        synchronized (holders) {
            for (ChunkHolder holder : holders) {
                LevelChunk chunk = holder.getTickingChunk();
                if (chunk != null)
                    holder.broadcastChanges(chunk);
            }
        }

//		for (BasicVerticalChunk[] column : columns) {
//			if (column == null) continue;
//			for (BasicVerticalChunk basicVerticalChunk : column) {
//				if (basicVerticalChunk == null) continue;
////				level.tickChunk(basicVerticalChunk, 100);
//				basicVerticalChunk.randomTick();
//			}
//		}
    }

    public Iterable<ChunkHolder> getChunks() {
        return holders;
    }

    public ParentLookup getLookup() {
        return ((AbstractTickerServerLevel) level).lookup;
    }

    WeakReference<Level>[] neighbors = new WeakReference[Direction.values().length];

    public ChunkAccess getChunk(int pChunkX, int pChunkY, int pChunkZ, ChunkStatus pRequiredStatus, boolean pLoad) {
        if (pChunkX >= (upb * 32) || pChunkZ >= (upb * 32) || pChunkZ < 0 || pChunkX < 0 || pChunkY < 0 || pChunkY >= (upb * 32)) {
            LevelChunk neighborChunk = WorldStitcher.getChunk(pChunkX, pChunkY, pChunkZ, (ITickerLevel) level, this, upb, pRequiredStatus, pLoad, neighbors);
            if (neighborChunk != null) return neighborChunk;
            if (!pLoad) return null;

            Region r = ((AbstractTickerServerLevel) level).region;
            RegionPos pos = r.pos;

            int x = pChunkX < 0 ? -1 : ((pChunkX > upb) ? 1 : 0);
            int y = pChunkY < 0 ? -1 : ((pChunkY > upb) ? 1 : 0);
            int z = pChunkZ < 0 ? -1 : ((pChunkZ > upb) ? 1 : 0);
            pos = new RegionPos(
                    pos.x + x,
                    pos.y + y,
                    pos.z + z
            );

            pChunkX = ((pChunkX < 0) ? pChunkX + upb : ((pChunkX > upb) ? (pChunkX - (upb * 32)) : pChunkX));
            pChunkY = ((pChunkY < 0) ? pChunkY + upb : ((pChunkY > upb) ? (pChunkX - (upb * 32)) : pChunkY));
            pChunkZ = ((pChunkZ < 0) ? pChunkZ + upb : ((pChunkZ > upb) ? (pChunkX - (upb * 32)) : pChunkZ));

            Level parent = ((AbstractTickerServerLevel) level).parent.get();
            Region otherRegion = null;
            Level level = null;
            otherRegion = ((RegionalAttachments) ((ServerChunkCache) parent.getChunkSource()).chunkMap).SU$getRegion(pos);
            if (otherRegion != null)
                level = otherRegion.getServerWorld(this.level.getServer(), (ServerLevel) parent, upb);
            else {
                EmptyLevelChunk chunk = empty;
                return chunk;
            }
            return level.getChunk(pChunkX, pChunkZ);
//			return new EmptyLevelChunk(level, new ChunkPos(pChunkX, pChunkZ));
        }
        if (!pLoad) {
            BasicVerticalChunk[] chunks = columns[pChunkX * (33 * upb) + pChunkZ];
            if (chunks == null) return null;
            return chunks[pChunkY];
        } else {
            BasicVerticalChunk[] ck = columns[pChunkX * (33 * upb) + pChunkZ];
//			if (ck == null) ck = columns[pChunkX * (33 * upb) + pChunkZ] = new BasicVerticalChunk[33 * upb];
            if (ck == null || ck[pChunkY] == null) {
                return createChunk(pChunkY, new ChunkPos(pChunkX, pChunkZ));
//                if (ck == null) {
//                    ck = columns[pChunkX * (33 * upb) + pChunkZ];
//                    ck[pChunkY] = vc;
//                }
            }
            return ck[pChunkY];
        }
    }

    @Override
    public void blockChanged(BlockPos pPos) {
        int i = SectionPos.blockToSectionCoord(pPos.getX());
        int j = SectionPos.blockToSectionCoord(pPos.getZ());
        int y = SectionPos.blockToSectionCoord(pPos.getY());

        BasicVerticalChunk vc = (BasicVerticalChunk) getChunk(i, j, false);
        if (vc == null) return;
        vc = vc.getSubChunk(y);
        if (vc == null) return;
        vc.holder.blockChanged(new BlockPos(pPos.getX() & 15, pPos.getY() & 15, pPos.getZ() & 15));
    }

    ChunkHolder.LevelChangeListener noListener = (a, b, c, d) -> {
    };

    @Override
    public BasicVerticalChunk createChunk(int yPos, ChunkPos ckPos) {
        int pChunkX = ckPos.x;
        int pChunkZ = ckPos.z;
        BasicVerticalChunk[] ck = columns[pChunkX * (33 * upb) + pChunkZ];
        if (ck == null)
            ck = columns[pChunkX * (33 * upb) + pChunkZ] = new BasicVerticalChunk[33 * upb];
        BasicVerticalChunk bvci = new BasicVerticalChunk(
                level, new ChunkPos(pChunkX, pChunkZ), yPos,
                new VChunkLookup(
                        this, yPos, ck,
                        new ChunkPos(pChunkX, pChunkZ), upb * 32
                ), getLookup(), upb
        );
        ck[yPos] = bvci;
        ((AbstractTickerServerLevel) level).saveWorld.load(bvci, bvci.getPos(), bvci.yPos);
        synchronized (newChunks) {
            newChunks.add(bvci);
        }
        UnitChunkHolder holder = new UnitChunkHolder(bvci.getPos(), 0, level, level.getLightEngine(), noListener, chunkMap, bvci, yPos);
        synchronized (holders) {
            holders.add(holder);
        }
        bvci.holder = holder;
        PlatformProvider.UTILS.chunkLoaded(bvci);

        return bvci;
    }

    @Override
    public void save(boolean p_8420_) {
//		((AbstractTickerServerLevel)level).saveWorld.saveAllChunks();
    }

    @Override
    public BasicVerticalChunk getChunk(int yPos, ChunkPos ckPos) {
        int pChunkX = ckPos.x;
        int pChunkZ = ckPos.z;
        BasicVerticalChunk[] ck = columns[pChunkX * (33 * upb) + pChunkZ];
        if (ck == null) return null;
        return ck[yPos];
    }

    @Nullable
    @Override
    public LevelChunk getChunkNow(int pChunkX, int pChunkZ) {
        return (LevelChunk) getChunk(pChunkX, pChunkZ, ChunkStatus.FULL, false);
    }

    @Override
    public EmptyLevelChunk getEmpty() {
        return empty;
    }

    @Override
    public ITickerLevel tickerLevel() {
        return (ITickerLevel) level;
    }

    public void removeChunk(BasicVerticalChunk pChunk) {
        ChunkPos ckPos = pChunk.getPos();
        int yPos = pChunk.yPos;
        int pChunkX = ckPos.x;
        int pChunkZ = ckPos.z;
        BasicVerticalChunk[] ck = columns[pChunkX * (33 * upb) + pChunkZ];
        if (ck == null) return;
        ck[yPos] = null;
    }

    @Override
    public boolean isPositionTicking(long pChunkPos) {
        ChunkPos cpos = new ChunkPos(pChunkPos);
        BlockPos bp = PositionUtils.getParentPos(
                new BlockPos(
                        cpos.getMinBlockX(), 0, cpos.getMinBlockZ()
                ),
                (ITickerLevel) level
        );

        return ((ServerChunkCache) ((ITickerLevel) level).getParent().getChunkSource()).isPositionTicking(new ChunkPos(bp).toLong()) && hasChunk(cpos.x, cpos.z);
    }

    @Override
    public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getChunkFuture(int pX, int pY, ChunkStatus p_8434_, boolean p_8435_) {
        return CompletableFuture.completedFuture(
                Either.left(getChunk(pX, pY, p_8434_, p_8435_))
        );
    }

    // TODO: do I need to override this?
//    @Override
//    public BlockGetter getChunkForLighting(int pChunkX, int pChunkZ) {
//        return getChunk(pChunkX, pChunkZ, ChunkStatus.FULL, true);
//    }
}

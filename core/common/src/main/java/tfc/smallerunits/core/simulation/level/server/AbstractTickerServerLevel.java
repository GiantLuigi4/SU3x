package tfc.smallerunits.core.simulation.level.server;

import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import tfc.smallerunits.core.UnitEdge;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.UnitSpaceBlock;
import tfc.smallerunits.core.api.PositionUtils;
import tfc.smallerunits.core.client.access.tracking.SUCapableChunk;
import tfc.smallerunits.core.data.access.EntityAccessor;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.core.data.capability.SUCapabilityManager;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.logging.Loggers;
import tfc.smallerunits.core.networking.hackery.NetworkingHacks;
import tfc.smallerunits.core.simulation.block.ParentLookup;
import tfc.smallerunits.core.simulation.chunk.BasicVerticalChunk;
import tfc.smallerunits.core.simulation.level.EntityManager;
import tfc.smallerunits.core.simulation.level.ITickerChunkCache;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.core.simulation.level.SUTickList;
import tfc.smallerunits.core.simulation.level.server.saving.SUSaveWorld;
import tfc.smallerunits.core.utils.AddOnlyList;
import tfc.smallerunits.core.utils.PositionalInfo;
import tfc.smallerunits.core.utils.config.CommonConfig;
import tfc.smallerunits.core.utils.math.Math1D;
import tfc.smallerunits.core.utils.math.Math3d;
import tfc.smallerunits.core.utils.storage.GroupMap;
import tfc.smallerunits.core.utils.storage.VecMap;
import tfc.smallerunits.core.utils.threading.ThreadLocals;
import tfc.smallerunits.plat.CapabilityWrapper;
import tfc.smallerunits.plat.util.PlatformProvider;
import tfc.smallerunits.storage.IRegion;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ApiStatus.Internal
@SuppressWarnings("removal")
public abstract class AbstractTickerServerLevel extends ServerLevel implements ITickerLevel {
	private static final NoStorageSource src = NoStorageSource.make();
	private static final LevelStorageSource.LevelStorageAccess noAccess;
	
	static {
		try {
			noAccess = src.createAccess("no");
		} catch (IOException e) {
			RuntimeException ex = new RuntimeException(e.getMessage(), e);
			ex.setStackTrace(e.getStackTrace());
			throw ex;
		}
	}
	
	List<Entity> interactingEntities = new AddOnlyList<>();
	
	public void addInteractingEntity(Entity e) {
		if (e == null) {
			if (CommonConfig.DebugOptions.crashOnNullInteracter) {
				throw new RuntimeException("A null interacting entity has been added?");
			} else return;
		}
		
		interactingEntities.add(e);
	}
	
	public void removeInteractingEntity(Entity e) {
		interactingEntities.remove(e);
	}
	
	public final GroupMap<Pair<BlockState, VecMap<VoxelShape>>> cache = new GroupMap<>(2);
	
	@Override
	public Level getParent() {
		return parent.get();
	}
	
	@Override
	public Region getRegion() {
		return region;
	}
	
	@Override
	public ParentLookup getLookup() {
		return lookup;
	}
	
	//	ArrayList<Entity> entitiesAdded = new ArrayList<>();
	ArrayList<Entity> entitiesRemoved = new ArrayList<>();
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
	
	public final WeakReference<Level> parent;
	//	public final UnitSpace parentU;
	public final Region region;
	protected final ArrayList<Runnable> completeOnTick = new ArrayList<>();
	public final int upb;
	
	public final SUSaveWorld saveWorld;
	
	public AbstractTickerServerLevel(MinecraftServer server, ServerLevelData data, ResourceKey<Level> p_8575_, DimensionType dimType, ChunkProgressListener progressListener, ChunkGenerator generator, boolean p_8579_, long p_8580_, List<CustomSpawner> spawners, boolean p_8582_, Level parent, int upb, IRegion region) {
		super(
				server,
				Util.backgroundExecutor(),
				noAccess,
				data,
				p_8575_,
				new LevelStem(Holder.direct(dimType), generator),
				progressListener,
				p_8579_,
				p_8580_,
				spawners,
				p_8582_,
				new RandomSequences(p_8580_)
		);
//		this.parentU = parentU;
		this.parent = new WeakReference<>(parent);
		this.upb = upb;
		this.chunkSource = new TickerChunkCache(
				this, noAccess,
				((ServerLevel) parent).getServer().getFixerUpper(),
				getStructureManager(),
				Util.backgroundExecutor(),
				generator,
				0, 0,
				true,
				progressListener, (pPos, pStatus) -> {
		}, () -> null,
				upb
		);
		this.region = (Region) region;
		this.blockTicks = new SUTickList<>(null, null);
		this.fluidTicks = new SUTickList<>(null, null);
		
		ThreadLocal<WeakReference<LevelChunk>> lastChunk = new ThreadLocal<>();
		lookup = (pos) -> {
			if (cache.containsKey(pos)) {
//				BlockState state = cache.get(bp).getFirst();
//				VoxelShape shape = state.getCollisionShape(parent, bp);
				// TODO: empty shape check
				return cache.get(pos).getFirst();
			}
			
			Level pArent = this.parent.get();
			
			if (!getServer().isReady())
				return Blocks.VOID_AIR.defaultBlockState();
			if (!pArent.isLoaded(pos))
				return Blocks.VOID_AIR.defaultBlockState();
			
			ChunkPos ckPos = new ChunkPos(pos);
			WeakReference<LevelChunk> chunkRef = lastChunk.get();
			LevelChunk ck;
			if (chunkRef == null || (ck = chunkRef.get()) == null)
				lastChunk.set(new WeakReference<>(ck = pArent.getChunkAt(pos)));
			else if (!chunkRef.get().getPos().equals(ckPos))
				lastChunk.set(new WeakReference<>(ck = pArent.getChunkAt(pos)));
			
			BlockState state = ck.getBlockState(pos);
			cache.put(pos, Pair.of(state, new VecMap<>(2)));
			return state;
		};
//		((ServerLevel) parent).getDataStorage()
		isLoaded = true;
		
		saveWorld = new SUSaveWorld(((ServerLevel) parent).getDataStorage().dataFolder, this);
		this.getDataStorage().dataFolder = new File(saveWorld.file + "/data/");
		
		this.entityManager = new EntityManager<>(this, Entity.class, new EntityCallbacks(), new EntityStorage(this, noAccess.getDimensionPath(p_8575_).resolve("entities"), server.getFixerUpper(), server.forceSynchronousWrites(), server));
		PlatformProvider.UTILS.loadLevel(this);
	}
	
	@Override
	public void unload(LevelChunk pChunk) {
		super.unload(pChunk);
		((TickerChunkCache) this.chunkSource).removeChunk((BasicVerticalChunk) pChunk);
	}
	
	int randomTickCount = Integer.MIN_VALUE;
	
	public void broadcastTo(Player pExcept, double pX, double pY, double pZ, double pRadius, ResourceKey<Level> pDimension, Packet<?> pPacket) {
		Level lvl = parent.get();
		if (lvl == null) return;
		for (int i = 0; i < lvl.players().size(); ++i) {
			ServerPlayer serverplayer = (ServerPlayer) lvl.players().get(i);
			if (serverplayer == pExcept && serverplayer.level().dimension() == pDimension) {
				double d0 = pX - serverplayer.getX();
				double d1 = pY - serverplayer.getY();
				double d2 = pZ - serverplayer.getZ();
				if (d0 * d0 + d1 * d1 + d2 * d2 < pRadius * pRadius) {
					serverplayer.connection.send(pPacket);
				}
			}
		}
	}
	
	public <T extends ParticleOptions> int sendParticles(T pType, double pPosX, double pPosY, double pPosZ, int pParticleCount, double pXOffset, double pYOffset, double pZOffset, double pSpeed) {
		ClientboundLevelParticlesPacket clientboundlevelparticlespacket = new ClientboundLevelParticlesPacket(pType, false, pPosX, pPosY, pPosZ, (float) pXOffset, (float) pYOffset, (float) pZOffset, (float) pSpeed, pParticleCount);
		int i = 0;
		
		Level lvl = parent.get();
		if (lvl == null) return 0;
		
		for (int j = 0; j < lvl.players().size(); ++j) {
			ServerPlayer serverplayer = (ServerPlayer) lvl.players().get(j);
			if (this.sendParticles(serverplayer, false, pPosX, pPosY, pPosZ, clientboundlevelparticlespacket)) {
				++i;
			}
		}
		
		return i;
	}
	
	@Override
	public boolean sendParticles(ServerPlayer pPlayer, boolean pLongDistance, double pPosX, double pPosY, double pPosZ, Packet<?> pPacket) {
		Level lvl = parent.get();
		if (lvl == null) return false;
		if (pPlayer.level() == lvl || pPlayer.level() == this) {
			BlockPos blockpos = pPlayer.blockPosition();
			
			double scl = 1f / upb;
			BlockPos pos = getRegion().pos.toBlockPos();
			pPosX *= scl;
			pPosY *= scl;
			pPosZ *= scl;
			pPosX += pos.getX();
			pPosY += pos.getY();
			pPosZ += pos.getZ();
			
			if (blockpos.closerToCenterThan(new Vec3(pPosX, pPosY, pPosZ), pLongDistance ? (512.0D * scl) : (32.0D * scl))) {
				pPlayer.connection.send(pPacket);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	@Override
	public void SU$removeEntity(Entity pEntity) {
		if (!entitiesRemoved.contains(pEntity)) entitiesRemoved.add(pEntity);
	}
	
	@Override
	public Holder<Biome> getBiome(BlockPos pos) {
		BlockPos bp = region.pos.toBlockPos().offset(
				// TODO: double check this
				(int) Math.floor(pos.getX() / (double) upb),
				(int) Math.floor(pos.getY() / (double) upb),
				(int) Math.floor(pos.getZ() / (double) upb)
		);
		return parent.get().getBiome(bp.offset(region.pos.toBlockPos()));
	}
	
	public ParentLookup lookup;
	ArrayList<Entity> entities = new ArrayList<>();
	
	@Override
	public void SU$removeEntity(UUID uuid) {
		SU$removeEntity(getEntity(uuid));
	}
	
	@Nullable
	@Override
	public Entity getEntity(UUID pUniqueId) {
		for (Entity entity : entities) {
			if (entity.getUUID().equals(pUniqueId)) { // TODO: make this smarter
				return entity;
			}
		}
		return null;
	}
	
	@Override
	public float getShade(Direction pDirection, boolean pShade) {
		return parent.get().getShade(pDirection, pShade);
	}
	
	@Override
	public int getSectionsCount() {
		return getMaxSection() - getMinSection();
	}
	
	@Override
	public int getMinSection() {
		return 0;
	}
	
	@Override
	public int getSectionIndexFromSectionY(int pSectionIndex) {
		return pSectionIndex;
	}
	
	@Override
	public int getMaxSection() {
		return upb * 512;
	}
	
	@Override
	public LevelChunk getChunkAt(BlockPos pPos) {
		return (LevelChunk) ((TickerChunkCache) this.getChunkSource()).getChunk(
				SectionPos.blockToSectionCoord(pPos.getX()),
				0,
				SectionPos.blockToSectionCoord(pPos.getZ()),
				ChunkStatus.FULL, true
		);
	}
	
	public LevelChunk getChunkAtNoLoad(BlockPos pPos) {
		return (LevelChunk) ((TickerChunkCache) this.getChunkSource()).getChunk(
				SectionPos.blockToSectionCoord(pPos.getX()),
				0,
				SectionPos.blockToSectionCoord(pPos.getZ()),
				ChunkStatus.FULL, false
		);
	}
	
	@Override
	public LevelChunk getChunk(int pChunkX, int pChunkZ) {
		return super.getChunk(pChunkX, pChunkZ);
	}
	
	@Nullable
	@Override
	public ChunkAccess getChunk(int pX, int pZ, ChunkStatus pRequiredStatus, boolean pNonnull) {
		return super.getChunk(pX, pZ, pRequiredStatus, pNonnull);
	}
	
	boolean isLoaded = false;
	
	@Override
	public int getUPB() {
		return upb;
	}
	
	HashMap<Entity, ServerEntity> serverEntityHashMap = new HashMap<>();
	
	int nextId = 0;
	
	@Override
	public boolean addFreshEntity(Entity pEntity) {
//		int firstOpen = -1;
//		int prev = -1;
//		for (Entity entity : entities) {
//			if (firstOpen != prev) {
//				break;
//			}
//			firstOpen++;
//			prev = entity.getId();
//		}
//		if (firstOpen != -1) pEntity.setId(firstOpen + 1);
//		else pEntity.setId(0);
//		pEntity.setId(nextId++);

//		entities.add(pEntity);
		
		if ((!(pEntity instanceof LivingEntity)) && !(pEntity instanceof ItemEntity))
			return super.addFreshEntity(pEntity);
		
		Level lvl = parent.get();
		if (lvl == null) return false;
		
		NetworkingHacks.LevelDescriptor descriptor = NetworkingHacks.unitPos.get();
		NetworkingHacks.setPos(descriptor.parent());
		
		Entity entity = PlatformProvider.UTILS.migrateEntity(pEntity, this, upb, lvl);
		
		NetworkingHacks.setPos(descriptor);
		
		return entity != null;
	}
	
	public boolean hasChunksAt(int pFromX, int pFromZ, int pToX, int pToZ) {
		// TODO
		return true;
	}
	
	@Nullable
	@Override
	public Entity getEntity(int pId) {
		for (Entity entity : entities) {
			if (entity.getId() == pId) return entity;
		}
		return null;
	}
	
	@Override
	public LevelEntityGetter<Entity> getEntities() {
		return new LevelEntityGetter<>() {
			public Entity get(int p_156931_) {
				for (Entity entity : entities) {
					if (entity.getId() == p_156931_) return entity; // TODO: be not dumb
				}
				return null;
			}
			
			public Entity get(UUID pUuid) {
				for (Entity entity : entities) {
					if (entity.getUUID().equals(pUuid)) return entity; // TODO: be not dumb
				}
				return null;
			}
			
			public Iterable<Entity> getAll() {
				return entities;
			}

			@Override
			public <U extends Entity> void get(EntityTypeTest<Entity, U> entityTypeTest, AbortableIterationConsumer<U> abortableIterationConsumer) {
				for (Entity entity : entities) {
					if (entityTypeTest.getBaseClass().isInstance(entity)) {
						abortableIterationConsumer.accept((U) entity);
					}
				}
			}
			
			public void get(AABB p_156937_, Consumer<Entity> p_156938_) {
				for (Entity entity : entities) {
					if (p_156937_.intersects(entity.getBoundingBox())) {
						p_156938_.accept(entity); // this seems slow, but ok mojang
					}
				}
			}

			@Override
			public <U extends Entity> void get(EntityTypeTest<Entity, U> entityTypeTest, AABB aabb, AbortableIterationConsumer<U> abortableIterationConsumer) {
// ?
				for (Entity entity : entities) {
					if (aabb.intersects(entity.getBoundingBox())) {
						if (entityTypeTest.getBaseClass().isInstance(entity)) {
							abortableIterationConsumer.accept((U) entity);
						}
					}
				}
			}
		};
	}
	
	@Override
	public Iterable<Entity> getAllEntities() {
		return entities;
	}
	
	// ???
	private void tickSUBlock(BlockPos pos) {
		getBlockState(pos).tick(this, pos, this.random);
	}
	
	@Override
	public void setFromSync(ChunkPos cp, int cy, int x, int y, int z, BlockState state, ArrayList<BlockPos> positions, HashMap<SectionPos, ChunkAccess> chunkCache) {
		BlockPos rp = region.pos.toBlockPos();
		int xo = ((cp.x * 16) / upb) + (x / upb);
		int yo = ((cy * 16) / upb) + (y / upb);
		int zo = ((cp.z * 16) / upb) + (z / upb);
		BlockPos parentPos = rp.offset(xo, yo, zo);
		ChunkAccess ac;
		SectionPos pos = SectionPos.of(parentPos);
		// vertical lookups shouldn't be too expensive
		if (!chunkCache.containsKey(pos)) {
			ac = parent.get().getChunkAt(parentPos);
			chunkCache.put(pos, ac);
			if (!positions.contains(parentPos)) {
				ac.setBlockState(parentPos, tfc.smallerunits.core.Registry.UNIT_SPACE.get().defaultBlockState(), false);
				positions.add(parentPos);
			}
		} else ac = chunkCache.get(pos);
		
		ISUCapability cap = SUCapabilityManager.getCapability((LevelChunk) ac);
		UnitSpace space = cap.getUnit(parentPos);
		if (space == null) {
			space = cap.getOrMakeUnit(parentPos);
			space.setUpb(upb);
		}
		BasicVerticalChunk vc = (BasicVerticalChunk) getChunkAt(cp.getWorldPosition());
		vc = vc.getSubChunk(cy);
		vc.setBlockFast(new BlockPos(x, y, z), state, chunkCache);
		
		((SUCapableChunk) ac).SU$markDirty(parentPos);
	}
	
	public CompoundTag getTicksIn(BlockPos myPosInTheLevel, BlockPos offset) {
		CompoundTag tag = new CompoundTag();
		AABB box = new AABB(myPosInTheLevel, offset);
		{
			CompoundTag blockTicks = new CompoundTag();
			ArrayList<ScheduledTick<Block>> ticks = ((SUTickList) this.blockTicks).getTicksInArea(box);
			Registry<Block> blockRegistry = parent.get().registryAccess().registryOrThrow(Registries.BLOCK);
			for (ScheduledTick<Block> tick : ticks) {
				CompoundTag tag1 = new CompoundTag();
				tag1.putLong("ttime", tick.triggerTick() - getGameTime());
				tag1.putString("ttype", blockRegistry.getKey(tick.type()).toString());
				tag1.putByte("tpriority", (byte) tick.priority().ordinal());
				tag1.putLong("tsub", (byte) tick.subTickOrder());
				blockTicks.put(tick.pos().toShortString().replace(" ", ""), tag1);
			}
			tag.put("blocks", blockTicks);
		}
		{
			CompoundTag blockTicks = new CompoundTag();
			ArrayList<ScheduledTick<Fluid>> ticks = ((SUTickList) this.fluidTicks).getTicksInArea(box);
			Registry<Fluid> fluidRegistry = parent.get().registryAccess().registryOrThrow(Registries.FLUID);
			for (ScheduledTick<Fluid> tick : ticks) {
				CompoundTag tag1 = new CompoundTag();
				tag1.putLong("ttime", tick.triggerTick() - getGameTime());
				tag1.putString("ttype", fluidRegistry.getKey(tick.type()).toString());
				tag1.putByte("tpriority", (byte) tick.priority().ordinal());
				tag1.putByte("tsub", (byte) tick.subTickOrder());
				blockTicks.put(tick.pos().toShortString().replace(" ", ""), tag1);
			}
			tag.put("fluids", blockTicks);
		}
		if (!tag.isEmpty()) {
			for (String allKey : tag.getAllKeys()) {
				if (!tag.getCompound(allKey).isEmpty()) {
					return tag;
				}
			}
			return new CompoundTag();
		}
//		((SUTickList) blockTicks).clearBox(box);
//		((SUTickList) fluidTicks).clearBox(box);
		return tag;
	}
	
	public void loadTicks(CompoundTag tag) {
		if (tag.isEmpty()) return;
		
		Registry<Block> blockRegistry = parent.get().registryAccess().registryOrThrow(Registries.BLOCK);
		CompoundTag blocks = tag.getCompound("blocks");
		for (String allKey : blocks.getAllKeys()) {
			CompoundTag tick = blocks.getCompound(allKey);
			long time = tick.getLong("ttime") + getGameTime();
			ResourceLocation regName = new ResourceLocation(tick.getString("ttype"));
			Block type = blockRegistry.get(regName);
			int priority = tick.getByte("tpriority");
			long sub = tick.getLong("tsub");
			String[] pos = allKey.split(",");
			int x = Integer.parseInt(pos[0]);
			int y = Integer.parseInt(pos[1]);
			int z = Integer.parseInt(pos[2]);
			blockTicks.schedule(new ScheduledTick<>(
					type, new BlockPos(x, y, z),
					time, TickPriority.values()[priority], sub
			));
		}
		
		Registry<Fluid> fluidRegistry = parent.get().registryAccess().registryOrThrow(Registries.FLUID);
		CompoundTag fluids = tag.getCompound("fluids");
		for (String allKey : fluids.getAllKeys()) {
			CompoundTag tick = fluids.getCompound(allKey);
			long time = tick.getLong("ttime") + getGameTime();
			ResourceLocation regName = new ResourceLocation(tick.getString("ttype"));
			Fluid type = fluidRegistry.get(regName);
			int priority = tick.getByte("tpriority");
			long sub = tick.getLong("tsub");
			String[] pos = allKey.split(",");
			int x = Integer.parseInt(pos[0]);
			int y = Integer.parseInt(pos[1]);
			int z = Integer.parseInt(pos[2]);
			fluidTicks.schedule(new ScheduledTick<>(
					type, new BlockPos(x, y, z),
					time, TickPriority.values()[priority], sub
			));
		}
	}
	
	@Override
	public long getGameTime() {
		return parent.get().getGameTime();
	}
	
	@Override
	public RegistryAccess registryAccess() {
		if (parent == null) return super.registryAccess();
		return parent.get().registryAccess();
	}
	
	public void clear(BlockPos myPosInTheLevel, BlockPos offset) {
		HashMap<SectionPos, ChunkAccess> cache = new HashMap<>();
		BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
		for (int x = myPosInTheLevel.getX(); x < offset.getX(); x++) {
			for (int z = myPosInTheLevel.getZ(); z < offset.getZ(); z++) {
				int pX = SectionPos.blockToSectionCoord(x);
				int pZ = SectionPos.blockToSectionCoord(z);
				BasicVerticalChunk vc = (BasicVerticalChunk) getChunk(pX, pZ, ChunkStatus.FULL, false);
				if (vc == null) continue;
				
				for (int y = myPosInTheLevel.getY(); y < offset.getY(); y++) {
					mutableBlockPos.set(x, y, z);
					vc.setBlockFast(mutableBlockPos, null, cache);
				}
			}
		}
	}
	
	@Override
	public void handleRemoval() {
		for (Entity entity : entities.toArray(new Entity[0])) {
			if (entity.isRemoved()) {
				entities.remove(entity);
			}
		}
	}
	
	@Override
	public void setBlockEntity(BlockEntity pBlockEntity) {
		LevelChunk chunk = this.getChunkAt(pBlockEntity.getBlockPos());
		pBlockEntity.worldPosition = chunk.getPos().getWorldPosition().offset(pBlockEntity.getBlockPos().getX() & 15, pBlockEntity.getBlockPos().getY(), pBlockEntity.getBlockPos().getZ() & 15);
		// TODO: figure out of deserialization and reserialization is necessary or not
		chunk.addAndRegisterBlockEntity(pBlockEntity);
	}
	
	@Override
	public void blockEntityChanged(BlockPos pPos) {
		BasicVerticalChunk vc = (BasicVerticalChunk) getChunk(pPos);
		BlockEntity be = vc.getBlockEntity(pPos);
		if (be == null) return;
		((BasicVerticalChunk) getChunkAt(pPos)).getSubChunk(pPos.getY() >> 4).setUnsaved(true);
		vc.beChanges.add(be);
		BlockPos parentPos = PositionUtils.getParentPosPrecise(pPos, vc);
		LevelChunk ac = getParent().getChunkAt(parentPos);
		ac.setUnsaved(true);
	}
	
	@Override
	public void tickChunk(LevelChunk pChunk, int pRandomTickSpeed) {
//		ChunkPos chunkpos = pChunk.getPos();
//		boolean flag = this.isRaining();
//		int i = chunkpos.getMinBlockX();
//		int j = chunkpos.getMinBlockZ();
//		ProfilerFiller profilerfiller = this.getProfiler();
//
//		profilerfiller.push("iceandsnow");
//		if (this.random.nextInt(16) == 0) {
//			BlockPos blockpos2 = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, this.getBlockRandomPos(i, 0, j, 15));
//			BlockPos blockpos3 = blockpos2.below();
//			Biome biome = this.getBiome(blockpos2).value();
//			if (this.isAreaLoaded(blockpos2, 1)) // Forge: check area to avoid loading neighbors in unloaded chunks
//				if (biome.shouldFreeze(this, blockpos3)) {
//					this.setBlockAndUpdate(blockpos3, Blocks.ICE.defaultBlockState());
//				}
//
//			if (flag) {
//				if (biome.shouldSnow(this, blockpos2)) {
//					this.setBlockAndUpdate(blockpos2, Blocks.SNOW.defaultBlockState());
//				}
//
//				BlockState blockstate1 = this.getBlockState(blockpos3);
//				Biome.Precipitation biome$precipitation = biome.getPrecipitation();
//				if (biome$precipitation == Biome.Precipitation.RAIN && biome.coldEnoughToSnow(blockpos3)) {
//					biome$precipitation = Biome.Precipitation.SNOW;
//				}
//
//				blockstate1.getBlock().handlePrecipitation(blockstate1, this, blockpos3, biome$precipitation);
//			}
//		}
//		profilerfiller.pop();
	}
	
	@Override
	public void playLocalSound(double pX, double pY, double pZ, SoundEvent pSound, SoundSource pCategory, float pVolume, float pPitch, boolean pDistanceDelay) {
		// TODO: does this even exist on server?
//		super.playLocalSound(pX, pY, pZ, pSound, pCategory, pVolume, pPitch, pDistanceDelay);
		double scl = 1f / upb;
		BlockPos pos = getRegion().pos.toBlockPos();
		pX *= scl;
		pY *= scl;
		pZ *= scl;
		pX += pos.getX();
		pY += pos.getY();
		pZ += pos.getZ();
		double finalPX = pX;
		double finalPY = pY;
		double finalPZ = pZ;
//		if (ResizingUtils.isResizingModPresent() && pPlayer != null) // TODO: I probably need to manually send sounds to each player
//			scl *= 1 / ResizingUtils.getSize(pPlayer);
		if (scl > 1) scl = 1 / scl;
		double finalScl = scl;
		completeOnTick.add(() -> {
			parent.get().playLocalSound(finalPX, finalPY, finalPZ, pSound, pCategory, (float) (pVolume * finalScl), pPitch, pDistanceDelay);
		});
	}
	
	protected BlockHitResult runTrace(VoxelShape sp, ClipContext pContext, BlockPos pos) {
		BlockHitResult result = sp.clip(pContext.getFrom(), pContext.getTo(), pos);
		if (result == null) return null;
		
		// improve precision
		if (!result.getType().equals(HitResult.Type.MISS)) {
			Vec3 off = pContext.getFrom().subtract(pContext.getTo());
			off = off.normalize().scale(0.5f);
			Vec3 hit = result.getLocation();
			return sp.clip(hit.add(off), hit.subtract(off), pos);
		}
		return result;
	}
	
	@Override
	public BlockHitResult clip(ClipContext pContext) {
		Collection<AABB> singleton = Collections.singleton(new AABB(0, 0, 0, 1, 1, 1));
		
		HashMap<BlockPos, BlockState> localCache = new HashMap<>();
		
		Level parent = getParent();
		
		return Math3d.traverseBlocks(
				pContext.getFrom(),
				pContext.getTo(),
				this,
				(pos, state) -> {
					if (state.isAir())
						return null;
					
					VoxelShape sp = switch (pContext.block) {
						case VISUAL -> state.getVisualShape(this, pos, pContext.collisionContext);
						case COLLIDER -> state.getCollisionShape(this, pos, pContext.collisionContext);
						case OUTLINE -> state.getShape(this, pos, pContext.collisionContext);
						default -> state.getCollisionShape(this, pos, pContext.collisionContext); // TODO
					};
					
					if (sp.isEmpty())
						return null;
					
					BlockHitResult result = runTrace(sp, pContext, pos);
					if (result != null && result.getType() != HitResult.Type.MISS) return result;
					if (pContext.fluid.canPick(state.getFluidState()))
						result = runTrace(state.getFluidState().getShape(this, pos), pContext, pos);
					return result;
				},
				(pos) -> {
					if (parent == null) return null;
					
					BlockPos pos1 = PositionUtils.getParentPos(pos, this);
					
					BlockState state = localCache.get(pos1);
					if (state == null) {
						state = parent.getBlockState(pos1);
						localCache.put(pos1.immutable(), state);
					}
					if (state.isAir()) return null;
					if (state.getBlock() instanceof UnitSpaceBlock) return null;
					
					BlockHitResult result = AABB.clip(
							singleton,
							pContext.getFrom(), pContext.getTo(),
							pos
					);
					
					return result;
				},
				() -> {
					Vec3 vec3 = pContext.getFrom().subtract(pContext.getTo());
					return BlockHitResult.miss(pContext.getTo(), Direction.getNearest(vec3.x, vec3.y, vec3.z), new BlockPos((int) pContext.getTo().x, (int) pContext.getTo().y, (int) pContext.getTo().z));
				}
		);
	}
	
	public void setLoaded() {
//		isLoaded = true;
//		lookupTemp = pos -> {
//			BlockPos bp = region.pos.toBlockPos().offset(
//					// TODO: double check this
//					Math.floor(pos.getX() / (double) upb),
//					Math.floor(pos.getY() / (double) upb),
//					Math.floor(pos.getZ() / (double) upb)
//			);
//			if (cache.containsKey(bp)) {
////				BlockState state = cache.get(bp).getFirst();
////				VoxelShape shape = state.getCollisionShape(parent, bp);
//				// TODO: empty shape check
//				return cache.get(bp).getFirst();
//			}
////			if (!parent.get().isLoaded(bp)) // TODO: check if there's a way to do this which doesn't cripple the server
////				return Blocks.VOID_AIR.defaultBlockState();
////			ChunkPos cp = new ChunkPos(bp);
////			if (parent.get().getChunk(cp.x, cp.z, ChunkStatus.FULL, false) == null)
////				return Blocks.VOID_AIR.defaultBlockState();
//			if (!getServer().isReady())
//				return Blocks.VOID_AIR.defaultBlockState();
//			BlockState state = parent.get().getBlockState(bp);
////			if (state.equals(Blocks.VOID_AIR.defaultBlockState()))
////				return state;
//			cache.put(bp, Pair.of(state, new VecMap<>(2)));
//			return state;
//		};
	}
	
	@Override
	public void invalidateCache(BlockPos pos) {
		cache.remove(pos);
	}
	
	public int getUnitsPerBlock() {
		return upb;
	}
	
	// yes, this is necessary
	// no, I don't know why java is like this
	public class EntityCallbacks extends ServerLevel.EntityCallbacks {
		public EntityCallbacks() {
		}
	}
	
	@Override
	public String toString() {
		Level parent = getParent();
		Region region = this.region;
		if (parent == null || region == null) {
			Loggers.SU_LOGGER.warn("toString called before SU world is initialized");
			return "TickerServerLevel[UNKNOWN]@[UNKNOWN]";
		}
		
		return "TickerServerLevel[" + getParent() + "]@[" + region.pos.x + "," + region.pos.y + "," + region.pos.z + "]";
	}
	
	@Override
	public BlockState getBlockState(BlockPos pPos) {
		LevelChunk chunk = getChunkAtNoLoad(pPos);
		if (chunk == null) {
			BlockPos parentPos = PositionUtils.getParentPos(pPos, this);
			BlockState parentState = lookup.getState(parentPos);
			if (parentState.isAir() || parentState.getBlock() instanceof UnitSpaceBlock) {
				return Blocks.VOID_AIR.defaultBlockState();
			}
			
			boolean transparent = true;
			Level lvl = this.getParent();
			if (parentState.isCollisionShapeFullBlock(lvl, parentPos))
				transparent = false;
			
			return tfc.smallerunits.core.Registry.UNIT_EDGE.get().defaultBlockState().setValue(UnitEdge.TRANSPARENT, transparent);
		}
		return chunk.getBlockState(new BlockPos(pPos.getX() & 15, pPos.getY(), pPos.getZ() & 15));
	}
	
	@Override
	public FluidState getFluidState(BlockPos pPos) {
//		return Blocks.AIR.defaultBlockState().getFluidState();
		LevelChunk chunk = getChunkAtNoLoad(pPos);
		if (chunk == null) return Fluids.EMPTY.defaultFluidState();
		return chunk.getFluidState(new BlockPos(pPos.getX() & 15, pPos.getY(), pPos.getZ() & 15));
	}
	
	@Override
	public ChunkAccess getChunk(int x, int y, int z, ChunkStatus pRequiredStatus, boolean pLoad) {
		ITickerChunkCache chunkCache = (ITickerChunkCache) getChunkSource();
		return chunkCache.getChunk(x, y, z, pRequiredStatus, pLoad);
	}
	
	@Override
	// nothing to do
	public void markRenderDirty(BlockPos pLevelPos) {
	}
	
	@Override
	public void advanceWeatherCycle() {
	}
	
	@Override
	public int getBrightness(LightLayer pLightType, BlockPos pBlockPos) {
		BlockPos parentPos = PositionUtils.getParentPos(pBlockPos, this);
		int lt = parent.get().getBrightness(pLightType, parentPos);
		if (pLightType.equals(LightLayer.SKY)) return lt;
		return Math.max(lt, super.getBrightness(pLightType, pBlockPos));
	}
	
	@Override
	public void tick(BooleanSupplier pHasTimeLeft) {
		if (upb == 0) return;
		
		Level parent = this.parent.get();
		if (parent == null) return;
		// compensate for create creating a level
		ThreadLocals.levelLocal.set(parent);

		PlatformProvider.UTILS.preTick(this, pHasTimeLeft);
		
		randomTickCount = Integer.MIN_VALUE;
		
		if (!isLoaded) return;
		if (!getServer().isReady()) return;
		
		NetworkingHacks.setPos(getDescriptor());
		
		resetEmptyTime();
		super.tick(pHasTimeLeft);
		getChunkSource().pollTask();
		
		// TODO: optimize this
		HashMap<ServerPlayer, PositionalInfo> infoMap = new HashMap<>();
		for (ChunkHolder holder : ((TickerChunkCache) chunkSource).holders) {
			List<ServerPlayer> players = null;
			if (holder.getTickingChunk() instanceof BasicVerticalChunk basicVerticalChunk) {
				if (players == null) {
					players = getChunkSource().chunkMap.getPlayers(basicVerticalChunk.getPos(), false);
					for (ServerPlayer player : players) {
						// TODO: do this properly
						try {
							PositionalInfo info = infoMap.get(player);
							if (info == null) {
								info = new PositionalInfo(player);
								infoMap.put(player, info);
								info.adjust(player, this, this.getUPB(), region.pos);
							}
							getChunkSource().chunkMap.move(player);
						} catch (Throwable ignored) {
						}
					}
				}
				
				for (BlockPos pos : basicVerticalChunk.besRemoved) {
					BlockEntity be = basicVerticalChunk.getBlockEntity(pos);
					if (be != null && !be.isRemoved())
						be.setRemoved();
				}
				basicVerticalChunk.beChanges.clear();
			}
		}
		for (Map.Entry<ServerPlayer, PositionalInfo> serverPlayerPositionalInfoEntry : infoMap.entrySet()) {
			serverPlayerPositionalInfoEntry.getValue().reset(serverPlayerPositionalInfoEntry.getKey());
		}
		
		NetworkingHacks.unitPos.remove();
		
		getLightEngine().runLightUpdates();
		for (Runnable runnable : completeOnTick) runnable.run();
		completeOnTick.clear();
		
		for (List<Entity> entitiesGrabbedByBlock : entitiesGrabbedByBlocks)
			for (Entity entity : entitiesGrabbedByBlock)
				((EntityAccessor) entity).setMotionScalar(1);
		entitiesGrabbedByBlocks.clear();
		
		saveWorld.tick();

		PlatformProvider.UTILS.postTick(this, pHasTimeLeft);
		
		ThreadLocals.levelLocal.remove();
	}
	
	@Override
	public int randomTickCount() {
		if (randomTickCount == Integer.MIN_VALUE)
			randomTickCount = parent.get().getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
		return randomTickCount;
	}
	
	@Override
	public int getHeight(Heightmap.Types pHeightmapType, int pX, int pZ) {
		return getMaxBuildHeight(); // TODO: do this properly
	}
	
	ArrayList<List<Entity>> entitiesGrabbedByBlocks = new ArrayList<>();
	
	@Override
	public <T extends Entity> List<? extends T> getEntities(EntityTypeTest<Entity, T> p_143281_, Predicate<? super T> p_143282_) {
		// TODO
		return super.getEntities(p_143281_, p_143282_);
	}
	
	@Override
	public List<Entity> getEntities(@Nullable Entity pEntity, AABB pBoundingBox, Predicate<? super Entity> pPredicate) {
		// for simplicity
		return getEntities(EntityTypeTest.forClass(Entity.class), pBoundingBox, pPredicate);
	}
	
	@Override
	public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> pEntityTypeTest, AABB aabb, Predicate<? super T> pPredicate) {
		Level owner = parent.get();
		if (owner != null) {
			List<T> entities = super.getEntities(pEntityTypeTest, aabb, pPredicate);
			
			double upb = this.upb;
			AABB aabb1 = new AABB(0, 0, 0, aabb.getXsize() / upb, aabb.getYsize() / upb, aabb.getZsize() / upb);
			AABB bb = aabb1.move(
					aabb.minX / upb,
					aabb.minY / upb,
					aabb.minZ / upb
			).move(region.pos.toBlockPos().getX(), region.pos.toBlockPos().getY(), region.pos.toBlockPos().getZ());
			// TODO: this is bugged for some reason
			List<T> parentEntities = owner.getEntities(pEntityTypeTest, bb, pPredicate);
			// scuffed solution to a ridiculous problem
			try {
				for (ServerPlayer player : ((ServerLevel) owner).getPlayers((Predicate<? super ServerPlayer>) pPredicate)) {
					T t = pEntityTypeTest.tryCast(player);
					if (t != null) {
						if (t.getBoundingBox().intersects(bb)) {
							if (!parentEntities.contains(t)) {
								parentEntities.add(t);
							}
						}
					}
				}
			} catch (Throwable ignored) {
			}
			
			for (Entity interactingEntity : interactingEntities) {
				if (interactingEntity.getBoundingBox().intersects(aabb)) {
					T ent = pEntityTypeTest.tryCast(interactingEntity);
					if (ent != null) {
						if (pPredicate.test(ent)) {
							if (!parentEntities.contains(ent)) {
								parentEntities.add(ent);
							}
						}
					}
				}
			}
			
			if (!parentEntities.isEmpty()) {
				entitiesGrabbedByBlocks.add((List<Entity>) parentEntities);
				for (T parentEntity : parentEntities)
					((EntityAccessor) parentEntity).setMotionScalar((float) (1 / upb));
			}
			
			entities.addAll(parentEntities);
			return entities;
		}
		
		return super.getEntities(pEntityTypeTest, aabb, pPredicate);
	}
	
	@Override
	public void ungrab(Player entitiesOfClass) {
		for (List<Entity> entitiesGrabbedByBlock : entitiesGrabbedByBlocks) {
			((EntityAccessor) entitiesOfClass).setMotionScalar(1);
			entitiesGrabbedByBlock.remove(entitiesOfClass);
		}
	}
	
	// compat: lithium
	// reason: un-inline
	public int getSectionYFromSectionIndex(int p_151569_) {
		return p_151569_;
	}
	
	// compat: lithium
	// reason: un-inline
	@Override
	public boolean isOutsideBuildHeight(int pY) {
		Level parent = this.parent.get();
		if (parent == null) return true;
		int yo = Math1D.getChunkOffset(pY, upb);
		yo = region.pos.toBlockPos().getY() + yo;
		return parent.isOutsideBuildHeight(yo);
	}
	
	// compat: lithium
	// reason: un-inline
	@Override
	public boolean isOutsideBuildHeight(BlockPos pos) {
		Level parent = this.parent.get();
		if (parent == null) return true;
		int yo = Math1D.getChunkOffset(pos.getY(), upb);
		yo = region.pos.toBlockPos().getY() + yo;
		return parent.isOutsideBuildHeight(yo);
	}
	
	@Override
	public int getMinBuildHeight() {
		return -32;
	}
	
	@Override
	public int getMaxBuildHeight() {
		return upb * 512 + 32;
	}
	
	@Override
	public boolean chunkExists(SectionPos pos) {
		return saveWorld.chunkExists(pos);
	}
	
	public abstract CapabilityWrapper getCaps();

//	/* redstone */
//	@Override
//	public int getDirectSignalTo(BlockPos pPos) {
//		return super.getDirectSignalTo(pPos);
//	}
//
//	@Override
//	public boolean hasSignal(BlockPos pPos, Direction pSide) {
//		return super.hasSignal(pPos, pSide);
//	}
//
//	@Override
//	public int getSignal(BlockPos pPos, Direction pFacing) {
//		BlockState blockstate = this.getBlockState(pPos);
//		int i = blockstate.getSignal(this, pPos, pFacing);
//
//		int v = blockstate.shouldCheckWeakPower(this, pPos, pFacing) ? Math.max(i, this.getDirectSignalTo(pPos)) : i;
//
////		BlockPos r = pPos.relative(pFacing);
////		BlockState state = getBlockState(r);
////		if (state.getBlock() == tfc.smallerunits.core.Registry.UNIT_EDGE.get()) {
////			Level parent = this.parent.get();
////			if (parent == null) return 0;
////			BlockPos parentPos = PositionUtils.getParentPos(pPos, this);
////			BlockState bs = lookup.getState(parentPos);
////			return bs.shouldCheckWeakPower(parent, parentPos, pFacing) ? Math.max(v, parent.getDirectSignalTo(parentPos)) : v;
////		}
//
//		return v;
//	}
//
//	@Override
//	public boolean hasNeighborSignal(BlockPos pPos) {
//		return super.hasNeighborSignal(pPos);
//	}
//
//	@Override
//	public int getBestNeighborSignal(BlockPos pPos) {
//		return super.getBestNeighborSignal(pPos);
//	}
//
//	@Override
//	public int getDirectSignal(BlockPos pPos, Direction pDirection) {
//		BlockPos r = pPos.relative(pDirection);
//		BlockState state = getBlockState(r);
//		if (state.getBlock() == tfc.smallerunits.core.Registry.UNIT_EDGE.get()) {
//			Level parent = this.parent.get();
//			if (parent == null) return 0;
//			BlockPos parentPos = PositionUtils.getParentPos(pPos, this);
//			BlockState bs = lookup.getState(parentPos);
//			return bs.getDirectSignal(parent, parentPos, pDirection);
//		}
//		return getBlockState(pPos).getDirectSignal(this, pPos, pDirection);
//	}
	
	protected void broadcast(Packet<?> packet, BlockPos pos) {
		BlockPos parentPos = PositionUtils.getParentPos(pos, this);
		for (Player player : parent.get().players()) {
			if (
					player.distanceToSqr(parentPos.getX(), parentPos.getY(), parentPos.getZ()) <
							(64) // TODO: scale this based off player scale and upb?
			) {
				((ServerPlayer) player).connection.send(packet);
			}
		}
	}
	
	protected void broadcast(
			Player exclude, Packet<?> packet,
			double x, double y, double z,
			double dist
	) {
		Vec3 parentPos = PositionUtils.getParentVec(new Vec3(x, y, z), this);
		for (Player player : parent.get().players()) {
			if (player != exclude) {
				if (
						player.distanceToSqr(parentPos.x, parentPos.y, parentPos.z) <
								(dist) // TODO: scale this based off player scale and upb?
				) {
					((ServerPlayer) player).connection.send(packet);
				}
			}
		}
	}
	
	public void globalLevelEvent(int p_8811_, BlockPos p_8812_, int p_8813_) {
		Level parent = getParent();
		if (parent != null) {
			parent.globalLevelEvent(p_8811_, p_8812_, p_8813_);
		}
	}
	
	public void levelEvent(@javax.annotation.Nullable Player p_8684_, int p_8685_, BlockPos p_8686_, int p_8687_) {
		broadcast(
				p_8684_,
				new ClientboundLevelEventPacket(p_8685_, p_8686_, p_8687_, false),
				p_8686_.getX(), p_8686_.getY(), p_8686_.getZ(),
				64.0D
		);
	}
	
	@Override
	public void runBlockEvents() {
		this.blockEventsToReschedule.clear();
		
		while (!this.blockEvents.isEmpty()) {
			BlockEventData blockeventdata = this.blockEvents.removeFirst();
			if (this.shouldTickBlocksAt(ChunkPos.asLong(blockeventdata.pos()))) {
				if (this.doBlockEvent(blockeventdata)) {
					ClientboundBlockEventPacket packet = new ClientboundBlockEventPacket(blockeventdata.pos(), blockeventdata.block(), blockeventdata.paramA(), blockeventdata.paramB());
					broadcast(packet, blockeventdata.pos());
				}
			} else {
				this.blockEventsToReschedule.add(blockeventdata);
			}
		}
		
		this.blockEvents.addAll(this.blockEventsToReschedule);
	}

	public static AbstractTickerServerLevel createServerLevel(MinecraftServer server, ServerLevelData data, ResourceKey<Level> p_8575_, DimensionType dimType, ChunkProgressListener progressListener, ChunkGenerator generator, boolean p_8579_, long p_8580_, List<CustomSpawner> spawners, boolean p_8582_, Level parent, int upb, IRegion region) {
		throw new RuntimeException("Check platform module self-impl mixins");
	}
}

package tfc.smallerunits.core.simulation.level.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import tfc.smallerunits.core.UnitEdge;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.UnitSpaceBlock;
import tfc.smallerunits.core.api.PositionUtils;
import tfc.smallerunits.core.client.access.tracking.SUCapableChunk;
import tfc.smallerunits.core.client.access.workarounds.ParticleEngineHolder;
import tfc.smallerunits.core.client.render.compat.UnitParticleEngine;
import tfc.smallerunits.core.data.access.EntityAccessor;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.core.data.capability.SUCapabilityManager;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.logging.Loggers;
import tfc.smallerunits.core.networking.hackery.NetworkingHacks;
import tfc.smallerunits.core.simulation.block.ParentLookup;
import tfc.smallerunits.core.simulation.chunk.BasicVerticalChunk;
import tfc.smallerunits.core.simulation.level.ITickerChunkCache;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.core.utils.AddOnlyList;
import tfc.smallerunits.core.utils.BreakData;
import tfc.smallerunits.core.utils.config.CommonConfig;
import tfc.smallerunits.core.utils.math.HitboxScaling;
import tfc.smallerunits.core.utils.math.Math1D;
import tfc.smallerunits.core.utils.math.Math3d;
import tfc.smallerunits.core.utils.storage.GroupMap;
import tfc.smallerunits.core.utils.storage.VecMap;
import tfc.smallerunits.plat.internal.ToolProvider;
import tfc.smallerunits.plat.util.PlatformProvider;
import tfc.smallerunits.plat.util.PlatformProviderClient;
import tfc.smallerunits.storage.IRegion;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ApiStatus.Internal
public class AbstractTickerClientLevel extends ClientLevel implements ITickerLevel, ParticleEngineHolder {
    public final Region region;
    public final int upb;
    public final GroupMap<Pair<BlockState, VecMap<VoxelShape>>> cache = new GroupMap<>(2);
    public ParentLookup lookup;
    WeakReference<ClientLevel> parent;

    @Override
    public ParticleEngine myEngine() {
        return particleEngine;
    }

    @Override
    public void setParticleEngine(ParticleEngine engine) {
    }

    List<Entity> interactingEntities = new AddOnlyList<>();
    ArrayList<List<Entity>> entitiesGrabbedByBlocks = new ArrayList<>();

    public void addInteractingEntity(Entity e) {
        if (!RenderSystem.isOnRenderThread()) {
            throw new RuntimeException("add interacting entity called from off thread");
        }

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

    private final ArrayList<Runnable> completeOnTick = new ArrayList<>();
    // if I do Minecraft.getInstance().getTextureManager(), it messes up particle textures
    UnitParticleEngine particleEngine = new UnitParticleEngine(this, new TextureManager(Minecraft.getInstance().getResourceManager()));

    public AbstractTickerClientLevel(ClientLevel parent, ClientPacketListener p_205505_, ClientLevelData p_205506_, ResourceKey<Level> p_205507_, Holder<DimensionType> p_205508_, int p_205509_, int p_205510_, Supplier<ProfilerFiller> p_205511_, LevelRenderer p_205512_, boolean p_205513_, long p_205514_, int upb, IRegion region) {
        super(p_205505_, p_205506_, p_205507_, p_205508_, p_205509_, p_205510_, p_205511_, p_205512_, p_205513_, p_205514_);
        this.parent = new WeakReference<>(parent);
        this.region = (Region) region;
        this.chunkSource = new TickerClientChunkCache(this, 0, upb);
        this.upb = upb;
        this.isClientSide = true;

        this.blockStatePredictionHandler = new BigWorldPredictionHandler();

        particleEngine.setLevel(this);


        ThreadLocal<WeakReference<LevelChunk>> lastChunk = new ThreadLocal<>();
        lookup = (pos) -> {
            Pair<BlockState, VecMap<VoxelShape>> value = cache.getOrDefault(pos, null);
            if (value != null) {
                // TODO: empty shape check
                return value.getFirst();
            }

            if (Minecraft.getInstance().level == null) return Blocks.VOID_AIR.defaultBlockState();

            ChunkPos ckPos = new ChunkPos(pos);
            WeakReference<LevelChunk> chunkRef = lastChunk.get();
            LevelChunk ck;
            if (chunkRef == null || (ck = chunkRef.get()) == null)
                lastChunk.set(new WeakReference<>(ck = this.parent.get().getChunkAt(pos)));
            else if (!chunkRef.get().getPos().equals(ckPos))
                lastChunk.set(new WeakReference<>(ck = this.parent.get().getChunkAt(pos)));

            BlockState state = ck.getBlockState(pos);
            cache.put(pos, Pair.of(state, new VecMap<>(2)));
            return state;
        };

        PlatformProviderClient.UTILS.onLoad(this);
    }

    public UnitParticleEngine getParticleEngine() {
        return particleEngine;
    }

    public HashMap<Integer, BreakData> breakStatus = new HashMap<>();

    @Override
    public void destroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress) {
        if (pProgress < 0) breakStatus.remove(pBreakerId);
        else {
            if (breakStatus.containsKey(pBreakerId))
                breakStatus.replace(pBreakerId, new BreakData(pPos, pProgress));
            else
                breakStatus.put(pBreakerId, new BreakData(pPos, pProgress));
        }
    }

    @Override
    public HashMap<Integer, BreakData> getBreakData() {
        return breakStatus;
    }

    @Override
    protected void finalize() throws Throwable {
        PlatformProvider.UTILS.unloadLevel(this);
        super.finalize();
    }

    @Override
    public void sendPacketToServer(Packet<?> pPacket) {
        NetworkingHacks.setPos(getDescriptor());
        parent.get().sendPacketToServer(pPacket);
        NetworkingHacks.unitPos.remove();
    }

    @Override
    public void disconnect() {
        NetworkingHacks.setPos(getDescriptor());
        parent.get().disconnect();
        NetworkingHacks.unitPos.remove();
    }

    // TODO: do this a bit more properly
    @Override
    public void addParticle(ParticleOptions pParticleData, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
        addAlwaysVisibleParticle(pParticleData, false, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    @Override
    public void addParticle(ParticleOptions pParticleData, boolean pForceAlwaysRender, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
        addAlwaysVisibleParticle(pParticleData, pForceAlwaysRender, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions pParticleData, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
        addAlwaysVisibleParticle(pParticleData, false, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    @Override
    public void addAlwaysVisibleParticle(ParticleOptions pParticleData, boolean pIgnoreRange, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
        Particle particle = particleEngine.createParticle(pParticleData, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        if (particle != null)
            particleEngine.add(particle);
    }

    // TODO: stuff that requires a level renderer
    @Override
    public void globalLevelEvent(int pId, BlockPos pPos, int pData) {
        // TODO
    }

    @Override
    public void levelEvent(@Nullable Player pPlayer, int pType, BlockPos pPos, int pData) {
//		System.out.println(pType);
        ClientLevel level = Minecraft.getInstance().level;
        ParticleEngine engine = Minecraft.getInstance().particleEngine;
        Minecraft.getInstance().level = this;
        Minecraft.getInstance().particleEngine = particleEngine;
        // TODO level renderer so I don't need this switch
        switch (pType) {
            case 1000 -> playLocalSound(pPos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            case 1001 -> playLocalSound(pPos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.2F, false);
            case 1002 -> playLocalSound(pPos, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.2F, false);
            case 1003 -> playLocalSound(pPos, SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
            case 1004 ->
                    playLocalSound(pPos, SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
            case 1005 ->
                    playLocalSound(pPos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
            case 1006 ->
                    playLocalSound(pPos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
            case 1007 ->
                    playLocalSound(pPos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
            case 1008 ->
                    playLocalSound(pPos, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
            case 1029 ->
                    playLocalSound(pPos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
            case 1030 ->
                    playLocalSound(pPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
            case 1031 ->
                    playLocalSound(pPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, random.nextFloat() * 0.1F + 0.9F, false);
            case 1011 ->
                    playLocalSound(pPos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
            case 1012 ->
                    playLocalSound(pPos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
            case 1013 ->
                    playLocalSound(pPos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
            case 1014 ->
                    playLocalSound(pPos, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
            case 1033 -> playLocalSound(pPos, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            case 1046 ->
                    playLocalSound(pPos, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.random.nextFloat() * 0.1F + 0.9F, false);
            case 1034 -> playLocalSound(pPos, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            case 1035 -> playLocalSound(pPos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            case 1036 ->
                    playLocalSound(pPos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
            case 1037 ->
                    playLocalSound(pPos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
            case 1039 ->
                    playLocalSound(pPos, SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 0.3F, random.nextFloat() * 0.1F + 0.9F, false);
            default -> {
                switch (pType) {
                    case 1505:
                        BoneMealItem.addGrowthParticles(this, pPos, pData);
                        playLocalSound(pPos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                        break;
                    case 1009:
                        if (pData == 0)
                            playLocalSound(pPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F, false);
                        else if (pData == 1)
                            playLocalSound(pPos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.7F, 1.6F + (random.nextFloat() - random.nextFloat()) * 0.4F, false);
                        break;
                    case 1504:
                        PointedDripstoneBlock.spawnDripParticle(this, pPos, getBlockState(pPos));
                        break;
                    case 1501:
                        playLocalSound(pPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F, false);

                        for (int i2 = 0; i2 < 8; ++i2) {
                            addParticle(ParticleTypes.LARGE_SMOKE, (double) pPos.getX() + random.nextDouble(), (double) pPos.getY() + 1.2D, (double) pPos.getZ() + random.nextDouble(), 0.0D, 0.0D, 0.0D);
                        }
                        break;
                    case 1502:
                        playLocalSound(pPos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F, false);

                        for (int l1 = 0; l1 < 5; ++l1) {
                            double d15 = (double) pPos.getX() + random.nextDouble() * 0.6D + 0.2D;
                            double d20 = (double) pPos.getY() + random.nextDouble() * 0.6D + 0.2D;
                            double d26 = (double) pPos.getZ() + random.nextDouble() * 0.6D + 0.2D;
                            addParticle(ParticleTypes.SMOKE, d15, d20, d26, 0.0D, 0.0D, 0.0D);
                        }
                        break;
                    case 1503:
                        playLocalSound(pPos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);

                        for (int k1 = 0; k1 < 16; ++k1) {
                            double d14 = (double) pPos.getX() + (5.0D + random.nextDouble() * 6.0D) / 16.0D;
                            double d19 = (double) pPos.getY() + 0.8125D;
                            double d25 = (double) pPos.getZ() + (5.0D + random.nextDouble() * 6.0D) / 16.0D;
                            addParticle(ParticleTypes.SMOKE, d14, d19, d25, 0.0D, 0.0D, 0.0D);
                        }
                        break;
                    case 2000:
                        Direction direction = Direction.from3DDataValue(pData);
                        int j1 = direction.getStepX();
                        int j2 = direction.getStepY();
                        int k2 = direction.getStepZ();
                        double d18 = (double) pPos.getX() + (double) j1 * 0.6D + 0.5D;
                        double d24 = (double) pPos.getY() + (double) j2 * 0.6D + 0.5D;
                        double d28 = (double) pPos.getZ() + (double) k2 * 0.6D + 0.5D;

                        for (int i3 = 0; i3 < 10; ++i3) {
                            double d4 = random.nextDouble() * 0.2D + 0.01D;
                            double d6 = d18 + (double) j1 * 0.01D + (random.nextDouble() - 0.5D) * (double) k2 * 0.5D;
                            double d8 = d24 + (double) j2 * 0.01D + (random.nextDouble() - 0.5D) * (double) j2 * 0.5D;
                            double d30 = d28 + (double) k2 * 0.01D + (random.nextDouble() - 0.5D) * (double) j1 * 0.5D;
                            double d9 = (double) j1 * d4 + random.nextGaussian() * 0.01D;
                            double d10 = (double) j2 * d4 + random.nextGaussian() * 0.01D;
                            double d11 = (double) k2 * d4 + random.nextGaussian() * 0.01D;
                            this.addParticle(ParticleTypes.SMOKE, d6, d8, d30, d9, d10, d11);
                        }
                        break;
                    case 2001:
                        BlockState blockstate = Block.stateById(pData);
                        if (!blockstate.isAir()) {
                            SoundType soundtype = PlatformProviderClient.UTILS.getSoundType(blockstate, this, pPos);
                            this.playLocalSound(pPos, soundtype.getBreakSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F, false);
                        }

                        this.addDestroyBlockEffect(pPos, blockstate);
                        break;
                    case 2009:
                        for (int j3 = 0; j3 < 8; ++j3)
                            this.addParticle(ParticleTypes.CLOUD, (double) pPos.getX() + random.nextDouble(), (double) pPos.getY() + 1.2D, (double) pPos.getZ() + random.nextDouble(), 0.0D, 0.0D, 0.0D);
                        break;
                    case 3000:
                        this.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, (double) pPos.getX() + 0.5D, (double) pPos.getY() + 0.5D, (double) pPos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
                        this.playLocalSound(pPos, SoundEvents.END_GATEWAY_SPAWN, SoundSource.BLOCKS, 10.0F, (1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F) * 0.7F, false);
                        break;
                    case 3001:
                        this.playLocalSound(pPos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 64.0F, 0.8F + this.random.nextFloat() * 0.3F, false);
                        break;
                    case 3002:
                        if (pData >= 0 && pData < Direction.Axis.VALUES.length) {
                            ParticleUtils.spawnParticlesAlongAxis(Direction.Axis.VALUES[pData], this, pPos, 0.125D, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(10, 19));
                        } else {
                            ParticleUtils.spawnParticlesOnBlockFaces(this, pPos, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(3, 5));
                        }
                        break;
                    case 3003:
                        ParticleUtils.spawnParticlesOnBlockFaces(this, pPos, ParticleTypes.WAX_ON, UniformInt.of(3, 5));
                        this.playLocalSound(pPos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                        break;
                    case 3004:
                        ParticleUtils.spawnParticlesOnBlockFaces(this, pPos, ParticleTypes.WAX_OFF, UniformInt.of(3, 5));
                        break;
                    case 3005:
                        ParticleUtils.spawnParticlesOnBlockFaces(this, pPos, ParticleTypes.SCRAPE, UniformInt.of(3, 5));
                        break;
                    default:
                        System.out.println("Unkown Level Event: " + pType);
                }
            }
        }
        Minecraft.getInstance().level = level;
        Minecraft.getInstance().particleEngine = engine;
    }

    @Override
    public void createFireworks(double pX, double pY, double pZ, double pMotionX, double pMotionY, double pMotionZ, @Nullable CompoundTag pCompound) {
        ParticleEngine engine = Minecraft.getInstance().particleEngine;
        Minecraft.getInstance().particleEngine = particleEngine;
        super.createFireworks(pX, pY, pZ, pMotionX, pMotionY, pMotionZ, pCompound);
        Minecraft.getInstance().particleEngine = engine;
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

    @Override
    public void setBlocksDirty(BlockPos pBlockPos, BlockState pOldState, BlockState pNewState) {
        // TODO
        BlockPos rp = region.pos.toBlockPos();
        int xo = ((pBlockPos.getX()) / upb);
        int yo = ((pBlockPos.getY()) / upb);
        int zo = ((pBlockPos.getZ()) / upb);
//		BlockPos parentPos = rp.offset(xo, yo, zo);
//		ChunkAccess ac;
//		ac = parent.get().getChunkAt(parentPos);
//
//		ISUCapability cap = SUCapabilityManager.getCapability((LevelChunk) ac);
//		UnitSpace space = cap.getUnit(parentPos);
//		if (space == null) {
//			space = cap.getOrMakeUnit(parentPos);
//			space.setUpb(upb);
//		}
    }

    @Override
    public void sendBlockUpdated(BlockPos pPos, BlockState pOldState, BlockState pNewState, int pFlags) {
        // TODO: check
        BlockEntity be = getBlockEntity(pPos);
        if (be != null) PlatformProviderClient.UTILS.updateModelData(this, be);

        ArrayList<BlockPos> positionsToRefresh = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos rp = region.pos.toBlockPos();
        for (int xOff = -1; xOff <= 1; xOff++) {
            for (int yOff = -1; yOff <= 1; yOff++) {
                for (int zOff = -1; zOff <= 1; zOff++) {
                    pos.set(pPos.getX() - xOff, pPos.getY() - yOff, pPos.getZ() - zOff);
                    int xo = Math1D.getChunkOffset(pos.getX(), upb);
                    int yo = Math1D.getChunkOffset(pos.getY(), upb);
                    int zo = Math1D.getChunkOffset(pos.getZ(), upb);
                    positionsToRefresh.add(rp.offset(xo, yo, zo));
                }
            }
        }

        for (BlockPos parentPos : positionsToRefresh) {
//			int xo = ((toRefresh.getX()) / upb);
//			int yo = ((toRefresh.getY()) / upb);
//			int zo = ((toRefresh.getZ()) / upb);
//			BlockPos parentPos = rp.offset(xo, yo, zo);
            ChunkAccess ac;
            ac = parent.get().getChunkAt(parentPos);
            if (ac != null) {
                ISUCapability cap = SUCapabilityManager.getCapability((LevelChunk) ac);
                if (cap != null) {
                    UnitSpace space = cap.getUnit(parentPos);
                    if (space != null) {
                        // TODO: do this on section dirty
                        ((SUCapableChunk) ac).SU$markDirty(parentPos);
                    }
                }
            }
        }
    }

//	@Override
//	public void setSectionDirtyWithNeighbors(int pSectionX, int pSectionY, int pSectionZ) {
//		super.setSectionDirtyWithNeighbors(pSectionX, pSectionY, pSectionZ);
//	}

    @Override
    public RecipeManager getRecipeManager() {
        return parent.get().getRecipeManager();
    }

    @Override
    public int getUPB() {
        return upb;
    }

    @Override
    public void handleRemoval() {
        // I don't remember what this is
    }

    @Nullable
    @Override
    public Entity getEntity(int pId) {
        return super.getEntity(pId);
    }

    @Override
    public Iterable<Entity> entitiesForRendering() {
        return getEntities().getAll();
    }

    @Override
    public void SU$removeEntity(Entity pEntity) {

    }

    @Override
    public void SU$removeEntity(UUID uuid) {

    }

    @Override
    public Iterable<Entity> getAllEntities() {
        return new ArrayList<>(); // TODO
    }

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

    @Override
    public RegistryAccess registryAccess() {
        // TODO: find a proper solution
        if (parent == null) parent = new WeakReference<>(Minecraft.getInstance().level);
        return parent.get().registryAccess();
    }

    @Override
    public Tag getTicksIn(BlockPos myPosInTheLevel, BlockPos offset) {
        return new CompoundTag();
    }

    @Override
    public void setLoaded() {
        // maybe TODO?
    }

    @Override
    public void loadTicks(CompoundTag ticks) {
        // nah, this don't exist client side
    }

    @Override
    public void tickTime() {
        // swap client level for IP compat
        ClientLevel tlevel = Minecraft.getInstance().level;
        Minecraft.getInstance().level = this;
        particleEngine.tick();
        Minecraft.getInstance().level = tlevel;

        PlatformProviderClient.UTILS.preTick(this);

        AABB box = HitboxScaling.getOffsetAndScaledBox(Minecraft.getInstance().player.getBoundingBox(), Minecraft.getInstance().player.position(), upb, region.pos);
        Vec3 vec = box.getCenter().subtract(0, box.getYsize() / 2, 0);
        BlockPos pos = new BlockPos((int) vec.x, (int) vec.y, (int) vec.z);
        this.animateTick(pos.getX(), pos.getY(), pos.getZ()); // TODO: this is borked

        try {
            for (Entity entity : entitiesForRendering()) {
                // TODO: remove null entities..?
                if (entity != null) {
                    entity.xOld = entity.position().x;
                    entity.yOld = entity.position().y;
                    entity.zOld = entity.position().z;
                    entity.yRotO = entity.getViewYRot(1);
                    entity.xRotO = entity.getViewXRot(1);
                    entity.tick();
                }
            }
        } catch (Throwable ignored) {
        }

        getLightEngine().runLightUpdates();
        for (Runnable runnable : completeOnTick) runnable.run();
        completeOnTick.clear();

        for (List<Entity> entitiesGrabbedByBlock : entitiesGrabbedByBlocks)
            for (Entity entity : entitiesGrabbedByBlock)
                ((EntityAccessor) entity).setMotionScalar(1);
        entitiesGrabbedByBlocks.clear();

        PlatformProviderClient.UTILS.postTick(this);
    }

    @Override
    public void doAnimateTick(int pPosX, int pPosY, int pPosZ, int pRange, RandomSource pRandom, @Nullable Block pBlock, BlockPos.MutableBlockPos pBlockPos) {
        if (pPosX < 0 || pPosY < 0 || pPosZ < 0) return;
        if (pPosX >= (upb * 16) || pPosZ >= (upb * 16) || (pPosY / 16) > upb) return;
//		super.doAnimateTick(pPosX, pPosY, pPosZ, pRange, pRandom, pBlock, pBlockPos);
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
        return upb + 4;
    }

    @Override
    public Holder<Biome> getBiome(BlockPos p_204167_) {
        Registry<Biome> reg = registryAccess().registry(Registries.BIOME).get();
        return reg.getHolderOrThrow(Biomes.THE_VOID);
    }

    @Override
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
    public void setFromSync(ChunkPos cp, int cy, int x, int y, int z, BlockState state, ArrayList<BlockPos> positions, HashMap<SectionPos, ChunkAccess> chunkCache) {
        BlockPos parentPos = PositionUtils.getParentPos(new BlockPos(x, y, z), cp, 0, this);
        ChunkAccess ac;
        // vertical lookups shouldn't be too expensive
        SectionPos pos = SectionPos.of(parentPos);
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
        // TODO: mark lighting engine dirty

        ((SUCapableChunk) ac).SU$markDirty(parentPos);
    }

    @Override
    public void markRenderDirty(BlockPos pLevelPos) {
        BlockPos parentPos = PositionUtils.getParentPos(pLevelPos, this);
        ChunkAccess ac = parent.get().getChunkAt(parentPos);
        ((SUCapableChunk) ac).SU$markDirty(parentPos);
    }

    @Override
    public void invalidateCache(BlockPos pos) {
        cache.remove(pos);
    }

    @Override
    public String toString() {
        Level parent = getParent();
        Region region = this.region;
        if (parent == null || region == null) {
            Loggers.SU_LOGGER.warn("toString called before SU world is initialized");
            return "TickerClientLevel[UNKNOWN]@[UNKNOWN]";
        }

        return "TickerClientLevel[" + getParent() + "]@[" + region.pos.x + "," + region.pos.y + "," + region.pos.z + "]";
    }

    @Override
    public LevelChunk getChunkAt(BlockPos pPos) {
        return ((TickerClientChunkCache) this.getChunkSource()).getChunk(
                SectionPos.blockToSectionCoord(pPos.getX()),
                0,
                SectionPos.blockToSectionCoord(pPos.getZ()),
                ChunkStatus.FULL, true
        );
    }

    public LevelChunk getChunkAtNoLoad(BlockPos pPos) {
        return ((TickerClientChunkCache) this.getChunkSource()).getChunk(
                SectionPos.blockToSectionCoord(pPos.getX()),
                0,
                SectionPos.blockToSectionCoord(pPos.getZ()),
                ChunkStatus.FULL, false
        );
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
        LevelChunk chunk = getChunkAtNoLoad(pPos);
        if (chunk == null) return Fluids.EMPTY.defaultFluidState();
        return chunk.getFluidState(new BlockPos(pPos.getX() & 15, pPos.getY(), pPos.getZ() & 15));
    }

    @Override
    public void setBlockEntity(BlockEntity pBlockEntity) {
        LevelChunk chunk = this.getChunkAt(pBlockEntity.getBlockPos());
        pBlockEntity.worldPosition = chunk.getPos().getWorldPosition().offset(pBlockEntity.getBlockPos().getX() & 15, pBlockEntity.getBlockPos().getY(), pBlockEntity.getBlockPos().getZ() & 15);
        // TODO: figure out of deserialization and reserialization is necessary or not
        chunk.addAndRegisterBlockEntity(pBlockEntity);
    }

//	@Override
//	public void blockEntityChanged(BlockPos pPos) {
//		super.blockEntityChanged(pPos);
//		BasicVerticalChunk vc = (BasicVerticalChunk) getChunk(pPos);
//		BlockEntity be = vc.getBlockEntity(pPos);
//		if (be == null) return;
//		((BasicVerticalChunk) getChunkAt(pPos)).getSubChunk(pPos.getY() >> 4).setUnsaved(true);
//		vc.beChanges.add(be);
//		BlockPos parentPos = PositionUtils.getParentPosPrecise(pPos, vc);
//		LevelChunk ac = getParent().getChunkAt(parentPos);
//		ac.setUnsaved(true);
//	}

    @Override
    public ChunkAccess getChunk(int x, int y, int z, ChunkStatus pRequiredStatus, boolean pLoad) {
        ITickerChunkCache chunkCache = (ITickerChunkCache) getChunkSource();
        return chunkCache.getChunk(x, y, z, pRequiredStatus, pLoad);
    }

    public BlockPos lightCacheCenter = null;
    private int[] lightCache = new int[3 * 3 * 3];

    public void setupLightCache(BlockPos center) {
        if (center != null)
            Arrays.fill(lightCache, -1);
        this.lightCacheCenter = center;
    }

    @Override
    public int getBrightness(LightLayer pLightType, BlockPos pBlockPos) {
        BlockPos parentPos = PositionUtils.getParentPos(pBlockPos, this);
        int lt;

        // TODO: caching of light values
//		if (pLightType.equals(LightLayer.SKY)) {
//			if (lightCacheCenter != null) {
//				lt = parent.get().getBrightness(pLightType, parentPos);
//			} else {
//				lt = parent.get().getBrightness(pLightType, parentPos);
//			}
//		} else {
//			lt = parent.get().getBrightness(pLightType, parentPos);
//		}

        lt = parent.get().getBrightness(pLightType, parentPos);
        if (pLightType.equals(LightLayer.SKY)) return lt;
        return Math.max(lt, super.getBrightness(pLightType, pBlockPos));
    }

    @Override
    public int randomTickCount() {
        return 0;
    }

    @Override
    public int getHeight(Heightmap.Types pHeightmapType, int pX, int pZ) {
        return getMaxBuildHeight(); // TODO: do this properly
//		return 0;
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity pEntity, AABB pBoundingBox, Predicate<? super Entity> pPredicate) {
        // for simplicity
        return getEntities(EntityTypeTest.forClass(Entity.class), pBoundingBox, pPredicate);
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> pEntityTypeTest, AABB aabb, Predicate<? super T> pPredicate) {
        boolean onRender = RenderSystem.isOnRenderThread();

        Level owner = parent.get();
        if (owner != null) {
            List<T> entities = super.getEntities(pEntityTypeTest, aabb, pPredicate);

            double upb = this.upb;
            AABB aabb1 = new AABB(0, 0, 0, aabb.getXsize() / upb, aabb.getZsize() / upb, aabb.getZsize() / upb);
            AABB bb = aabb1.move(
                    aabb.minX / upb,
                    aabb.minY / upb,
                    aabb.minZ / upb
            ).move(region.pos.toBlockPos().getX(), region.pos.toBlockPos().getY(), region.pos.toBlockPos().getZ());
            // TODO: this is bugged for some reason
            List<T> parentEntities = owner.getEntities(pEntityTypeTest, bb, pPredicate);
            // scuffed solution to a ridiculous problem
            try {
                T t = pEntityTypeTest.tryCast(Minecraft.getInstance().player);
                if (t != null) {
                    if (t.getBoundingBox().intersects(bb)) {
                        if (!parentEntities.contains(t)) {
                            parentEntities.add(t);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            // if the game is not on the render thread, then doing this will cause problems
            if (onRender) {
                for (Entity interactingEntity : interactingEntities) {
                    if (CommonConfig.DebugOptions.crashOnNullInteracter) {
                        if (interactingEntity == null) {
                            throw new RuntimeException("???");
                        }
                    }

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
            }

            entitiesGrabbedByBlocks.add((List<Entity>) parentEntities);
            for (T parentEntity : parentEntities)
                ((EntityAccessor) parentEntity).setMotionScalar((float) (1 / upb));

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
        return p_151569_ + this.getMinSection();
    }

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
        return false;
    }

    @Override
    public void playSound(
            double pX, double pY, double pZ,
            SoundEvent pSound, SoundSource pCategory,
            float pVolume, float pPitch,
            boolean pDistanceDelay, long p_233611_
    ) {
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
        if (ToolProvider.RESIZING.isResizingModPresent())
            scl *= 1 / ToolProvider.RESIZING.getSize(Minecraft.getInstance().cameraEntity);
        if (scl > 1) scl = 1 / scl;
        double finalScl = scl;
        completeOnTick.add(() -> {
            parent.get().playSound(
                    finalPX, finalPY, finalPZ,
                    pSound, pCategory,
                    (float) (pVolume * finalScl), pPitch,
                    pDistanceDelay, p_233611_
            );
        });
    }

    public static AbstractTickerClientLevel createClientLevel(ClientLevel parent, ClientPacketListener p_205505_, ClientLevel.ClientLevelData p_205506_, ResourceKey<Level> p_205507_, Holder<DimensionType> p_205508_, int p_205509_, int p_205510_, Supplier<ProfilerFiller> p_205511_, LevelRenderer p_205512_, boolean p_205513_, long p_205514_, int upb, IRegion region) {
        throw new RuntimeException("Check platform module self-impl mixins");
    }
}

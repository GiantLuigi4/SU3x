package tfc.smallerunits.plat.util;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tfc.smallerunits.common.UUIDs;
import tfc.smallerunits.level.SimpleTickerLevel;
import tfc.smallerunits.plat.internal.ToolProvider;
import tfc.smallerunits.plat.itf.CapabilityLike;
import tfc.smallerunits.plat.itf.ICullableBE;
import tfc.smallerunits.plat.itf.access.IPortaledEntity;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class FabricPlatformUtils extends PlatformUtils {
    public boolean isDevEnv() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    public boolean isLoaded(String mod) {
        return FabricLoader.getInstance().isModLoaded(mod);
    }

    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType().equals(EnvType.CLIENT);
    }

    public ResourceLocation getRegistryName(BlockEntity be) {
        return Registries.BLOCK_ENTITY_TYPE.registry();
    }

    public boolean shouldCaptureBlockSnapshots(Level level) {
        return false;
    }

    public double getStepHeight(LocalPlayer player) {
        return player.maxUpStep();
    }

    // config
    private final ArrayList<Runnable> toRun = new ArrayList<>();

    public void delayConfigInit(Runnable r) {
        if (hasConfigLib()) {
            if (r == null) {
                for (Runnable runnable : toRun) {
                    runnable.run();
                }
                toRun.clear();
                return;
            }

            toRun.add(r);
        }
    }

    private boolean hasConfigLib() {
        return isLoaded("cloth-config2");
    }

    // entity
    public PortalInfo createPortalInfo(Entity pEntity, Level lvl) {
        Vec3 pos = pEntity.getPosition(1);
        BlockPos bp = ((SimpleTickerLevel) lvl).getRegion().pos().toBlockPos();
        pos = pos.scale(1d / ((SimpleTickerLevel) lvl).getUPB());
        pos = pos.add(bp.getX(), bp.getY(), bp.getZ());
        return new PortalInfo(
                pos,
                pEntity.getDeltaMovement(),
                pEntity.getYRot(),
                pEntity.getXRot()
        );
    }

    public Entity migrateEntity(Entity pEntity, ServerLevel serverLevel, int upb, Level lvl) {
        ((IPortaledEntity) pEntity).setPortalInfo(createPortalInfo(pEntity, serverLevel));
        ToolProvider.RESIZING.resizeForUnit(pEntity, 1f / upb);
        Entity entity = pEntity.changeDimension((ServerLevel) lvl);
        ((IPortaledEntity) pEntity).setPortalInfo(null);

        return entity;
    }

    // block entity
    public void beLoaded(BlockEntity pBlockEntity, Level level) {
        if (level.isClientSide)
            ((FabricPlatformUtilsClient) PlatformProviderClient.UTILS).loadBe(pBlockEntity, level);
        else
            ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.invoker().onLoad(pBlockEntity, (ServerLevel) level);
    }

    public void dataPacket(BlockEntity be, CompoundTag tag) {
        be.load(tag);
        // TODO: event?
    }

    public <T extends BlockEntity> AABB getRenderBox(T pBlockEntity) {
        return ICullableBE.getCullingBB(pBlockEntity);
    }

    // events
    public void preTick(ServerLevel level, BooleanSupplier pHasTimeLeft) {
        ServerTickEvents.START_WORLD_TICK.invoker().onStartTick(level);
    }

    public void postTick(ServerLevel level, BooleanSupplier pHasTimeLeft) {
        ServerTickEvents.END_WORLD_TICK.invoker().onEndTick(level);
    }

    public void loadLevel(ServerLevel serverLevel) {
        ServerWorldEvents.LOAD.invoker().onWorldLoad(serverLevel.getServer(), (ServerLevel) serverLevel);
    }

    public void unloadLevel(Level level) {
        if (level instanceof ServerLevel) // TODO: split
            ServerWorldEvents.UNLOAD.invoker().onWorldUnload(level.getServer(), (ServerLevel) level);
    }

    public void chunkLoaded(LevelChunk bvci) {
        // TODO: is this a no-op
    }

    // reach
    public double getReach(LivingEntity entity, double reach) {
        AttributeInstance instance = entity.getAttribute(ReachEntityAttributes.REACH);
        if (instance == null) return reach;
        AttributeModifier modifier = instance.getModifier(UUIDs.SU_REACH_UUID);

        for (AttributeModifier instanceModifier : instance.getModifiers())
            if (instanceModifier.getOperation().equals(AttributeModifier.Operation.MULTIPLY_BASE))
                reach *= instanceModifier.getAmount();

        for (AttributeModifier instanceModifier : instance.getModifiers())
            if (instanceModifier.getOperation().equals(AttributeModifier.Operation.ADDITION))
                reach += instanceModifier.getAmount();

        for (AttributeModifier instanceModifier : instance.getModifiers())
            if (instanceModifier.getOperation().equals(AttributeModifier.Operation.MULTIPLY_TOTAL))
                if (!instanceModifier.equals(modifier))
                    reach *= instanceModifier.getAmount();

        if (modifier != null)
            reach *= modifier.getAmount();

        return reach;
    }

    public double getReach(LivingEntity entity) {
        return getReach(entity, 7);
    }

    public AttributeInstance getReachAttrib(LivingEntity livingEntity) {
        return livingEntity.getAttribute(ReachEntityAttributes.REACH);
    }

    // tabs
    public FabricSUTabBuilder tab(String name, Supplier<ItemStack> icon) {
        return new FabricSUTabBuilder(name, icon);
    }

//    private static void SU$fillItemCategory(CreativeModeTab.ItemDisplayParameters pItems, CreativeModeTab.Output output) {
//        for (int i = 2; i <= 16; i++) {
//            ItemStack stack = new ItemStack(UNIT_SPACE_ITEM.get());
//            stack.getOrCreateTag().putInt("upb", i);
//            output.accept(stack);
//        }
//    }

    public void customPayload(ClientboundCustomPayloadPacket clientboundCustomPayloadPacket, Object context, PacketListener listener) {
        clientboundCustomPayloadPacket.handle((ClientGamePacketListener) listener);
    }

    public void injectCrashReport(String smallerUnits, Supplier<String> o) {
        // TODO: is there a fabric impl of this?
    }

    public void updateModelData(ClientLevel level, BlockEntity be) {
    }

    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pPos) {
        return state.getLightEmission();
    }

    public boolean collisionExtendsVertically(BlockState blockstate, Level lvl, BlockPos blockpos1, Entity entity) {
        return blockstate.hasLargeCollisionShape(); // TODO: ?
    }

    public void startupWarning(String msg) {
        throw new RuntimeException();
    }

//		return (CapabilityLike) levelChunk.getCapability((Capability<?>) ToolProvider.CAPABILITY.get()).orElse(null);
    public CapabilityLike getSuCap(LevelChunk levelChunk) {
        return levelChunk.getComponent((ComponentKey<? extends CapabilityLike>) (ToolProvider.CAPABILITY.get()));
    }

    public CompoundTag chunkCapNbt(LevelChunk basicVerticalChunk) {
        return ((ComponentProvider) basicVerticalChunk).getComponentContainer().toTag(new CompoundTag());
    }

    public void readChunkCapNbt(LevelChunk shell, CompoundTag capabilities) {
        ((ComponentProvider) shell).getComponentContainer().fromTag(capabilities);
    }

    public Tag serializeEntity(Entity ent) {
        CompoundTag tag = new CompoundTag();
        ent.save(tag);
        return tag;
    }

    public boolean canRenderIn(BakedModel model, BlockState block, RandomSource randomSource, Object modelData, RenderType chunkBufferLayer) {
        return ItemBlockRenderTypes.getChunkRenderType(block).equals(chunkBufferLayer);
    }

    public void tesselate(BlockRenderDispatcher dispatcher, BlockAndTintGetter wld, BakedModel blockModel, BlockState block, BlockPos offsetPos, PoseStack stk, VertexConsumer consumer, boolean b, RandomSource randomSource, int i, int i1, Object modelData, RenderType chunkBufferLayer) {
        dispatcher.getModelRenderer().tesselateBlock(
                wld, dispatcher.getBlockModel(block),
                block, offsetPos, stk,
                consumer, true,
                randomSource,
                0, 0
        );
    }

    public boolean isFabric() {
        return true;
    }
}

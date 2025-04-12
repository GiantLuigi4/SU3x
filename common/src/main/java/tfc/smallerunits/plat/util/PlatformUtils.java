package tfc.smallerunits.plat.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import tfc.smallerunits.plat.itf.CapabilityLike;
import tfc.smallerunits.plat.itf.ICullableBE;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public abstract class PlatformUtils {
    public abstract boolean isDevEnv();

    public abstract boolean isLoaded(String mod);

    public abstract boolean isClient();

    public ResourceLocation getRegistryName(BlockEntity be) {
        return be.getLevel().registryAccess().registryOrThrow(Registries.BLOCK_ENTITY_TYPE).getKey(be.getType());
    }

//	 double getReach(LivingEntity entity, double reach) {
//		AttributeInstance instance = entity.getAttribute(ReachEntityAttributes.REACH);
//		if (instance == null) return reach;
//		AttributeModifier modifier = instance.getModifier(PositionalInfo.SU_REACH_UUID);
//
//		for (AttributeModifier instanceModifier : instance.getModifiers())
//			if (instanceModifier.getOperation().equals(AttributeModifier.Operation.MULTIPLY_BASE))
//				reach *= instanceModifier.getAmount();
//
//		for (AttributeModifier instanceModifier : instance.getModifiers())
//			if (instanceModifier.getOperation().equals(AttributeModifier.Operation.ADDITION))
//				reach += instanceModifier.getAmount();
//
//		for (AttributeModifier instanceModifier : instance.getModifiers())
//			if (instanceModifier.getOperation().equals(AttributeModifier.Operation.MULTIPLY_TOTAL))
//				if (!instanceModifier.equals(modifier))
//					reach *= instanceModifier.getAmount();
//
//		if (modifier != null)
//			reach *= modifier.getAmount();
//
//		return reach;
//	}
//
//	 double getReach(LivingEntity entity) {
//		return getReach(entity, 7);
//	}

    public abstract boolean shouldCaptureBlockSnapshots(Level level);

    public double getStepHeight(LocalPlayer player) {
        return player.maxUpStep();
    }

//	 CompoundTag getCapTag(Object level) {
//		if (level instanceof ComponentProvider provider)
//			return provider.getComponentContainer().toTag(new CompoundTag());
//		throw new RuntimeException(level + " is not a component provider.");
//	}
//
//	 void readCaps(Object level, CompoundTag tag) {
//		if (level instanceof ComponentProvider provider)
//			provider.getComponentContainer().fromTag(tag);
//		throw new RuntimeException(level + " is not a component provider.");
//	}

    // config
    private static final ArrayList<Runnable> toRun = new ArrayList<>();

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

    private static boolean hasConfigLib() {
        return true;
    }

    // entity
    public abstract PortalInfo createPortalInfo(Entity pEntity, Level lvl);

    public abstract Entity migrateEntity(Entity pEntity, ServerLevel serverLevel, int upb, Level lvl);

    // block entity
    public void beLoaded(BlockEntity pBlockEntity, Level level) {
//		if (level.isClientSide)
//			IHateTheDistCleaner.loadBe(pBlockEntity, level);
//		else
//			ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.invoker().onLoad(pBlockEntity, (ServerLevel) level);
    }

    public abstract void dataPacket(BlockEntity be, CompoundTag tag);

    public <T extends BlockEntity> AABB getRenderBox(T pBlockEntity) {
        return ICullableBE.getCullingBB(pBlockEntity);
    }

    // events
    public abstract void preTick(ServerLevel level, BooleanSupplier pHasTimeLeft);

    public abstract void postTick(ServerLevel level, BooleanSupplier pHasTimeLeft);

    public abstract void loadLevel(ServerLevel serverLevel);

    public abstract void unloadLevel(Level level);

    public abstract void chunkLoaded(LevelChunk bvci);

    // reach
    public abstract double getReach(LivingEntity entity, double reach);

    public double getReach(LivingEntity entity) {
        return getReach(entity, 7);
    }

    public abstract AttributeInstance getReachAttrib(LivingEntity livingEntity);

    // tabs
    public abstract SUTabBuilder tab(String name, Supplier<ItemStack> icon);

    public abstract void customPayload(ClientboundCustomPayloadPacket clientboundCustomPayloadPacket, Object context, PacketListener listener);

    public abstract void injectCrashReport(String smallerUnits, Supplier<String> o);

    public abstract int getLightEmission(BlockState state, BlockGetter level, BlockPos pPos);

    public abstract boolean collisionExtendsVertically(BlockState blockstate, Level lvl, BlockPos blockpos1, Entity entity);

    public abstract void startupWarning(String msg);

    public abstract CapabilityLike getSuCap(LevelChunk levelChunk);

    public abstract CompoundTag chunkCapNbt(LevelChunk basicVerticalChunk);

    public abstract void readChunkCapNbt(LevelChunk shell, CompoundTag capabilities);

    public Tag serializeEntity(Entity ent) {
        CompoundTag tag = new CompoundTag();
        ent.save(tag);
        return tag;
    }

    public abstract boolean canRenderIn(BakedModel model, BlockState block, RandomSource randomSource, Object modelData, RenderType chunkBufferLayer);

    public void tesselate(BlockRenderDispatcher dispatcher, BlockAndTintGetter wld, BakedModel blockModel, BlockState block, BlockPos offsetPos, PoseStack stk, VertexConsumer consumer, boolean b, RandomSource randomSource, int i, int i1, Object modelData, RenderType chunkBufferLayer) {
        dispatcher.getModelRenderer().tesselateBlock(
                wld, dispatcher.getBlockModel(block),
                block, offsetPos, stk,
                consumer, true,
                randomSource,
                0, 0
        );
    }

    public abstract boolean isFabric();
}

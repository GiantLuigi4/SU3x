package tfc.smallerunits.core.utils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.data.access.EntityAccessor;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.data.storage.RegionPos;
import tfc.smallerunits.core.data.tracking.RegionalAttachments;
import tfc.smallerunits.core.logging.Loggers;
import tfc.smallerunits.core.networking.hackery.NetworkingHacks;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.core.utils.config.CommonConfig;
import tfc.smallerunits.core.utils.math.HitboxScaling;
import tfc.smallerunits.plat.util.PlatformProvider;

import java.util.Random;
import java.util.UUID;

public class PositionalInfo {
	public final Vec3 pos;
	public final Level lvl;
	private final Level clientLevel; // immersive portals compat
	public final AABB box;
	public final float eyeHeight;
	public static final UUID SU_REACH_UUID = new UUID(new Random(847329).nextLong(), new Random(426324).nextLong());
	private boolean isReachSet = false;
	private Object particleEngine = null;
	public final Vec3 oPos;
	public final Vec3 oldPos;
	
	public PositionalInfo(Entity pEntity) {
		this(pEntity, true);
	}
	
	public PositionalInfo(Entity pEntity, boolean cacheParticleEngine) {
		pos = new Vec3(pEntity.getX(), pEntity.getY(), pEntity.getZ());
		lvl = pEntity.level();
		box = pEntity.getBoundingBox();
		eyeHeight = pEntity.eyeHeight;
		oPos = new Vec3(pEntity.xo, pEntity.yo, pEntity.zo);
		oldPos = new Vec3(pEntity.xOld, pEntity.yOld, pEntity.zOld);
		Level clvl = null;
		if (PlatformProvider.UTILS.isClient()) {
			if (pEntity.level().isClientSide) {
				if (pEntity instanceof Player player) {
					if (cacheParticleEngine) {
						particleEngine = IHateTheDistCleaner.getParticleEngine(player);
					}
					clvl = IHateTheDistCleaner.getClientLevel();
				}
			}
		}
		clientLevel = clvl;
	}
	
	public void scalePlayerReach(Player pPlayer, int upb) {
		AttributeInstance instance = PlatformProvider.UTILS.getReachAttrib(pPlayer);
		instance.removeModifier(SU_REACH_UUID);
		instance.addPermanentModifier(
				new AttributeModifier(SU_REACH_UUID, "su:reach", upb, AttributeModifier.Operation.MULTIPLY_TOTAL)
		);
		isReachSet = true;
	}
	
	public void adjust(Entity pEntity, UnitSpace space) {
		adjust(pEntity, space.getMyLevel(), space.unitsPerBlock, space.regionPos, true);
	}
	
	public void adjust(Entity pEntity, UnitSpace space, boolean updateParticleEngine) {
		adjust(pEntity, space.getMyLevel(), space.unitsPerBlock, space.regionPos, updateParticleEngine);
	}
	
	public void adjust(Entity pEntity, Level level, int upb, RegionPos regionPos) {
		adjust(pEntity, level, upb, regionPos, true);
	}
	
	public void adjust(Entity pEntity, Level parent, NetworkingHacks.LevelDescriptor descriptor, boolean server) {
		if (descriptor == null) {
			Loggers.SU_LOGGER.warn("Positional Info got a null descriptor in recursive adjust");
			return;
		}
		if (descriptor.parent() != null) adjust(pEntity, parent, descriptor.parent(), server);
		
		RegionalAttachments attachments = (RegionalAttachments) pEntity.level();
		Region region = attachments.SU$getRegion(descriptor.pos());
		if (region == null) return;
		Level spaceLevel;
		if (server)
			spaceLevel = region.getServerWorld(pEntity.getServer(), (ServerLevel) pEntity.level(), descriptor.upb());
		else
			spaceLevel = region.getClientWorld(pEntity.level(), descriptor.upb());
		
		adjust(pEntity, spaceLevel, descriptor.upb(), descriptor.pos(), false);
	}
	
	public void adjust(Entity pEntity, Level level, int upb, RegionPos regionPos, boolean updateParticleEngine) {
		if (pEntity == null) {
			if (CommonConfig.DebugOptions.crashOnNullInteracter)
				throw new RuntimeException("Positional info adjusting null");
			else return;
		}
		
		AABB scaledBB;
		pEntity.setBoundingBox(scaledBB = HitboxScaling.getOffsetAndScaledBox(pEntity.getBoundingBox(), pEntity.getPosition(1), upb, regionPos));
		pEntity.eyeHeight = (float) (this.eyeHeight * upb);
		((EntityAccessor) pEntity).setPosRawNoUpdate(scaledBB.getCenter().x, scaledBB.minY, scaledBB.getCenter().z);
		if (pEntity instanceof Player player)
			setupClient(player, level, updateParticleEngine);
		// TODO: fix this
		pEntity.xo = HitboxScaling.scaleX((ITickerLevel) level, pEntity.xo);
		pEntity.yo = HitboxScaling.scaleY((ITickerLevel) level, pEntity.yo);
		pEntity.zo = HitboxScaling.scaleZ((ITickerLevel) level, pEntity.zo);
		pEntity.xOld = HitboxScaling.scaleX((ITickerLevel) level, pEntity.xOld);
		pEntity.yOld = HitboxScaling.scaleY((ITickerLevel) level, pEntity.yOld);
		pEntity.zOld = HitboxScaling.scaleZ((ITickerLevel) level, pEntity.zOld);
		
		((EntityAccessor) pEntity).setLevel(level);
		ITickerLevel tkLvl = (ITickerLevel) pEntity.level();
		tkLvl.addInteractingEntity(pEntity);
	}
	
	public void reset(Entity pEntity) {
		if (pEntity.level() instanceof ITickerLevel tkLvl) {
			if (pEntity instanceof Player player) {
				tkLvl.ungrab(player);
			}
			tkLvl.removeInteractingEntity(pEntity);
		}
		
		if (isReachSet) {
			if (pEntity instanceof LivingEntity livingEntity) {
				AttributeInstance instance = PlatformProvider.UTILS.getReachAttrib(livingEntity);
				instance.removeModifier(SU_REACH_UUID);
				isReachSet = false;
			}
		}
		
		((EntityAccessor) pEntity).setLevel(lvl);
		if (pEntity.level().isClientSide) {
			if (pEntity instanceof Player player) {
				resetClient(player);
			}
		}
		pEntity.setBoundingBox(box);
		((EntityAccessor) pEntity).setPosRawNoUpdate(pos.x, pos.y, pos.z);
		pEntity.xOld = oldPos.x;
		pEntity.yOld = oldPos.y;
		pEntity.zOld = oldPos.z;
		pEntity.xo = oPos.x;
		pEntity.yo = oPos.y;
		pEntity.zo = oPos.z;
		pEntity.eyeHeight = eyeHeight;
	}
	
	public void resetClient(Player player) {
		if (PlatformProvider.UTILS.isClient()) {
			if (player.level().isClientSide) {
				IHateTheDistCleaner.resetClient(player, lvl, particleEngine);
				if (clientLevel != null) IHateTheDistCleaner.setClientLevel(clientLevel);
			}
		}
	}
	
	public void setupClient(Player player, Level spaceLevel, boolean updateParticleEngine) {
		if (PlatformProvider.UTILS.isClient()) {
			if (player.level().isClientSide) {
				Object o = IHateTheDistCleaner.adjustClient(player, spaceLevel, particleEngine != null);
			}
		}
	}
}

package tfc.smallerunits.forge.mixin.quality;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.data.storage.RegionPos;
import tfc.smallerunits.core.data.tracking.RegionalAttachments;
import tfc.smallerunits.core.utils.math.HitboxScaling;
import tfc.smallerunits.forge.asm.PlatformQol;
import tfc.smallerunits.level.SimpleTickerLevel;

import java.util.function.BiConsumer;

@Mixin(Entity.class)
public abstract class EntityQolForge {
	
	@Shadow
	protected Object2DoubleMap<FluidType> forgeFluidTypeHeight;
	@Shadow
	public Level level;
	
	@Shadow
	public abstract boolean touchingUnloadedChunk();
	
	@Shadow
	public abstract Vec3 getPosition(float f);
	
	@Shadow
	protected Object2DoubleMap<TagKey<Fluid>> fluidHeight;
	
	@Shadow
	public abstract float getEyeHeight();
	
	@Shadow
	private FluidType forgeFluidTypeOnEyes;
	
	@Shadow
	public abstract AABB getBoundingBox();

	@Shadow public abstract Vec3 position();

	// forge, casually rewriting the entirety of fluid logic
	@Inject(at = @At("RETURN"), method = "updateFluidHeightAndDoFluidPushing()V", remap = false)
	public void postCheckInFluid(CallbackInfo ci) {
		SU$runPerWorld((level, regionPos) -> {
			// TODO: optimize?
			PlatformQol.runSUFluidCheck((Entity) (Object) this, level, regionPos, fluidHeight, forgeFluidTypeHeight);
		});
	}
	
	@Inject(at = @At("RETURN"), method = "updateFluidOnEyes")
	public void postCheckFluidOnEyes(CallbackInfo ci) {
		SU$runPerWorld((level, pos) -> {
			if (!forgeFluidTypeOnEyes.isAir()) return;
			
			AABB aabb = HitboxScaling.getOffsetAndScaledBox(this.getBoundingBox().deflate(0.001D), this.getPosition(0), ((SimpleTickerLevel) level).getUPB(), pos);
			
			double d0 = (this.getEyeHeight()/* - (double) 0.11111111F*/) * ((SimpleTickerLevel) level).getUPB();
			BlockPos blockpos = new BlockPos((int) aabb.getCenter().x, (int) (aabb.minY + d0), (int) aabb.getCenter().z);
			FluidState fluidstate = level.getFluidState(blockpos);
			double d1 = ((float) blockpos.getY() + fluidstate.getHeight(level, blockpos));
			if (d1 > d0) {
				if (!fluidstate.isEmpty())
					this.forgeFluidTypeOnEyes = fluidstate.getFluidType();
			}
		});
	}
	
	@Unique
	private void SU$runPerWorld(BiConsumer<Level, RegionPos> action) {
		if (!(level instanceof RegionalAttachments)) return;
		if (touchingUnloadedChunk()) return;
		Vec3 position = getPosition(0);
		RegionPos regionPos = new RegionPos(new BlockPos((int) position.x, (int) position.y, (int) position.z));
		Region region = ((RegionalAttachments) level).SU$getRegionMap().get(regionPos);
		if (region != null) {
			for (Level regionLevel : region.getLevels()) {
				if (regionLevel != null) {
					action.accept(regionLevel, regionPos);
				}
			}
		}
	}
}

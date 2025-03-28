package tfc.smallerunits.core.mixin.quality;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.data.storage.RegionPos;
import tfc.smallerunits.core.data.tracking.RegionalAttachments;
import tfc.smallerunits.core.utils.asm.EntityQol;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Mixin(Entity.class)
public abstract class EntityQolMixin {
	@Shadow
	public Level level;
	
	@Shadow
	public abstract boolean touchingUnloadedChunk();
	
	@Shadow
	public abstract boolean isPushedByFluid();
	
	@Shadow
	public abstract AABB getBoundingBox();
	
	@Shadow
	protected Object2DoubleMap<TagKey<Fluid>> fluidHeight;
	@Shadow
	@Final
	private Set<TagKey<Fluid>> fluidOnEyes;
	
	@Shadow
	public abstract Vec3 getPosition(float pPartialTicks);
	
	@Shadow
	public abstract boolean isSprinting();
	
	@Shadow
	public abstract boolean isUnderWater();
	
	@Shadow
	public abstract boolean isPassenger();
	
	@Shadow
	public abstract boolean isSwimming();
	
	@Shadow
	public abstract void setSwimming(boolean pSwimming);
	
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
	
	@Unique
	private void SU$runPerWorldInterruptable(BiFunction<Level, RegionPos, Boolean> action) {
		if (!(level instanceof RegionalAttachments)) return;
		if (touchingUnloadedChunk()) return;
		Vec3 position = getPosition(0);
		RegionPos regionPos = new RegionPos(new BlockPos((int) position.x, (int) position.y, (int) position.z));
		Region region = ((RegionalAttachments) level).SU$getRegionMap().get(regionPos);
		if (region != null) {
			for (Level regionLevel : region.getLevels()) {
				if (regionLevel != null) {
					if (action.apply(regionLevel, regionPos)) {
						return;
					}
				}
			}
		}
	}
	
	@Inject(at = @At("RETURN"), method = "updateFluidOnEyes")
	public void postCheckEyeInFluid(CallbackInfo ci) {
		SU$runPerWorld((level, regionPos) -> {
			EntityQol.runSUFluidEyeCheck((Entity) (Object) this, fluidOnEyes, level, regionPos);
		});
	}
	
	@Inject(at = @At("RETURN"), method = "updateSwimming")
	public void postUpdateSwimming(CallbackInfo ci) {
		if (!this.isSwimming()) {
			if (isSprinting() && isUnderWater() && !isPassenger()) {
				boolean[] inWater = new boolean[]{false};
				SU$runPerWorldInterruptable((level, regionPos) -> {
					
					BlockState state = EntityQol.getSUBlockAtFeet((Entity) (Object) this, level, regionPos);
					if (state.getFluidState().is(FluidTags.WATER))
						return inWater[0] = true;
					return false;
				});
				if (inWater[0])
					setSwimming(true);
			}
		}
	}
	
	@Inject(at = @At("TAIL"), method = "isFree(Lnet/minecraft/world/phys/AABB;)Z", cancellable = true)
	public void postCheckFree(AABB pBb, CallbackInfoReturnable<Boolean> cir) {
		// TODO: do this better (truthfully, it should be done by the resizing mod)
		if (cir.getReturnValue()) {
			boolean[] out = new boolean[]{false};
			SU$runPerWorldInterruptable((level, regionPos) -> {
				out[0] = EntityQol.inAnyFluid(pBb, level, regionPos);
				return out[0];
			});
			if (out[0])
				cir.setReturnValue(false);
		}
	}
}

package tfc.smallerunits.core.mixin.quality;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.data.storage.RegionPos;
import tfc.smallerunits.core.data.tracking.RegionalAttachments;
import tfc.smallerunits.core.utils.asm.AssortedQol;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private boolean initialized;
	@Shadow
	private BlockGetter level;
	@Shadow
	private Vec3 position;
	
	@Shadow
	public abstract Camera.NearPlane getNearPlane();
	
	@Shadow
	public abstract Vec3 getPosition();
	
	@Unique
	private void SU$runPerWorld(BiConsumer<Level, RegionPos> action) {
		Vec3 position = getPosition();
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
	
	@Inject(at = @At("RETURN"), method = "getFluidInCamera", cancellable = true)
	public void postGetFluid(CallbackInfoReturnable<FogType> cir) {
		if (!initialized) return;
		
		Camera.NearPlane camera$nearplane = this.getNearPlane();
		List<Vec3> positions = Arrays.asList(position, camera$nearplane.getTopLeft(), camera$nearplane.getTopRight(), camera$nearplane.getBottomLeft(), camera$nearplane.getBottomRight());
		FogType vanilla = cir.getReturnValue();
		if (!vanilla.equals(FogType.NONE)) return;
		
		FogType[] out = new FogType[]{vanilla};
		SU$runPerWorld((level, regionPos) -> {
			if (!out[0].equals(FogType.NONE)) return;
			for (Vec3 position : positions) {
				out[0] = AssortedQol.getFogType(level, regionPos, position, this.position);
			}
		});
		
		if (out[0] != FogType.NONE)
			cir.setReturnValue(out[0]);
	}
}

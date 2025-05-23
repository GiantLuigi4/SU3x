package tfc.smallerunits.core.utils.vr;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfc.smallerunits.core.api.PositionUtils;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.core.utils.spherebox.Box;
import tfc.smallerunits.core.utils.spherebox.SphereBox;
import tfc.smallerunits.core.utils.spherebox.VecMath;
import tfc.smallerunits.core.utils.vr.player.SUVRPlayer;
import tfc.smallerunits.core.utils.vr.player.VRController;

import java.util.ArrayList;

public class ArmUtils {
	public static Box getArmBox(SUVRPlayer vrPlayer, InteractionHand controller) {
		if (vrPlayer == null) return null;
		VRController arm = vrPlayer.getHand(controller);
		
		Quaternionf quat = arm.getQuaternion();
		quat.conjugate();
		
		ArrayList<Vec3> points = new ArrayList<>();
		float sz = 0.05f;
		float len = 6;
		points.add(new Vec3(-sz, -sz, 0));
		points.add(new Vec3(sz, -sz, 0));
		points.add(new Vec3(-sz, sz, 0));
		points.add(new Vec3(-sz, -sz, sz * len));
		points.add(new Vec3(sz, -sz, sz * len));
		points.add(new Vec3(sz, sz, 0));
		points.add(new Vec3(-sz, sz, sz * len));
		points.add(new Vec3(sz, sz, sz * len));
		
		
		Vec3[] vecs = new Vec3[points.size()];
		Vector4f worker = new Vector4f();
		for (int i = 0; i < vecs.length; i++) {
			VecMath.rotate(points.get(i), quat, worker);
			vecs[i] = new Vec3(worker.x() * vrPlayer.worldScale, worker.y() * vrPlayer.worldScale, worker.z() * vrPlayer.worldScale);
		}
		
		quat.conjugate();
		
		return new Box(vecs, quat, worker, arm.getPosition());
	}
	
	public static void runPistonCheck(ArrayList<Box> bxs, Vector4f vec, ITickerLevel tkLvl, Level level, boolean isSmol, BlockPos pOriginPos, Direction pushDirection, ThreadLocal<BlockPos> bp, CallbackInfoReturnable<Boolean> cir) {
		if (isSmol) {
			if (bxs.isEmpty()) return;
			
			pOriginPos = bp.get();
			if (!level.isEmptyBlock(pOriginPos)) {
				Vec3 vec1 = PositionUtils.getParentVec(pOriginPos.relative(pushDirection), tkLvl);
				
				int scl = 10;
				float divisor = 1f / scl;
				float rad = divisor / 4;
				
				for (Box box : bxs) {
					if (SphereBox.intersects(vec, box, vec1, rad)) {
						cir.setReturnValue(false);
						return;
					}
				}
			}
		}
	}
}

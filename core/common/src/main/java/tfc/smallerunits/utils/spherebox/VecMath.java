package tfc.smallerunits.utils.spherebox;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public class VecMath {
	protected static final ThreadLocal<Quaternionf> point = ThreadLocal.withInitial(() -> new Quaternionf(0, 0, 0, 0));
	protected static final ThreadLocal<Quaternionf> newPoint = ThreadLocal.withInitial(() -> new Quaternionf(0, 0, 0, 0));
	
	public static void rotate(Vec3 src, Quaternionf quaternion, Vector4f dst) {
		Quaternionf point = VecMath.point.get();
		point.set((float) src.x, (float) src.y, (float) src.z, 0);
		Quaternionf newPoint = VecMath.newPoint.get();
		newPoint.set(quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w());
		point.mul(newPoint);
		newPoint.conjugate();
		newPoint.mul(point);
		
		dst.set(newPoint.x(), newPoint.y(), newPoint.z(), 0);
	}
}

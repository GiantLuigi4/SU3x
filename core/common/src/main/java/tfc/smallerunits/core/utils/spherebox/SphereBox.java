package tfc.smallerunits.core.utils.spherebox;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class SphereBox {
	private static final ThreadLocal<Vector3f> offset = ThreadLocal.withInitial(() -> new Vector3f(0, 0, 0));
	private static final ThreadLocal<Vector3f> localCenter = ThreadLocal.withInitial(() -> new Vector3f(0, 0, 0));
	
	public static boolean intersects(Vector4f worker, Box box, Vec3 point, float radius) {
		Vector3f offset = SphereBox.offset.get();
		Vector3f localCenter = SphereBox.localCenter.get();
		
		AABB bounds = box.getLsAABB(worker);
		
		localCenter.set(
				(float) (bounds.minX + bounds.maxX) / 2f,
				(float) (bounds.minY + bounds.maxY) / 2f,
				(float) (bounds.minZ + bounds.maxZ) / 2f
		);
		
		Quaternionf quaternion = box.quaternion;
		
		VecMath.rotate(point, quaternion, worker);
		
		offset.set(
				localCenter.x() - worker.x(),
				localCenter.y() - worker.y(),
				localCenter.z() - worker.z()
		);
		offset.normalize();
		offset.mul(radius);
		
		return bounds.contains(worker.x() + offset.x(), worker.y() + offset.y(), worker.z() + offset.z()) || bounds.contains(worker.x(), worker.y(), worker.z());
	}
}

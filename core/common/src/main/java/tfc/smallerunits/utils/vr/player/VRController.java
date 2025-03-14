package tfc.smallerunits.utils.vr.player;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class VRController {
	Vec3 position;
	Quaternionf quaternion;
	
	public VRController(Vec3 position, Quaternionf quaternion) {
		this.position = position;
		this.quaternion = quaternion;
	}
	
	public Vec3 getPosition() {
		return position;
	}
	
	public Quaternionf getQuaternion() {
		return quaternion.identity();
	}
}

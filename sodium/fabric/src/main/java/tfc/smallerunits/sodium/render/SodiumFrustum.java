package tfc.smallerunits.sodium.render;

import me.jellysquid.mods.sodium.client.render.viewport.frustum.Frustum;
import net.minecraft.world.phys.AABB;
import tfc.smallerunits.core.client.abstraction.IFrustum;

public class SodiumFrustum extends IFrustum {
	Frustum frustum;

	@Override
	public boolean test(AABB box) {
		if (frustum != null) {
			return frustum.testAab(
					(float) box.minX, (float) box.minY, (float) box.minZ,
					(float) box.maxX, (float) box.maxY, (float) box.maxZ
			);
		}
		return true;
	}

	public void set(Frustum frustum) {
		this.frustum = frustum;
	}
}

package tfc.smallerunits.data.access;

import net.minecraft.world.level.Level;

public interface EntityAccessor {
	void setLevel(Level level);
	void setPosRawNoUpdate(double pX, double pY, double pZ);
	void setMotionScalar(float scl);
}

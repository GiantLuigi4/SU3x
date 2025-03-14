package tfc.smallerunits.core.data.tracking;

import net.minecraft.world.phys.HitResult;
import tfc.smallerunits.core.utils.PositionalInfo;

public class RaytraceData {
	public final HitResult result;
	public final PositionalInfo info;
	
	public RaytraceData(HitResult result, PositionalInfo info) {
		this.result = result;
		this.info = info;
	}
}

package tfc.smallerunits.simulation.level;

import net.minecraft.world.level.portal.PortalInfo;
import tfc.smallerunits.data.storage.IRegion;

public interface ITickerLevel {
    IRegion getRegion();

    int getUPB();
}

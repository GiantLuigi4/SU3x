package tfc.smallerunits.level;

import tfc.smallerunits.storage.IRegion;

public interface SimpleTickerLevel {
    IRegion getRegion();

    int getUPB();
}

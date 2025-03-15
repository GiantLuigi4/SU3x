package tfc.smallerunits.resizing;

import tfc.smallerunits.plat.internal.IResizingUtil;
import tfc.smallerunits.plat.util.PlatformProvider;
import tfc.smallerunits.resizing.pehkui.PehkuiSupport;
import tfc.smallerunits.resizing.pehkui.PehkuiUtils;

public abstract class ResizingSetup {
    public static void init() {
        if (PlatformProvider.UTILS.isLoaded("pehkui")) {
            PehkuiSupport.setup();
        }
    }

    public static IResizingUtil makeUtils() {
        if (PlatformProvider.UTILS.isLoaded("pehkui")) {
            return new PehkuiUtils();
        }
        return null;
    }
}

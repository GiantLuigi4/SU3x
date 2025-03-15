package tfc.smallerunits.plat.mixin.self_impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import tfc.smallerunits.plat.config.AnnoCFG;
import tfc.smallerunits.plat.config.ForgeAnnoCFG;

@Mixin(value = AnnoCFG.class, remap = false)
public class AnnoCfgMixin {
    /**
     * @author GiantLuigi4
     */
    @Overwrite
    public static AnnoCFG of(Class<?> par1) {
        return new ForgeAnnoCFG(par1);
    }
}

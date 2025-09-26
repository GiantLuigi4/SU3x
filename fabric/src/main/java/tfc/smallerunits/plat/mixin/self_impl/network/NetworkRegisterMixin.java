package tfc.smallerunits.plat.mixin.self_impl.network;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import tfc.smallerunits.plat.net.FabricPacketRegister;
import tfc.smallerunits.plat.net.PacketRegister;

import java.util.function.Predicate;

@Mixin(value = PacketRegister.class, remap = false)
public class NetworkRegisterMixin {
    /**
     * @author GiantLuigi4
     */
    @Overwrite
    public static PacketRegister of(ResourceLocation par1, String par2, Predicate<String> par3, Predicate<String> par4) {
        return new FabricPacketRegister(
                par1, par2, par3, par4
        );
    }
}

package tfc.smallerunits.plat.mixin.self_impl.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import tfc.smallerunits.plat.net.ForgePacketRegister;
import tfc.smallerunits.plat.net.Packet;
import tfc.smallerunits.plat.net.PacketTarget;

import java.util.function.BiConsumer;

@Mixin(PacketTarget.class)
public class PacketTargets {
    @Shadow
    @Mutable
    public static PacketTarget SERVER;

    static {
        SERVER = new PacketTarget((pkt, register) -> {
            ((ForgePacketRegister) register).NETWORK_INSTANCE.sendToServer(pkt);
        });
    }

    /**
     * @author GiantLuigi4
     * @reason Implement method
     */
    @Overwrite
    public static PacketTarget trackingChunk(LevelChunk chunk) {
        return new PacketTarget((pkt, register) -> {
            ((ForgePacketRegister) register).NETWORK_INSTANCE.send(net.minecraftforge.network.PacketDistributor.TRACKING_CHUNK.with(() -> chunk), pkt);
        });
    }

    /**
     * @author GiantLuigi4
     * @reason Implement method
     */
    @Overwrite
    public static PacketTarget player(ServerPlayer player) {
        return new PacketTarget((pkt, register) -> {
            ((ForgePacketRegister) register).NETWORK_INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), pkt);
        });
    }
}

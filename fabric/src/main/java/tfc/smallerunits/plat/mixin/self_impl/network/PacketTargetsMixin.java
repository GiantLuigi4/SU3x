package tfc.smallerunits.plat.mixin.self_impl.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import tfc.smallerunits.plat.net.PacketTarget;

@Mixin(value = PacketTarget.class, remap = false)
public class PacketTargetsMixin {
    @Shadow
    @Mutable
    public static PacketTarget SERVER;

    static {
        SERVER = new PacketTarget((pkt, register) -> {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(register.channel, register.encode(pkt));
        });
    }

    /**
     * @author GiantLuigi4
     */
    @Overwrite
    public static PacketTarget trackingChunk(LevelChunk chunk) {
        return new PacketTarget((pkt, register) -> {
            ((net.minecraft.server.level.ServerChunkCache) chunk.getLevel().getChunkSource()).chunkMap.getPlayers(chunk.getPos(), false).forEach(
                    e -> net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(e, register.channel, register.encode(pkt))
            );
        });
    }

    /**
     * @author GiantLuigi4
     */
    @Overwrite
    public static PacketTarget player(ServerPlayer player) {
        return new PacketTarget((pkt, register) -> {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, register.channel, register.encode(pkt));
        });
    }
}

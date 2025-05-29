package tfc.smallerunits.plat.util;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public abstract class PlatformUtilsClient {
    public abstract void postTick(ClientLevel fakeClientLevel);

    public abstract void preTick(ClientLevel fakeClientLevel);

    public boolean checkRenderLayer(FluidState fluid, RenderType chunkBufferLayer) {
        return ItemBlockRenderTypes.getRenderLayer(fluid).equals(chunkBufferLayer);
    }

    public boolean checkRenderLayer(BlockState state, RenderType chunkBufferLayer) {
        return ItemBlockRenderTypes.getChunkRenderType(state).equals(chunkBufferLayer);
    }

    public abstract void onLoad(ClientLevel fakeClientLevel);

    public void handlePacketClient(ClientGamePacketListener packetListener, ClientboundCustomPayloadPacket clientboundCustomPayloadPacket) {
        clientboundCustomPayloadPacket.handle(packetListener);
    }

    public void recieveBeData(BlockEntity be, CompoundTag tag) {
        be.load(tag);
    }

    public abstract SoundType getSoundType(BlockState blockstate, ClientLevel tickerClientLevel, BlockPos pPos);

    public abstract ChunkRenderDispatcher.RenderChunk updateRenderChunk(ChunkRenderDispatcher.RenderChunk chunk);

    public abstract void updateModelData(ClientLevel level, BlockEntity be);

    public abstract Object populateModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, BakedModel model, Object modelData);
}

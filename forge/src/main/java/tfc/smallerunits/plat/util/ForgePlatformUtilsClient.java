package tfc.smallerunits.plat.util;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.LogicalSide;
import tfc.smallerunits.plat.internal.ToolProvider;

public class ForgePlatformUtilsClient extends PlatformUtilsClient {
	public void postTick(ClientLevel fakeClientLevel) {
		MinecraftForge.EVENT_BUS.post(new TickEvent.LevelTickEvent(LogicalSide.CLIENT, TickEvent.Phase.END, fakeClientLevel, () -> true));
	}
	
	public void preTick(ClientLevel fakeClientLevel) {
		MinecraftForge.EVENT_BUS.post(new TickEvent.LevelTickEvent(LogicalSide.CLIENT, TickEvent.Phase.START, fakeClientLevel, () -> true));
	}
	
	public boolean checkRenderLayer(FluidState fluid, RenderType chunkBufferLayer) {
		return ItemBlockRenderTypes.getRenderLayer(fluid).equals(chunkBufferLayer);
	}
	
	public boolean checkRenderLayer(BlockState state, RenderType chunkBufferLayer) {
		return ItemBlockRenderTypes.getChunkRenderType(state).equals(chunkBufferLayer);
	}
	
	public void onLoad(ClientLevel fakeClientLevel) {
		MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(fakeClientLevel));
	}
	
	public void handlePacketClient(ClientGamePacketListener packetListener, ClientboundCustomPayloadPacket clientboundCustomPayloadPacket) {
		clientboundCustomPayloadPacket.handle(packetListener);
	}
	
	public void recieveBeData(BlockEntity be, CompoundTag tag) {
		be.load(tag);
	}
	
	public SoundType getSoundType(BlockState blockstate, ClientLevel tickerClientLevel, BlockPos pPos) {
		return blockstate.getSoundType(tickerClientLevel, pPos, null);
	}
	
	public ChunkRenderDispatcher.RenderChunk updateRenderChunk(ChunkRenderDispatcher.RenderChunk chunk) {
		ToolProvider.currentRenderChunk.set(chunk);
		return chunk;
	}
}

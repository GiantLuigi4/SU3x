package tfc.smallerunits.plat.util;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import tfc.smallerunits.common.logging.Loggers;
import tfc.smallerunits.plat.internal.ToolProvider;
import tfc.smallerunits.plat.itf.IMayManageModelData;

public class FabricPlatformUtilsClient extends PlatformUtilsClient {
	public void postTick(ClientLevel fakeClientLevel) {
		// NO-OP
	}
	
	public void preTick(ClientLevel fakeClientLevel) {
		// NO-OP
	}
	
	public boolean checkRenderLayer(FluidState fluid, RenderType chunkBufferLayer) {
		return ItemBlockRenderTypes.getRenderLayer(fluid).equals(chunkBufferLayer);
	}
	
	public boolean checkRenderLayer(BlockState state, RenderType chunkBufferLayer) {
		return ItemBlockRenderTypes.getChunkRenderType(state).equals(chunkBufferLayer);
	}
	
	public void onLoad(ClientLevel fakeClientLevel) {
		// NO-OP?
	}
	
	public void handlePacketClient(ClientGamePacketListener packetListener, ClientboundCustomPayloadPacket clientboundCustomPayloadPacket) {
		clientboundCustomPayloadPacket.handle(packetListener);
	}
	
	public void recieveBeData(BlockEntity be, CompoundTag tag) {
		be.load(tag);
	}
	
	public SoundType getSoundType(BlockState blockstate, ClientLevel tickerClientLevel, BlockPos pPos) {
		return blockstate.getSoundType();
	}
	
	public void loadBe(BlockEntity pBlockEntity, Level level) {
		ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.invoker().onLoad(pBlockEntity, (ClientLevel) level);
	}
	
	public ChunkRenderDispatcher.RenderChunk updateRenderChunk(ChunkRenderDispatcher.RenderChunk chunk) {
		ToolProvider.currentRenderChunk.set(chunk);
		return chunk;
	}

	@Override
	public void updateModelData(ClientLevel level, BlockEntity be) {
		Loggers.SU_LOGGER.warn("Update model data NYI for fabric in FabricPlatformUtilsClient");
	}

	@Override
	public Object populateModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, BakedModel model, Object modelData) {
		return ((IMayManageModelData) level).getModelData(pos);
	}
}

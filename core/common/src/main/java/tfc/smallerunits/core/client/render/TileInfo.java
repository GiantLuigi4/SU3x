package tfc.smallerunits.core.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import tfc.smallerunits.core.client.render.storage.BufferStorage;

import java.util.List;

public class TileInfo {
    public final BlockPos pos;
    public boolean culled;
    public final BufferStorage storage;
    public final List<BlockEntity> blockEntities;

    public TileInfo(BlockPos pos, BufferStorage storage, List<BlockEntity> blockEntities) {
        this.pos = pos;
        this.culled = false;
        this.storage = storage;
        this.blockEntities = blockEntities;
    }
}

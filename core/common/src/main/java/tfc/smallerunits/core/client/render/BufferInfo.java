package tfc.smallerunits.core.client.render;

import net.minecraft.core.BlockPos;
import tfc.smallerunits.core.client.render.storage.BufferStorage;

public class BufferInfo {
    public final BlockPos pos;
    public boolean culled;
    public final BufferStorage storage;

    public BufferInfo(BlockPos pos, BufferStorage storage) {
        this.pos = pos;
        this.storage = storage;
    }
}

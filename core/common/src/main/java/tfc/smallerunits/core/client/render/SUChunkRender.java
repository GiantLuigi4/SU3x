package tfc.smallerunits.core.client.render;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import tfc.smallerunits.core.client.abstraction.IFrustum;
import tfc.smallerunits.core.client.render.storage.BufferStorage;
import tfc.smallerunits.core.utils.asm.ModCompatClient;
import tfc.smallerunits.core.utils.selection.MutableAABB;

import java.util.ArrayList;
import java.util.List;

public class SUChunkRender {
    private final LevelChunk chunk;
    final ArrayList<TileInfo> buffers = new ArrayList<>();
    boolean empty = true;
    boolean culled = false;
    boolean allDirty = false;

    public boolean isEmpty() {
        return empty;
    }

    public SUChunkRender(LevelChunk chunk) {
        this.chunk = chunk;
    }

    public void dirty() {
        allDirty = true;
    }

    public boolean isDirty() {
        return allDirty;
    }

    public void draw(RenderType type, AbstractUniform uniform) {
        if (!isEmpty()) {
            if (culled) return;

            ((Uniform) uniform).upload();

            for (TileInfo buffer : buffers) {
                if (buffer.culled)
                    continue;

                BufferStorage strg = buffer.storage;
                if (strg.hasActive(type)) {
                    VertexBuffer buffer1 = buffer.storage.getBuffer(type);
                    buffer1.bind();
                    buffer1.draw();
                }
            }
        }
    }

    public void addBuffers(
            BlockPos pos,
            List<BlockEntity> blockEntities,
            BufferStorage genBuffers
    ) {
        for (TileInfo buffer : buffers) {
            if (buffer.pos.equals(pos)) {
                buffers.remove(buffer);
                break;
            }
        }
        if (genBuffers != null) buffers.add(new TileInfo(
                pos, genBuffers, blockEntities
        ));

        empty = false;
    }

    public void freeBuffers(BlockPos pos, SUVBOEmitter emitter) {
        for (TileInfo buffer : buffers) {
            if (buffer.pos.equals(pos)) {
                buffers.remove(buffer);
                emitter.getAndMark(pos);

                empty = buffers.isEmpty();
                break;
            }
        }
    }

    public void performCull(MutableAABB mutableAABB, IFrustum frustum) {
        culled = true;
        for (TileInfo buffer : buffers) {
            buffer.culled = !frustum.test(mutableAABB.set(
                    buffer.pos.getX(),
                    buffer.pos.getY(),
                    buffer.pos.getZ(),
                    buffer.pos.getX() + 1,
                    buffer.pos.getY() + 1,
                    buffer.pos.getZ() + 1
            ));
            if (!buffer.culled) culled = false;
        }
    }

    public void drawBEs(
            BlockPos origin,
            PoseStack stk,
            IFrustum SU$Frustum,
            float pct,
            boolean sodium
    ) {
        if (!isEmpty()) {
            if (culled) return;

            stk.pushPose();
			if (!sodium)
				stk.translate(-origin.getX(), -origin.getY(), -origin.getZ());
            for (TileInfo buffer : buffers) {
                for (BlockEntity blockEntity : buffer.blockEntities) {
                    ModCompatClient.drawBE(
                            blockEntity, origin, SU$Frustum,
                            stk, pct
                    );
                }
            }
            stk.popPose();
        }
    }
}

package tfc.smallerunits.plat.internal;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.network.Connection;

import java.util.function.Function;
import java.util.function.Supplier;

public class ToolProvider {
    public static final IResizingUtil RESIZING = null;
    public static final Supplier<Object> CAPABILITY = null;
    public static final Function<Object, Connection> ACTIVE_CONTEXT = null; // ((tfc.smallerunits.core.networking.hackery.NetworkContext) context).connection
    public static ThreadLocal<ChunkRenderDispatcher.RenderChunk> currentRenderChunk = new ThreadLocal<>();
}

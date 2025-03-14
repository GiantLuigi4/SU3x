package tfc.smallerunits.core.mixin.data.access;

import com.mojang.blaze3d.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import tfc.smallerunits.core.client.access.VertexBufferAccessor;

@Mixin(VertexBuffer.class)
public abstract class VertexBufferMixin implements VertexBufferAccessor {
	@Shadow
	public abstract void bind();
	
	@Override
	public void invokeBindVAO() {
		bind();
	}
}

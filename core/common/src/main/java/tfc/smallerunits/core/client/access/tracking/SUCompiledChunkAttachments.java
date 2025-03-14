package tfc.smallerunits.core.client.access.tracking;

import tfc.smallerunits.core.client.render.SUChunkRender;

public interface SUCompiledChunkAttachments {
	SUCapableChunk getSUCapable();
	
	void setSUCapable(int yCoord, SUCapableChunk chunk);

	void markForCull();

	boolean needsCull();

	void markCulled();

	SUChunkRender SU$getChunkRender();
}

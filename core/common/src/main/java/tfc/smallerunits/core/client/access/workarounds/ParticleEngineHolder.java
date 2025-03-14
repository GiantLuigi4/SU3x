package tfc.smallerunits.core.client.access.workarounds;

import net.minecraft.client.particle.ParticleEngine;

public interface ParticleEngineHolder {
	ParticleEngine myEngine();
	
	void setParticleEngine(ParticleEngine engine);
}

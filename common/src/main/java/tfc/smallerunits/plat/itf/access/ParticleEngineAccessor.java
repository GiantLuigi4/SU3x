package tfc.smallerunits.plat.itf.access;

import net.minecraft.client.particle.ParticleEngine;

import java.util.Map;

public interface ParticleEngineAccessor {
    void copyProviders(ParticleEngine source);

    Map getProviders();
}

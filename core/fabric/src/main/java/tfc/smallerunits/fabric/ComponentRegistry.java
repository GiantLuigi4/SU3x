package tfc.smallerunits.fabric;

import dev.onyxstudios.cca.api.v3.chunk.ChunkComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.chunk.ChunkComponentInitializer;
import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import tfc.smallerunits.core.data.capability.SUCapability;

public class ComponentRegistry implements ChunkComponentInitializer {
    public static final ComponentKey<Component> SU_CAPABILITY_COMPONENT_KEY =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation("smallerunits:unit_space_cap"), (Class<Component>) (Object) SUCapability.class);

    @Override
    public void registerChunkComponentFactories(ChunkComponentFactoryRegistry registry) {
        registry.register(SU_CAPABILITY_COMPONENT_KEY, (a) -> {
            if (a instanceof LevelChunk lvlChk) {
                //noinspection ConstantValue
                if (lvlChk.getLevel() != null)
                    return (Component) new SUCapability(lvlChk.getLevel(), a);
            }
            if (a.getHeightAccessorForGeneration() instanceof Level level) {
                return (Component) new SUCapability(level, a);
            } else if (a.levelHeightAccessor instanceof Level level) {
                return (Component) new SUCapability(level, a);
            }
            throw new RuntimeException("uhhhh I ned help");
        });
    }
}

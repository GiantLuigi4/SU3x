package tfc.smallerunits.flywheel.mixin;

import dev.engine_room.flywheel.backend.engine.EngineImpl;
import dev.engine_room.flywheel.backend.engine.embed.EmbeddedEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EmbeddedEnvironment.class)
public interface EnvironmentAccessor {
    @Accessor("engine")
    EngineImpl getEngine();
}

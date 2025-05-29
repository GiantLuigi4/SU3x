package tfc.smallerunits.flywheel.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.simulation.level.client.AbstractTickerClientLevel;
import tfc.smallerunits.flywheel.visual.UnitEffect;
import tfc.smallerunits.flywheel.visual.VisualContainer;
import tfc.smallerunits.storage.IRegion;

import java.util.function.Supplier;

@Mixin(AbstractTickerClientLevel.class)
public abstract class TickerClientLevelMixin implements VisualContainer
//        , VisualizationLevel
{
    UnitEffect effect;

    @Inject(at = @At("TAIL"), method = "<init>")
    public void postInit(ClientLevel parent, ClientPacketListener p_205505_, ClientLevel.ClientLevelData p_205506_, ResourceKey p_205507_, Holder p_205508_, int p_205509_, int p_205510_, Supplier p_205511_, LevelRenderer p_205512_, boolean p_205513_, long p_205514_, int upb, IRegion region, CallbackInfo ci) {
        effect = new UnitEffect((Level) (Object) this, parent);
    }

    @Override
    public UnitEffect getEffect() {
        return effect;
    }

//    @Override
//    public boolean supportsVisualization() {
//        return true;
//    }
}

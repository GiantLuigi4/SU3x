package tfc.smallerunits.flywheel.mixin;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.flywheel.visual.VisualContainer;

@Mixin(value = Region.class, remap = false)
public class RegionMixin {
    @Inject(at = @At("TAIL"), method = "onAddClientLevel")
    protected void postAddClientLevel(Level parent, Level added, int upb, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        Minecraft.getInstance().level = (ClientLevel) parent;
        VisualizationManager manager = VisualizationManagerImpl.get(parent);
        Minecraft.getInstance().level = level;
        if (manager == null) {
            System.out.println(parent);
        } else {
            manager.effects().queueAdd(((VisualContainer) added).getEffect());
        }
    }

    @Inject(at = @At("TAIL"), method = "onClientLevelClosed")
    public void postCloseClientLevel(Level level, CallbackInfo ci) {
        ClientLevel clevel = Minecraft.getInstance().level;
        Level parent = ((ITickerLevel) level).getParent();
        Minecraft.getInstance().level = (ClientLevel) parent;
        VisualizationManager manager = VisualizationManagerImpl.get(parent);
        Minecraft.getInstance().level = clevel;
        if (manager == null) {
            System.out.println(parent);
        } else {
            manager.effects().queueRemove(((VisualContainer) level).getEffect());
        }
    }
}

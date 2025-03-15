package tfc.smallerunits.resizing.pehkui;

import net.minecraft.world.entity.Entity;
import tfc.smallerunits.plat.internal.IResizingUtil;
import virtuoel.pehkui.util.ScaleUtils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static tfc.smallerunits.core.utils.config.ServerConfig.GameplayOptions.EntityScaleOptions;

public class PehkuiUtils extends IResizingUtil {
    private static final UUID uuidHeight = UUID.fromString("5440b01a-974f-4495-bb9a-c7c87424bca4");
    private static final UUID uuidWidth = UUID.fromString("3949d2ed-b6cc-4330-9c13-98777f48ea51");

    public void resize(Entity entity, int amt) {
        if (entity == null) return;
        float newSize = getSize(entity);
        float oldSize = newSize;

        if (amt > 0) {
            if (oldSize >= EntityScaleOptions.minSize)
                newSize = (float) Math.max(getSize(entity) - (amt * EntityScaleOptions.downscaleRate), EntityScaleOptions.minSize);
        } else if (oldSize <= EntityScaleOptions.maxSize)
            newSize = (float) Math.min(getSize(entity) - (amt / EntityScaleOptions.upscaleRate), EntityScaleOptions.maxSize);

        PehkuiSupport.SUScaleType.get().getScaleData(entity).setTargetScale(newSize);
    }

    public float getSize(Entity entity) {
        if (entity == null) return 1;
        AtomicReference<Float> size = new AtomicReference<>(1f);
        size.set(size.get() * PehkuiSupport.SUScaleType.get().getScaleData(entity).getTargetScale());
        return size.get();
    }

    public float getActualSize(Entity entity) {
        if (entity == null) return 1;
        AtomicReference<Float> size = new AtomicReference<>(1f);
        size.set(size.get() * ScaleUtils.getBoundingBoxHeightScale(entity));
        return size.get();
    }

    public void resizeForUnit(Entity entity, float amt) {
        if (entity == null) return;
        PehkuiSupport.SUScaleType.get().getScaleData(entity).setScale(amt);
    }

    public boolean isResizingModPresent() {
        return true;
    }
}

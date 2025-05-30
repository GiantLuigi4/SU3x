package tfc.smallerunits.plat.internal;

import net.minecraft.world.entity.Entity;

public abstract class IResizingUtil {
    public abstract void resize(Entity entity, int amt);

    public abstract float getSize(Entity entity);

    public abstract float getActualSize(Entity entity);

    public abstract void resizeForUnit(Entity entity, float scale);

    public abstract boolean isResizingModPresent();
}

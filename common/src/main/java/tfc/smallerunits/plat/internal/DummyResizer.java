package tfc.smallerunits.plat.internal;

import net.minecraft.world.entity.Entity;

public class DummyResizer extends IResizingUtil{
    @Override
    public void resize(Entity entity, int amt) {

    }

    @Override
    public float getSize(Entity entity) {
        return 1;
    }

    @Override
    public float getActualSize(Entity entity) {
        return 1;
    }

    @Override
    public void resizeForUnit(Entity entity, float scale) {

    }

    @Override
    public boolean isResizingModPresent() {
        return false;
    }
}

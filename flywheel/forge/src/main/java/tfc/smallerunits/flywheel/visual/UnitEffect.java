package tfc.smallerunits.flywheel.visual;

import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class UnitEffect implements Effect {
    Level ticker, parent;
    UnitVisual myVisual;

    public UnitEffect(Level ticker, Level parent) {
        this.ticker = ticker;
        this.parent = parent;
    }

    @Override
    public LevelAccessor level() {
        return parent;
    }

    List<BlockEntity> deferAdd = new ArrayList<>();

    @Override
    public EffectVisual<?> visualize(VisualizationContext visualizationContext, float v) {
        myVisual = UnitVisual.with(this, visualizationContext, v);
        for (BlockEntity blockEntity : deferAdd) {
            myVisual.add(blockEntity);
        }
        deferAdd.clear();
        return myVisual;
    }

    public void add(BlockEntity be) {
        if (myVisual == null) {
            deferAdd.add(be);
        } else {
            myVisual.add(be);
        }
    }

    public void remove(BlockEntity be) {
        if (myVisual == null) {
            deferAdd.remove(be);
        } else {
            myVisual.remove(be);
        }
    }
}

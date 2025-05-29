package tfc.smallerunits.flywheel.visual;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.*;
import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import dev.engine_room.flywheel.lib.task.PlanMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import tfc.smallerunits.core.simulation.level.ITickerLevel;

import java.util.HashMap;
import java.util.Map;

public class UnitVisual implements EffectVisual<UnitEffect>, DynamicVisual, TickableVisual {
    VisualEmbedding embedding;
    UnitEffect effect;
    Level ticker;
    VisualizationContext context;

    Map<BlockEntity, Visual> visuals = new HashMap<>();
    PlanMap<DynamicVisual, DynamicVisual.Context> dynamicVisuals = new PlanMap<>();
    PlanMap<TickableVisual, TickableVisual.Context> tickableVisuals = new PlanMap<>();

    public static UnitVisual with(UnitEffect unitEffect, VisualizationContext visualizationContext, float v) {
        return new UnitVisual(
                visualizationContext.createEmbedding(Vec3i.ZERO),
                unitEffect,
                unitEffect.ticker,
                visualizationContext
        );
    }

    public UnitVisual(VisualEmbedding embedding, UnitEffect effect, Level ticker, VisualizationContext context) {
        this.embedding = embedding;
        this.effect = effect;
        this.ticker = ticker;
        this.context = context;
        ITickerLevel iticker = (ITickerLevel) ticker;
        BlockPos ps = iticker.getRegion().pos().toBlockPos();
        embedding.transforms(
                new Matrix4f().identity()
                        .translate(ps.getX(), ps.getY(), ps.getZ())
                        .scale(1f / iticker.getUPB()),
                new Matrix3f().identity()
        );
    }

    protected <T extends BlockEntity> void add(T be) {
        BlockEntityVisualizer<? super T> visualizer = (BlockEntityVisualizer<? super T>) VisualizerRegistry.getVisualizer(be.getType());
        if (visualizer == null) return;

        BlockEntityVisual<? super T> visual = visualizer.createVisual(this.embedding, be, 0);
        visuals.put(be, visual);
        if (visual instanceof DynamicVisual dynamic) dynamicVisuals.add(dynamic, dynamic.planFrame());
        if (visual instanceof TickableVisual tickable) tickableVisuals.add(tickable, tickable.planTick());
    }

    protected <T extends BlockEntity> void remove(T be) {
        BlockEntityVisualizer<? super T> visualizer = (BlockEntityVisualizer<? super T>) VisualizerRegistry.getVisualizer(be.getType());
        if (visualizer == null) return;

        BlockEntityVisual<? super T> visual = (BlockEntityVisual<? super T>) visuals.get(be);
        visual.delete();
    }

    @Override
    public void update(float v) {
    }

    @Override
    public void delete() {
        embedding.delete();
    }

    @Override
    public Plan<DynamicVisual.Context> planFrame() {
        return dynamicVisuals;
    }

    @Override
    public Plan<TickableVisual.Context> planTick() {
        return tickableVisuals;
    }
}

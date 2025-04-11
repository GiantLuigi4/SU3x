package tfc.smallerunits.forge.mixin.self_impl;

import net.minecraft.core.BlockPos;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelDataManager;
import net.minecraftforge.common.extensions.IForgeBlockGetter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import tfc.smallerunits.core.client.render.util.RenderWorld;
import tfc.smallerunits.plat.itf.IMayManageModelData;

import java.util.Objects;

@Mixin(RenderWorld.class)
public abstract class RenderWorldMixin implements IMayManageModelData, IForgeBlockGetter {
    @Nullable
    @Override
    public ModelDataManager getModelDataManager() {
        return getActual().getModelDataManager();
    }

    @Override
    public Object getModelData(BlockPos offsetPos) {
        ModelData modelData = Objects.requireNonNull(getModelDataManager()).getAt(offsetPos);
        if (modelData == null) modelData = ModelData.EMPTY;
        return modelData;
    }
}

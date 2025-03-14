package tfc.smallerunits.core.simulation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface ParentLookup {
	BlockState getState(BlockPos pos);
}

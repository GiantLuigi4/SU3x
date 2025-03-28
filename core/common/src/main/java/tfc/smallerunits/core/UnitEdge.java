package tfc.smallerunits.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.data.tracking.ICanUseUnits;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.plat.itf.IContextAwarePickable;

public class UnitEdge extends Block implements IContextAwarePickable {
	public static final Property<Boolean> TRANSPARENT = BooleanProperty.create("transparent");
	
	public UnitEdge() {
		super(Properties.copy(Blocks.BARRIER).destroyTime(0.1f));
	}
	
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder.add(TRANSPARENT));
	}
	
	@Override
	public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
		if (pState.getValue(TRANSPARENT)) {
			// TODO: look at parent world's block state and upscale the shape and offset it, I think? maybe?
			return Shapes.empty();
		}
		return super.getShape(pState, pLevel, pPos, pContext);
	}
	
	@Override
	public RenderShape getRenderShape(BlockState pState) {
		return RenderShape.INVISIBLE;
	}
	
	@Override
	public VoxelShape getOcclusionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
		return Shapes.empty();
	}
	
	@Override
	public VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
		return getOcclusionShape(pState, pLevel, pPos);
	}
	
	@Override
	protected void spawnDestroyParticles(Level pLevel, Player pPlayer, BlockPos pPos, BlockState pState) {
		if (pLevel instanceof ITickerLevel tickerLevel) {
			Region region = (Region) tickerLevel.getRegion();
			int upb = tickerLevel.getUPB();
			BlockPos bp = region.pos.toBlockPos().offset(
					// TODO: double check this
                    (int) Math.floor(pPos.getX() / (double) upb),
                    (int) Math.floor(pPos.getY() / (double) upb),
                    (int) Math.floor(pPos.getZ() / (double) upb)
            );
			
			BlockState state = tickerLevel.getParent().getBlockState(bp);
			pLevel.levelEvent(pPlayer, 2001, pPos, getId(state));
		}
	}
	
	@Override
	public void attack(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
		if (pPlayer instanceof ICanUseUnits unitUser)
			unitUser.removeUnit();
		super.attack(pState, pLevel, pPos, pPlayer);
	}
	
	@Override
	public void playerWillDestroy(Level level, BlockPos blockPos, BlockState blockState, Player player) {
		if (player instanceof ICanUseUnits unitUser)
			unitUser.removeUnit();
		super.playerWillDestroy(level, blockPos, blockState, player);
	}
	
	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
		if (player instanceof ICanUseUnits unitUser) {
			if (level instanceof ITickerLevel tickerLevel) {
				if (unitUser.actualResult() instanceof BlockHitResult bhr) {
					// TODO: move player out temporarily
					BlockPos pPos = bhr.getBlockPos().relative(bhr.getDirection().getOpposite());
					return IContextAwarePickable.getCloneStack(
							tickerLevel.getParent().getBlockState(pPos),
							bhr, tickerLevel.getParent(), pPos,
							player
					);
				}
			}
		}
		
		return ItemStack.EMPTY;
	}
}

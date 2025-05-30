package tfc.smallerunits.core.simulation.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import tfc.smallerunits.core.Registry;
import tfc.smallerunits.core.UnitEdge;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.UnitSpaceBlock;
import tfc.smallerunits.core.api.PositionUtils;
import tfc.smallerunits.core.client.access.tracking.FastCapabilityHandler;
import tfc.smallerunits.core.client.access.tracking.SUCapableChunk;
import tfc.smallerunits.core.data.access.ChunkAccessor;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.core.data.capability.SUCapabilityManager;
import tfc.smallerunits.core.logging.Loggers;
import tfc.smallerunits.core.networking.hackery.NetworkingHacks;
import tfc.smallerunits.core.simulation.block.ParentLookup;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.core.simulation.level.UnitChunkHolder;
import tfc.smallerunits.core.simulation.level.server.AbstractTickerServerLevel;
import tfc.smallerunits.core.utils.asm.ModCompat;
import tfc.smallerunits.core.utils.math.Math1D;
import tfc.smallerunits.core.utils.threading.ThreadLocals;
import tfc.smallerunits.plat.net.PacketTarget;
import tfc.smallerunits.plat.util.PlatformProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static tfc.smallerunits.core.simulation.WorldStitcher.chunkRelative;

public class BasicVerticalChunk extends LevelChunk {
	public final int yPos;
	// holds the functional chunk and a method which gets the corresponding BasicVerticalChunk from an integer representing which vertical chunk
	// quite basic... weird to word however
	private final VChunkLookup verticalLookup;
	public final ArrayList<BlockPos> updated = new ArrayList<>();
	public final ArrayList<BlockPos> besRemoved = new ArrayList<>();
	public ArrayList<BlockEntity> beChanges = new ArrayList<>();
	ParentLookup lookup;
	private final int upb;
	public UnitChunkHolder holder = null;

	LevelChunkSection section;

	public boolean isLoaded() {
		return !section.hasOnlyAir();
	}

	public BlockState getBlockState$(BlockPos pos) {
		// locals would be redundant, this is an internal method
		// this method assumes that pos.y will always be in bounds of the specific BasicVerticalChunk
		if (section.hasOnlyAir()) return Blocks.AIR.defaultBlockState(); // simple optimization, can do a fair amount
		return section.getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
	}

	public BasicVerticalChunk(Level pLevel, ChunkPos pPos, int y, VChunkLookup verticalLookup, ParentLookup lookup, int upb) {
		super(pLevel, pPos);
		this.yPos = y;
		this.verticalLookup = verticalLookup;
		this.lookup = lookup;
		this.upb = upb;
		setLoaded(true);

		section = super.getSection(0);
		((ChunkAccessor) this).setSectionArray(new LevelChunkSection[]{section});
		// TODO: use mixin to null out unnecessary fields, maybe
	}

	@Override
	public boolean isTicking(BlockPos pPos) {
		// TODO:
		BlockPos origin = new BlockPos(chunkPos.getMinBlockX(), yPos * 16, chunkPos.getMinBlockZ());
		BlockPos.MutableBlockPos pos = ThreadLocals.posLocal.get();
		Level parent = ((ITickerLevel) level).getParent();

		if (parent == null) return false;

		PositionUtils.getParentPos(pPos, this, pos);
		return parent.isLoaded(pos);
	}

	@Override
	public LevelChunkSection getSection(int p_187657_) {
		if (p_187657_ == yPos) return section;
		int yO = chunkRelative(p_187657_, upb) + p_187657_;
		return verticalLookup.applyAbs(p_187657_).getSection(yO);
	}

	public LevelChunkSection getSectionNullable(int sectionIndex) {
		if (sectionIndex == yPos) return section;
		int yO = chunkRelative(sectionIndex, upb) + sectionIndex;
		LevelChunk chunk = verticalLookup.applyAbsNoLoad(sectionIndex);
		if (chunk == null) return null;
		return chunk.getSection(yO);
	}

	@Override
	public int getSectionsCount() {
		return 3;
	}

	@Override
	public int getMinSection() {
		return yPos - 1;
	}

	@Override
	public int getMaxSection() {
		return yPos + 1;
	}

	@Override
	public int getMinBuildHeight() {
		if (yPos == 0) return 0;
		return (yPos - 1) * 16;
	}

	@Override
	public int getMaxBuildHeight() {
		return (yPos + 1) * 16;
	}

	@Override
	public int getSectionIndex(int pY) {
//		return this.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord(pY));
		return pY >> 4;
	}

	@Override
	public int getSectionIndexFromSectionY(int pSectionIndex) {
		return level.getSectionIndexFromSectionY(pSectionIndex);
//		return pSectionIndex >> 4;
	}

	@Override
	public int getSectionYFromSectionIndex(int pSectionIndex) {
//		return super.getSectionYFromSectionIndex(pSectionIndex);
		return pSectionIndex << 4;
	}

	@Nullable
	@Override
	public BlockState setBlockState(BlockPos pos, BlockState pState, boolean pIsMoving) {
		int yO = Math1D.getChunkOffset(pos.getY(), 16);
		
		BlockPos wrap = new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
		
		if (yO != 0 || pos.getX() < 0 || pos.getZ() < 0 || pos.getX() >= (upb * 32) || pos.getZ() >= (upb * 32)) {
			// TODO: non-grid aligned world stitching?

			BasicVerticalChunk chunk = verticalLookup.apply(yPos + yO);
			if (chunk == null) {
				return Blocks.VOID_AIR.defaultBlockState();
			}
			if (chunk.holder != null)
				chunk.holder.setBlockDirty(wrap);
			return chunk.setBlockState$(wrap, pState, pIsMoving);
		}
		if (holder != null)
			holder.setBlockDirty(wrap);
		return setBlockState$(wrap, pState, pIsMoving);
	}

	// TODO: optimize?
	public BlockState setBlockState$(BlockPos pPos, BlockState pState, boolean pIsMoving) {
		if (level.isClientSide) return setBlockState$$(pPos, pState, pIsMoving);

		int j = pPos.getX() & 15;
		int k = pPos.getY();
		int l = pPos.getZ() & 15;

		BlockPos parentPos = PositionUtils.getParentPosPrecise(pPos, this);
		LevelChunk ac = ((ITickerLevel) level).getParent().getChunkAt(parentPos);
		UnitSpace space = null;
		BlockState oldState = section.getBlockState(j, k, l);

		if (ac instanceof FastCapabilityHandler capabilityHandler) {
			space = capabilityHandler.getSUCapability().getUnit(parentPos);

			if (space == null) {
				BlockState state = ac.getBlockState(parentPos);

				if (state.isAir()) { // TODO: do this better
					if (!pState.isAir()) {
						// setup network
						NetworkingHacks.LevelDescriptor descriptor = NetworkingHacks.unitPos.get();
						if (descriptor != null)
							NetworkingHacks.setPos(descriptor.parent());

						ac.getSection(
								ac.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord((parentPos.getY())))
						).setBlockState(parentPos.getX() & 15, parentPos.getY() & 15, parentPos.getZ() & 15, Registry.UNIT_SPACE.get().defaultBlockState());
						ac.getLevel().sendBlockUpdated(parentPos, state, Registry.UNIT_SPACE.get().defaultBlockState(), 0);

						space = capabilityHandler.getSUCapability().getOrMakeUnit(parentPos);
						// TODO: debug why space can still be null after this or what
						space.isNatural = true;
						space.setUpb(((ITickerLevel) level).getUPB());
						// send unit to client
						space.sendSync(PacketTarget.trackingChunk(ac));
						
						ac.setUnsaved(true);
						
						// reset network
						NetworkingHacks.unitPos.set(descriptor);
					}
				}
			}
		}

		BlockState output = setBlockState$$(pPos, pState, pIsMoving);

		if (ac instanceof FastCapabilityHandler capabilityHandler) {
			if (space != null) {
				space.removeState(oldState);
				space.addState(pState);

				if (space.isEmpty() && space.isNatural) {
					space.clear();

					// setup network
					NetworkingHacks.LevelDescriptor descriptor = NetworkingHacks.unitPos.get();
					if (descriptor != null)
						NetworkingHacks.setPos(descriptor.parent());

					// remove unit space
					ac.getSection(
							ac.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord((parentPos.getY())))
					).setBlockState(parentPos.getX() & 15, parentPos.getY() & 15, parentPos.getZ() & 15, Blocks.AIR.defaultBlockState());
					ac.getLevel().sendBlockUpdated(parentPos, Registry.UNIT_SPACE.get().defaultBlockState(), Blocks.AIR.defaultBlockState(), 0);

					// remove unit
					capabilityHandler.getSUCapability().removeUnit(parentPos);
					space.sendRemove(PacketTarget.trackingChunk(ac));
					
					ac.setUnsaved(true);
					
					// reset network
					if (descriptor != null)
						NetworkingHacks.setPos(descriptor);
				}
			}
		}

		return output;
	}

	// TODO: I'm sure I can shrink this
	public BlockState setBlockState$$(BlockPos pPos, BlockState pState, boolean pIsMoving) {
		boolean flag = section.hasOnlyAir();
		int j = pPos.getX() & 15;
		int k = pPos.getY();
		int l = pPos.getZ() & 15;
		// TODO
		if (flag && pState.isAir()) {
			return null;
		} else {
			pPos = pPos.above(yPos * 16);
			BlockState blockstate = section.setBlockState(j, k, l, pState);
			if (blockstate == pState) {
				return null;
			} else {
				BlockPos offsetPos = chunkPos.getWorldPosition().offset(pPos.getX(), pPos.getY() & 15, pPos.getZ()).offset(0, yPos * 16, 0);

				Block block = pState.getBlock();
				this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING).update(j, k, l, pState);
				this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(j, k, l, pState);
				this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR).update(j, k, l, pState);
				this.heightmaps.get(Heightmap.Types.WORLD_SURFACE).update(j, k, l, pState);

				boolean flag2 = blockstate.hasBlockEntity();
				if (!this.level.isClientSide) {
					blockstate.onRemove(this.level, offsetPos, pState, pIsMoving);
				} else if ((!blockstate.is(block) || !pState.hasBlockEntity()) && flag2) {
					this.removeBlockEntity(new BlockPos(pPos.getX(), pPos.getY() & 15, pPos.getZ()));
				}

				if (!section.getBlockState(j, k, l).is(block)) {
					return null;
				} else {
					if (!this.level.isClientSide && !PlatformProvider.UTILS.shouldCaptureBlockSnapshots(level)) {
						pState.onPlace(this.level, offsetPos, blockstate, pIsMoving);
					}

					if (pState.hasBlockEntity()) {
						BlockEntity blockentity = this.getBlockEntity$(new BlockPos(pPos.getX(), pPos.getY() & 15, pPos.getZ()), EntityCreationType.CHECK);
						if (blockentity == null) {
							blockentity = ((EntityBlock) block).newBlockEntity(offsetPos, pState);
							if (blockentity != null) {
								this.addAndRegisterBlockEntity(blockentity);
							}
						} else {
							blockentity.setBlockState(pState);
							this.updateBlockEntityTicker(blockentity);
						}
					}

					boolean flag1 = section.hasOnlyAir();
					if (!flag1) level.getLightEngine().checkBlock(offsetPos);

					updated.add(pPos.below(yPos * 16));
					setUnsaved(true);
					return blockstate;
				}
			}
		}
	}

	@Override
	public void setUnsaved(boolean pUnsaved) {
		if (pUnsaved) {
			if (!super.isUnsaved()) {
				if (level instanceof AbstractTickerServerLevel) {
					((AbstractTickerServerLevel) level).saveWorld.markForSave(this);
				}
			}
		}
		super.setUnsaved(pUnsaved);
	}
	
	private static final BlockState eTranpsarent = Registry.UNIT_EDGE.get().defaultBlockState().setValue(UnitEdge.TRANSPARENT, true);
	private static final BlockState eSolid = Registry.UNIT_EDGE.get().defaultBlockState().setValue(UnitEdge.TRANSPARENT, false);
	
	@Override
	public BlockState getBlockState(BlockPos pos) {
		boolean lookupPass = false;

		BlockPos parentPos = PositionUtils.getParentPos(pos, this, ThreadLocals.posLocal.get());
		BlockState parentState = lookup.getState(parentPos);
		if (parentState.isAir()) lookupPass = true;

		boolean transparent = true;
		Level lvl = ((ITickerLevel) level).getParent();
		if (parentState.isCollisionShapeFullBlock(lvl, parentPos))
			transparent = false;
		if (parentState.getBlock() instanceof UnitSpaceBlock) {
			LevelChunk chunk = lvl.getChunkAt(parentPos);
			if (chunk instanceof EmptyLevelChunk) {
				lookupPass = false;
			} else {
				ISUCapability capability = SUCapabilityManager.getCapability(chunk);
				UnitSpace space = capability.getUnit(parentPos);
				if (space != null) {
					lookupPass = space.unitsPerBlock == upb;
				} else {
					lookupPass = false;
				}
			}
		}

		if (lookupPass) {
			int yO = Math1D.getChunkOffset(pos.getY(), 16);
			if (yO != 0 || pos.getX() < 0 || pos.getZ() < 0 || pos.getX() >= (upb * 32) || pos.getZ() >= (upb * 32)) {
				BasicVerticalChunk chunk = verticalLookup.applyAbsNoLoad(yPos + yO);
				if (chunk == null)
					return Blocks.VOID_AIR.defaultBlockState();
				return chunk.getBlockState$(new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15));
			}
			return getBlockState$(pos);
		} else {
//			return Registry.UNIT_EDGE.get().defaultBlockState().setValue(UnitEdge.TRANSPARENT, transparent);
			return transparent ? eTranpsarent : eSolid;
		}
	}

	public BlockState getBlockStateSmallOnly(BlockPos pos) {
		int yO = Math1D.getChunkOffset(pos.getY(), 16);
		if (yO != 0 || pos.getX() < 0 || pos.getZ() < 0 || pos.getX() >= (upb * 32) || pos.getZ() >= (upb * 32)) {
			BasicVerticalChunk chunk = verticalLookup.applyAbsNoLoad(yPos + yO);
			if (chunk == null)
				return Blocks.VOID_AIR.defaultBlockState();
			return chunk.getBlockState$(new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15));
		}
		return getBlockState$(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		int yO = Math1D.getChunkOffset(pos.getY(), 16);
		if (yO != 0 || pos.getX() < 0 || pos.getZ() < 0 || pos.getX() >= (upb * 32) || pos.getZ() >= (upb * 32)) {
			BasicVerticalChunk chunk = verticalLookup.applyAbsNoLoad(yPos + yO);
			if (chunk == null || !chunk.isLoaded())
				return Fluids.EMPTY.defaultFluidState();
			return chunk.getFluidState(new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15));
		}
		if (section.hasOnlyAir()) return Fluids.EMPTY.defaultFluidState();
		return getBlockState$(pos).getFluidState();
	}

	private void setBlockFast$(boolean allowSave, BlockPos pos, BlockState state, HashMap<SectionPos, ChunkAccess> chunkCache) {
		int j = pos.getX() & 15;
		int k = pos.getY();
		int l = pos.getZ() & 15;
		BlockPos parentPos = PositionUtils.getParentPos(pos, this, ThreadLocals.posLocal.get());

		SectionPos pPosAsSectionPos = SectionPos.of(parentPos);
		BlockState oldState = section.setBlockState(j, k, l, state);

		if (!level.isClientSide && allowSave)
			setUnsaved(true);

		if (level.isClientSide) {
			ChunkAccess ac = chunkCache.get(pPosAsSectionPos);
			if (ac == null) {
				ac = ((ITickerLevel) level).getParent().getChunkAt(parentPos);
				chunkCache.put(pPosAsSectionPos, ac);
			}

			if (!state.isAir()) {
				ISUCapability capability = SUCapabilityManager.getCapability(((ITickerLevel) level).getParent(), ac);
				UnitSpace space = capability.getOrMakeUnit(parentPos);
				if (space != null) {
					space.removeState(oldState);
					space.addState(state);
				} else Loggers.SU_LOGGER.warn("Unit space @" + parentPos + " did not exist");

				section.setBlockState(j, k, l, state);

				level.getLightEngine().checkBlock(chunkPos.getWorldPosition().offset(pos).offset(0, yPos * 16, 0));
			} else {
				ISUCapability capability = SUCapabilityManager.getCapability(((ITickerLevel) level).getParent(), ac);
				UnitSpace space = capability.getUnit(parentPos);
				if (space != null) space.removeState(section.getBlockState(j, k, l));
				else Loggers.SU_LOGGER.warn("Unit space @" + parentPos + " did not exist");
			}
		}
	}

	public void setBlockFast(BlockPos pos, BlockState state, HashMap<SectionPos, ChunkAccess> chunkCache) {
		setBlockFast(true, pos, state, chunkCache);
	}

	public void setBlockFast(boolean allowSave, BlockPos pos, BlockState state, HashMap<SectionPos, ChunkAccess> chunkCache) {
		int yO = Math1D.getChunkOffset(pos.getY(), 16);
		if (yO != 0) {
			BasicVerticalChunk chunk = verticalLookup.apply(yPos + yO);
			chunk.setBlockFast$(allowSave, new BlockPos(pos.getX(), pos.getY() & 15, pos.getZ()), state, chunkCache);
			return;
		}
		setBlockFast$(allowSave, new BlockPos(pos), state, chunkCache);
	}

	public void randomTick() {
		if (section.hasOnlyAir())
			return;

		for (int k = 0; k < ((ITickerLevel) level).randomTickCount(); ++k) {
			BlockPos blockpos1 = level.getBlockRandomPos(0, 0, 0, 15);
			BlockState blockstate = this.getBlockState$(blockpos1);
			BlockPos wp = blockpos1.offset(chunkPos.getWorldPosition()).relative(Direction.UP, yPos * 16 - 1);
			if (blockstate.isRandomlyTicking()) {
				if (!level.isClientSide() && level instanceof ServerLevel) { // TODO: ?
					blockstate.randomTick((ServerLevel) level, wp.above(), level.random);
				}
			}

			FluidState fluidstate = blockstate.getFluidState();
			if (fluidstate.isRandomlyTicking()) {
				fluidstate.randomTick(level, wp, level.random);
			}
		}
	}

	public BasicVerticalChunk getSubChunk(int cy) {
		return verticalLookup.apply(cy);
	}

	@Override
	// TODO: do this more properly?
	public FluidState getFluidState(int pX, int pY, int pZ) {
		return getBlockState(new BlockPos(pX, pY, pZ)).getFluidState();
	}

	long modTime = 0;

	public void updateModificationTime(long gameTime) {
		if (gameTime == -1) modTime = -1;
		// staggers save time
//		this.modTime = gameTime + new Random().nextInt(700) + 300;
		this.modTime = gameTime + new Random().nextInt(300) + 200;
//		this.modTime = gameTime + 100;
	}

	public boolean isSaveTime(long gameTime) {
		if (modTime == -1) return false;
        // past self, WHAT DO YOU MEAN, TOD0?
		// TODO:
		return gameTime >= modTime;
	}

	public void maybeUnload() {
		if (!level.isClientSide) {
			if (!(level instanceof AbstractTickerServerLevel))
				return;
		} else return;
		BlockPos origin = new BlockPos(chunkPos.getMinBlockX(), yPos * 16, chunkPos.getMinBlockZ());
		BlockPos.MutableBlockPos pos = ThreadLocals.posLocal.get();
		Level parent = ((ITickerLevel) level).getParent();
		try {
			if (parent == null) {
				((AbstractTickerServerLevel) level).saveWorld.saveChunk(this);
				return;
			}
			boolean anyLoaded = false;
			lx:
			for (int x = 0; x <= 1; x++) {
				for (int y = 0; y <= 1; y++) {
					for (int z = 0; z <= 1; z++) {
						// TODO: check?
						BlockPos test = origin.offset(x * 15, y * 15, z * 15);
						PositionUtils.getParentPos(test, this, pos);
						if (parent.isLoaded(pos)) {
							anyLoaded = true;
							break lx;
						}
					}
				}
			}

			if (!anyLoaded) {
				if (isUnsaved()) {
					((AbstractTickerServerLevel) level).saveWorld.saveChunk(this);
					((AbstractTickerServerLevel) level).unload(this);
					// after this point, block entities get cleared
					// do NOT save after here
					updateModificationTime(-1);
					setUnsaved(false);
				}
			}
		} catch (Throwable ignored) {
			ignored.printStackTrace();
		}
		// TODO:
	}

	public SectionPos getSectionPos() {
		return SectionPos.of(getPos(), yPos);
	}

	@Override
	public Holder<Biome> getNoiseBiome(int p_204347_, int p_204348_, int p_204349_) {
		return level.getBiome(
				new BlockPos(
						getSectionPos().minBlockX() + p_204347_,
						getSectionPos().minBlockY() + p_204348_,
						getSectionPos().minBlockZ() + p_204349_
				)
		);
	}

	private boolean isInLevel() {
		return this.isLoaded() || this.level.isClientSide();
	}

	@Nullable
	@Override
	public BlockEntity getBlockEntity(BlockPos pPos, EntityCreationType pCreationType) {
        int yO = Math1D.getChunkOffset(pPos.getY(), 16);
        if (yO != 0 || pPos.getX() < 0 || pPos.getZ() < 0 || pPos.getX() >= (upb * 32) || pPos.getZ() >= (upb * 32)) {
            // TODO: non-grid aligned world stitching?

            BasicVerticalChunk chunk = verticalLookup.apply(yPos + yO);
            if (chunk == null)
                return null;

			BlockPos bp = new BlockPos(pPos.getX() & 15, pPos.getY() & 15, pPos.getZ() & 15);
            return chunk.getBlockEntity$(bp, pCreationType);
        }

        return getBlockEntity$(pPos, pCreationType);
	}

    private BlockEntity getBlockEntity$(BlockPos pPos, EntityCreationType pCreationType) {
        return super.getBlockEntity(new BlockPos(pPos.getX() & 15, pPos.getY() & 15, pPos.getZ() & 15), pCreationType);
    }

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pPos) {
//        return super.createBlockEntity(pPos.offset(
////                chunkPos.getMinBlockX(),
////                yPos * 16,
////                chunkPos.getMinBlockZ()
////        ));
		BlockState blockstate = this.getBlockStateSmallOnly(pPos);
		return !blockstate.hasBlockEntity() ? null : ((EntityBlock)blockstate.getBlock()).newBlockEntity(pPos.offset(
                chunkPos.getMinBlockX(),
                yPos * 16,
                chunkPos.getMinBlockZ()
        ), blockstate);
    }

	@Override
	public void setBlockEntity(BlockEntity pBlockEntity) {
        BlockPos pPos = pBlockEntity.getBlockPos();
		pPos = pPos.offset(0, -yPos * 16, 0);
		
		int yO = Math1D.getChunkOffset(pPos.getY(), 16);
        if (yO != 0 || pPos.getX() < 0 || pPos.getZ() < 0 || pPos.getX() >= (upb * 32) || pPos.getZ() >= (upb * 32)) {
            // TODO: non-grid aligned world stitching?

            BasicVerticalChunk chunk = verticalLookup.applyAbs(yPos + yO);
            if (chunk == null)
                return;

            if (chunk.holder != null)
                chunk.holder.setBlockDirty(new BlockPos(pPos.getX() & 15, pPos.getY() & 15, pPos.getZ() & 15));
            chunk.setBlockEntity$(pBlockEntity);
            return;
        }

        if (holder != null)
            holder.setBlockDirty(pPos);
        setBlockEntity$(pBlockEntity);
	}

    public void setBlockEntity$(BlockEntity pBlockEntity) {
		BlockPos blockpos = pBlockEntity.getBlockPos();
		blockpos = new BlockPos(blockpos.getX() & 15, blockpos.getY() & 15, blockpos.getZ() & 15);
		if (this.getBlockStateSmallOnly(blockpos).hasBlockEntity()) {
			pBlockEntity.setLevel(this.level);
			pBlockEntity.clearRemoved();
			BlockEntity blockentity = this.blockEntities.put(blockpos.immutable(), pBlockEntity);
			if (blockentity != null && blockentity != pBlockEntity) {
				blockentity.setRemoved();
			}
			PlatformProvider.UTILS.beLoaded(pBlockEntity, level);
		}

		if (!level.isClientSide) return;

		ITickerLevel tickerWorld = ((ITickerLevel) level);

		BlockPos pos = pBlockEntity.getBlockPos();
		pos = pos.offset(0, -yPos * 16, 0);
		BlockPos parentPos = PositionUtils.getParentPos(pos, this, ThreadLocals.posLocal.get());
		ChunkAccess ac;
		ac = tickerWorld.getParent().getChunkAt(parentPos);

		// TODO: check if a renderer exists, or smth?
		((SUCapableChunk) ac).addTile(pBlockEntity);
    }
	
	@Override
	public void addAndRegisterBlockEntity(BlockEntity $$0) {
		int yO = Math1D.getChunkOffset($$0.getBlockPos().getY(), 16);
		if (yO >= upb * 32 || yO < 0) {
			$$0.worldPosition = new BlockPos(
					$$0.getBlockPos().getX(),
//					yPos * 16 + $$0.getBlockPos().getY() & 15,
					verticalLookup.applyAbs(yO).yPos * 16 + ($$0.getBlockPos().getY() & 15),
					$$0.getBlockPos().getZ()
			);
			verticalLookup.applyAbs(yO).verticalLookup.applyAbs(0).addAndRegisterBlockEntity(
					$$0
			);
			return;
		}
		
		if (yPos != 0) {
			verticalLookup.applyAbs(0).addAndRegisterBlockEntity($$0);
			return;
		}
		
		super.addAndRegisterBlockEntity($$0);
	}
	
	@Override
	public void removeBlockEntity(BlockPos pPos) {
		int yO = Math1D.getChunkOffset(pPos.getY(), 16);
		if (yO != 0 || pPos.getX() < 0 || pPos.getZ() < 0 || pPos.getX() >= (upb * 32) || pPos.getZ() >= (upb * 32)) {
			// TODO: non-grid aligned world stitching?

			BasicVerticalChunk chunk = verticalLookup.apply(yPos + yO);
			if (chunk == null)
				return;

			if (chunk.holder != null)
				chunk.holder.setBlockDirty(new BlockPos(pPos.getX() & 15, pPos.getY() & 15, pPos.getZ() & 15));
			chunk.removeBlockEntity$(new BlockPos(pPos.getX() & 15, pPos.getY() & 15, pPos.getZ() & 15));
			return;
		}

		if (holder != null)
			holder.setBlockDirty(pPos);
		removeBlockEntity$(pPos);
	}

	public void removeBlockEntity$(BlockPos pos) {
		if (level.isClientSide) {
			BlockPos pPos = new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
			ITickerLevel tickerWorld = ((ITickerLevel) level);

			BlockPos parentPos = PositionUtils.getParentPos(pPos, this, ThreadLocals.posLocal.get());
			ChunkAccess ac;
			ac = tickerWorld.getParent().getChunkAt(parentPos);
			
			BlockPos offsetPos = pPos.offset(chunkPos.getMinBlockX(), yPos * 16, chunkPos.getMinBlockZ());

			ArrayList<BlockEntity> toRemove = new ArrayList<>();
			synchronized (((SUCapableChunk) ac).getTiles()) {
				for (BlockEntity tile : ((SUCapableChunk) ac).getTiles()) {
					if (tile.getBlockPos().equals(offsetPos)) {
						toRemove.add(tile);
						ModCompat.onRemoveBE(tile);
					}
				}
				((SUCapableChunk) ac).getTiles().removeAll(toRemove);
			}
		}

        super.removeBlockEntity(new BlockPos(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15));
	}

	@Override
	public <T extends BlockEntity> void updateBlockEntityTicker(T pBlockEntity) {
		if (yPos == 0) super.updateBlockEntityTicker(pBlockEntity);
		else verticalLookup.applyAbs(0).updateBlockEntityTicker(pBlockEntity);
	}

	@Override
	public <T extends BlockEntity> TickingBlockEntity createTicker(T pBlockEntity, BlockEntityTicker<T> pTicker) {
		if (yPos == 0)
			return super.createTicker(pBlockEntity, pTicker);
		return verticalLookup.applyAbs(0).createTicker(pBlockEntity, pTicker);
	}

	@Override
	public void removeBlockEntityTicker(BlockPos pPos) {
		if (yPos != 0)
			verticalLookup.applyAbs(0).removeBlockEntityTicker(new BlockPos(pPos.getX(), pPos.getY() + yPos * 16, pPos.getZ()));
		else super.removeBlockEntityTicker(chunkPos.getWorldPosition().offset(pPos));
	}

//	@Override
//	public Map<BlockPos, BlockEntity> getBlockEntities() {
//		return super.getBlockEntities();
//	}
	
	/* lithium compat */
	@Override
	public ChunkStatus getStatus() {
		return ChunkStatus.FULL;
	}

	@Override
	public FullChunkStatus getFullStatus() {
		return FullChunkStatus.ENTITY_TICKING;
	}
}

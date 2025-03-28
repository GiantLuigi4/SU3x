package tfc.smallerunits.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import tfc.smallerunits.core.client.render.util.RenderWorld;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.core.data.capability.SUCapabilityManager;
import tfc.smallerunits.core.data.storage.Region;
import tfc.smallerunits.core.data.storage.RegionPos;
import tfc.smallerunits.core.data.storage.UnitPallet;
import tfc.smallerunits.core.data.tracking.RegionalAttachments;
import tfc.smallerunits.core.logging.Loggers;
import tfc.smallerunits.core.networking.SUNetworkRegistry;
import tfc.smallerunits.core.networking.sync.RemoveUnitPacketS2C;
import tfc.smallerunits.core.networking.sync.SyncPacketS2C;
import tfc.smallerunits.core.simulation.chunk.BasicVerticalChunk;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.core.utils.config.ServerConfig;
import tfc.smallerunits.core.utils.math.Math1D;
import tfc.smallerunits.plat.net.PacketTarget;
import tfc.smallerunits.plat.util.PlatformProvider;

import java.util.*;

public class UnitSpace {
	// TODO: cache a list of redstone blocks?
	
	// TODO: migrate to chunk class
	public final BlockPos pos;
	public int unitsPerBlock;
	
	public final Level level;
	public RegionPos regionPos;
	protected Level myLevel;
	CompoundTag tag;
	private BlockPos myPosInTheLevel;
	public boolean isNatural;
	RenderWorld wld;
	
	int numBlocks = 0;
	
	public UnitSpace(BlockPos pos, Level level) {
		this.pos = pos;
		this.level = level;
		
		unitsPerBlock = 1;
		setUpb(ServerConfig.SizeOptions.defaultScale);
		isNatural = false;
		
		regionPos = new RegionPos(pos);
	}
	
	public Level getMyLevel() {
		return myLevel;
	}
	
	public void setUpb(int upb) {
		this.unitsPerBlock = upb;
		myPosInTheLevel = new BlockPos(
				Math1D.regionMod(pos.getX()) * upb,
				Math1D.regionMod(pos.getY()) * upb,
				Math1D.regionMod(pos.getZ()) * upb
		);
		myLevel = null;
//		tick();
	}
	
	public static UnitSpace fromNBT(CompoundTag tag, Level lvl) {
		UnitSpace space = new UnitSpace(
				new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")),
				lvl
		);
		space.tag = tag;
		space.unitsPerBlock = tag.getInt("upb");
		space.setUpb(space.unitsPerBlock);
		space.loadWorld(tag);
		if (tag.contains("natural")) space.isNatural = tag.getBoolean("natural");
		// TODO: multiply by upb
		
		if (space.unitsPerBlock == 0)
			Loggers.UNITSPACE_LOGGER.error("A unit space had a UPB of " + space.unitsPerBlock + "; this is not a good thing! Coords: " + space.pos.getX() + ", " + space.pos.getY() + ", " + space.pos.getZ());
		
		return space;
	}
	
	int minBlock(Direction.Axis axis) {
		return SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(
				axis.choose(
						this.myPosInTheLevel.getX(),
						this.myPosInTheLevel.getY(),
						this.myPosInTheLevel.getZ()
				)
		));
	}
	
	int maxBlock(Direction.Axis axis) {
		return SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(
				axis.choose(
						this.myPosInTheLevel.getX() + unitsPerBlock,
						this.myPosInTheLevel.getY() + unitsPerBlock,
						this.myPosInTheLevel.getZ() + unitsPerBlock
				)
		));
	}
	
	private void loadWorld(CompoundTag tag) {
		if (myLevel == null || tag == null) return;
		if (!level.isLoaded(pos)) return;
		
		// ensures that chunks are loaded
		for (int x = minBlock(Direction.Axis.X); x <= maxBlock(Direction.Axis.X); x += 16) {
			int pX = SectionPos.blockToSectionCoord(x);
			
			for (int z = minBlock(Direction.Axis.Z); z <= maxBlock(Direction.Axis.Z); z += 16) {
				int pZ = SectionPos.blockToSectionCoord(z);
				
				boolean anyExists = false;
				for (int y = minBlock(Direction.Axis.Y); y <= maxBlock(Direction.Axis.Y); y += 16) {
					int pY = (y) >> 4;
					
					if (((ITickerLevel) myLevel).chunkExists(SectionPos.of(pX, pY, pZ))) {
						anyExists = true;
						break;
					}
				}
				if (anyExists) {
					ChunkAccess chunk = myLevel.getChunk(pX, pZ, ChunkStatus.FULL, true);
					if (chunk == null) continue;
					BasicVerticalChunk vc = (BasicVerticalChunk) chunk;
					
					for (int y = minBlock(Direction.Axis.Y); y <= maxBlock(Direction.Axis.Y); y += 16) {
						int pY = (y) >> 4;

						if (((ITickerLevel) myLevel).chunkExists(SectionPos.of(pX, pY, pZ))) {
							vc.getSubChunk(pY);
						}
					}
				}
			}
		}
		
		if (tag.contains("blocks", Tag.TAG_COMPOUND)) {
			UnitPallet pallet = UnitPallet.fromNBT(tag.getCompound("blocks"));
			loadPallet(pallet);
		}
		
		if (tag.contains("ticks")) {
			if (myLevel instanceof ITickerLevel) {
				((ITickerLevel) myLevel).loadTicks(tag.getCompound("ticks"));
			}
		}
		
		if (tag.contains("tiles", Tag.TAG_COMPOUND)) {
			CompoundTag tiles = tag.getCompound("tiles");
			for (String pos : tiles.getAllKeys()) {
				String[] strs = pos.split(",");
				BlockPos pos1 = new BlockPos(
						Integer.parseInt(strs[0]),
						Integer.parseInt(strs[1]),
						Integer.parseInt(strs[2])
				);
				// TODO: fix
				BlockEntity be = null;
				try {
					be = BlockEntity.loadStatic(
							pos1,
							myLevel.getBlockState(pos1),
							tiles.getCompound(pos)
					);
				} catch (Exception err) {
					err.printStackTrace();
				}
				if (be == null) continue;
				myLevel.setBlockEntity(be);
			}
			((ITickerLevel) myLevel).setLoaded();
		}
		
		if (tag.contains("countBlocks", Tag.TAG_INT)) {
			numBlocks = tag.getInt("countBlocks");
		} else {
			for (BlockState block : getBlocks()) {
				if (block != null && !block.isAir())
					addState(block);
			}
		}
		
		this.tag = null;
	}
	
	/* reason: race conditions */
	public void tick() {
		if (myLevel instanceof ServerLevel) {
//			((ServerLevel) myLevel).tick(() -> true);
		} else if (myLevel == null) {
			int upb = unitsPerBlock;
			if (level instanceof ServerLevel) {
				ChunkMap cm = ((ServerLevel) level).getChunkSource().chunkMap;
				Region r = ((RegionalAttachments) cm).SU$getRegion(new RegionPos(pos));
				if (r == null) {
//					if (level.isLoaded(pos))
					Loggers.UNITSPACE_LOGGER.error("Server: Region@" + new RegionPos(pos) + " was null");
					return;
				}
				if (myLevel != null)
					((ITickerLevel) myLevel).clear(myPosInTheLevel, myPosInTheLevel.offset(upb, upb, upb));
				myLevel = r.getServerWorld(level.getServer(), (ServerLevel) level, upb);
//				setState(new BlockPos(0, 0, 0), Blocks.STONE);
			} else if (level instanceof RegionalAttachments) {
				Region r = ((RegionalAttachments) level).SU$getRegion(new RegionPos(pos));
				if (r == null) {
					if (PlatformProvider.UTILS.isDevEnv())
						Loggers.UNITSPACE_LOGGER.error("Client: Region@" + new RegionPos(pos) + " was null");
					return;
				}
				if (myLevel != null)
					((ITickerLevel) myLevel).clear(myPosInTheLevel, myPosInTheLevel.offset(upb, upb, upb));
				myLevel = r.getClientWorld(level, upb);
				
				// TODO: allow for optimization?
				wld = new RenderWorld(getMyLevel(), getOffsetPos(new BlockPos(0, 0, 0)), upb);
			}
			loadWorld(tag);
		}
	}
	
	// gets every block within the unit space
	public BlockState[] getBlocks() {
		numBlocks = 0;
		final BlockState[] states = new BlockState[unitsPerBlock * unitsPerBlock * unitsPerBlock];
		
		BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
		for (int x = 0; x < unitsPerBlock; x++) {
			for (int z = 0; z < unitsPerBlock; z++) {
				int pX = SectionPos.blockToSectionCoord(x + myPosInTheLevel.getX());
				int pZ = SectionPos.blockToSectionCoord(z + myPosInTheLevel.getZ());
				ChunkAccess chunk = myLevel.getChunk(pX, pZ, ChunkStatus.FULL, false);
				
				if (chunk == null) continue;
				
				BasicVerticalChunk vc = (BasicVerticalChunk) chunk;
				
				for (int y = 0; y < unitsPerBlock; y++) {
					int sectionIndex = vc.getSectionIndex(y + myPosInTheLevel.getY());
					LevelChunkSection section = vc.getSectionNullable(sectionIndex);
					
					if (section == null || section.hasOnlyAir()) {
						int trg;
						if (y == (y >> 4) << 4) trg = y + 15;
						else trg = ((y >> 4) << 4) + 15;
						if (trg > (unitsPerBlock - 1)) trg = (unitsPerBlock - 1);
						
						y = trg;
						
						continue;
					}
					
					blockPos.set(x + myPosInTheLevel.getX(), y + myPosInTheLevel.getY(), z + myPosInTheLevel.getZ());
					BlockState state = states[(((x * unitsPerBlock) + y) * unitsPerBlock) + z] = section.getBlockState(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);
					addState(state);
				}
			}
		}
		
		return states;
	}
	
	// used for saving
	public UnitPallet getPallet() {
		return new UnitPallet(this);
	}
	
	public void loadPallet(UnitPallet pallet) {
		loadPallet(pallet, null);
	}
	
	public void loadPallet(UnitPallet pallet, HashSet<BlockPos> positionsWithBE) {
		HashMap<SectionPos, ChunkAccess> cache = new HashMap<>();
		final BlockState[] states = new BlockState[unitsPerBlock * unitsPerBlock * unitsPerBlock];
//		for (int i = 0; i < states.length; i++) states[i] = Blocks.AIR.defaultBlockState();
		pallet.acceptStates(states, false);
		try {
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int x = 0; x < unitsPerBlock; x++) {
				for (int z = 0; z < unitsPerBlock; z++) {
					int pX = SectionPos.blockToSectionCoord(x + myPosInTheLevel.getX());
					int pZ = SectionPos.blockToSectionCoord(z + myPosInTheLevel.getZ());
					ChunkAccess chunk = myLevel.getChunk(pX, pZ, ChunkStatus.FULL, false);
					BasicVerticalChunk vc = (BasicVerticalChunk) chunk;
					
					for (int y = 0; y < unitsPerBlock; y++) {
						int indx = (((x * unitsPerBlock) + y) * unitsPerBlock) + z;
						if (states[indx] == null) continue;
						if (states[indx] == Blocks.AIR.defaultBlockState()) continue;
						
						if (chunk == null) {
							chunk = myLevel.getChunk(pX, pZ, ChunkStatus.FULL, true);
							vc = (BasicVerticalChunk) chunk;
						}
						
						pos.set(x, y, z);
						BlockPos pz = getOffsetPos(pos);
						vc.setBlockFast(false, new BlockPos(pz.getX(), pz.getY(), pz.getZ()), states[indx], cache);
						vc.getSubChunk(pz.getY() >> 4).setUnsaved(true);
						
						addState(states[indx]);
						if (positionsWithBE != null)
							if (states[indx].hasBlockEntity())
								positionsWithBE.add(pz);
					}
				}
			}
		} catch (Throwable e) {
			RuntimeException ex = new RuntimeException(e.getMessage(), e);
			ex.setStackTrace(e.getStackTrace());
			Loggers.UNITSPACE_LOGGER.error("", e);
			throw ex;
		}
	}
	
	public BlockState getBlock(int x, int y, int z) {
		return myLevel.getBlockState(getOffsetPos(new BlockPos(x, y, z)));
	}
	
	public void setState(BlockPos relative, Block block) {
//		int indx = (((relative.getX() * 16) + relative.getY()) * 16) + relative.getZ();
//		states[indx] = block.defaultBlockState();
		BlockState st = block.defaultBlockState();
		myLevel.setBlockAndUpdate(getOffsetPos(relative), st);
	}
	
	public BlockPos getOffsetPos(BlockPos pos) {
		return pos.offset(myPosInTheLevel);
	}
	
	public BlockPos getOffsetPosMut(BlockPos.MutableBlockPos pos) {
		return pos.set(
				pos.getX() + myPosInTheLevel.getX(),
				pos.getY() + myPosInTheLevel.getY(),
				pos.getZ() + myPosInTheLevel.getZ()
		);
	}
	
	public void setFast(boolean allowSave, int x, int y, int z, BlockState state) {
		BlockPos pz = getOffsetPos(new BlockPos(x, y, z));
		BasicVerticalChunk vc = (BasicVerticalChunk) myLevel.getChunkAt(pz);
		vc.setBlockFast(allowSave, new BlockPos(x, pz.getY(), z), state, new HashMap<>());
	}
	
	public void clear() {
		HashMap<SectionPos, ChunkAccess> cache = new HashMap<>();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos posMod = new BlockPos.MutableBlockPos();
		for (int x = 0; x < unitsPerBlock; x++) {
			for (int z = 0; z < unitsPerBlock; z++) {
				int pX = SectionPos.blockToSectionCoord(x + myPosInTheLevel.getX());
				int pZ = SectionPos.blockToSectionCoord(z + myPosInTheLevel.getZ());
				ChunkAccess chunk = myLevel.getChunk(pX, pZ, ChunkStatus.FULL, false);
				if (chunk == null) continue;
				BasicVerticalChunk vc = (BasicVerticalChunk) chunk;
				
				for (int y = 0; y < unitsPerBlock; y++) {
					pos.set(myPosInTheLevel.getX() + x, myPosInTheLevel.getY() + y, myPosInTheLevel.getZ() + z);
					posMod.set(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
					vc.setBlockFast(posMod, Blocks.AIR.defaultBlockState(), cache);
					vc.removeBlockEntity(pos);
				}
			}
		}
	}
	
	public BlockEntity[] getTiles() {
		final BlockEntity[] states = new BlockEntity[unitsPerBlock * unitsPerBlock * unitsPerBlock];
		for (int x = 0; x < unitsPerBlock; x++) {
			for (int z = 0; z < unitsPerBlock; z++) {
				int pX = SectionPos.blockToSectionCoord(x + myPosInTheLevel.getX());
				int pZ = SectionPos.blockToSectionCoord(z + myPosInTheLevel.getZ());
				ChunkAccess chunk = myLevel.getChunk(pX, pZ, ChunkStatus.FULL, false);
				if (chunk == null) continue;
				
				for (int y = 0; y < unitsPerBlock; y++) {
					states[(((x * unitsPerBlock) + y) * unitsPerBlock) + z] = chunk.getBlockEntity(myPosInTheLevel.offset(x, y, z));
				}
			}
		}
		return states;
	}
	
	public CompoundTag serialize() {
		if (unitsPerBlock == 0) return null;
		if (this.myLevel == null) {
			if (level != null) tick();
			if (this.myLevel == null) return this.tag; // TODO: figure out if this still happens
		}
		CompoundTag tag = new CompoundTag();
		
		tag.putInt("x", pos.getX());
		tag.putInt("y", pos.getY());
		tag.putInt("z", pos.getZ());
		tag.putInt("upb", unitsPerBlock);
		tag.putBoolean("natural", isNatural);
		tag.putInt("countBlocks", numBlocks);
		
		return tag;
	}
	
	public RenderWorld getRenderWorld() {
		return wld;
	}
	
	public void sendSync(PacketTarget target) {
		SyncPacketS2C pkt = new SyncPacketS2C(this);
		SUNetworkRegistry.NETWORK_INSTANCE.send(target, pkt);
	}

	public void sendRemove(PacketTarget target) {
		RemoveUnitPacketS2C pkt = new RemoveUnitPacketS2C(this.pos, this.unitsPerBlock);
		SUNetworkRegistry.NETWORK_INSTANCE.send(target, pkt);
	}
	
	public void removeState(BlockState block) {
		if (!block.isAir()) {
			numBlocks -= 1;
			if (numBlocks < 0) {
				numBlocks = 0; // idk how this would happen
			}
		}
	}
	
	public void addState(BlockState block) {
		if (!block.isAir()) {
			numBlocks += 1;
		}
	}

	public boolean isEmpty() {
		// TODO: this doesn't work on client
		return numBlocks <= 0;
	}

	public Set<BasicVerticalChunk> getChunks() {
		Set<BasicVerticalChunk> chunks = new HashSet<>();
		if (myLevel == null) return chunks;
		for (int x = minBlock(Direction.Axis.X); x <= maxBlock(Direction.Axis.X); x += 16) {
			int pX = SectionPos.blockToSectionCoord(x);

			for (int z = minBlock(Direction.Axis.Z); z <= maxBlock(Direction.Axis.Z); z += 16) {
				int pZ = SectionPos.blockToSectionCoord(z);

				ChunkAccess chunk = myLevel.getChunk(pX, pZ, ChunkStatus.FULL, false);
				if (chunk == null) continue;
				BasicVerticalChunk vc = (BasicVerticalChunk) chunk;

				for (int y = minBlock(Direction.Axis.Y); y <= maxBlock(Direction.Axis.Y); y += 16) {
					chunks.add(vc.getSubChunk((y) >> 4));
				}
			}
		}
		return chunks;
	}

	public List<UnitSpace[]> getPotentiallyNestedSpaces() {
		ArrayList<UnitSpace[]> nestedSpaces = new ArrayList<>();
		for (BasicVerticalChunk chunk : getChunks()) {
			ISUCapability cap = SUCapabilityManager.getCapability(chunk);
			nestedSpaces.add(cap.getUnits());
		}
		return nestedSpaces;
	}

	public boolean contains(UnitSpace unitSpace) {
		if (unitSpace.level == myLevel) {
			return unitSpace.pos.getX() >= myPosInTheLevel.getX() &&
					unitSpace.pos.getY() >= myPosInTheLevel.getY() &&
					unitSpace.pos.getZ() >= myPosInTheLevel.getZ() &&
					unitSpace.pos.getX() < myPosInTheLevel.getX() + unitsPerBlock &&
					unitSpace.pos.getY() < myPosInTheLevel.getY() + unitsPerBlock &&
					unitSpace.pos.getZ() < myPosInTheLevel.getZ() + unitsPerBlock;
		}
		return false;
	}
}

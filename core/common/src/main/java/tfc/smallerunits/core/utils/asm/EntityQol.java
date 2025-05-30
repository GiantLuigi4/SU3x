package tfc.smallerunits.core.utils.asm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tfc.smallerunits.core.data.storage.RegionPos;
import tfc.smallerunits.core.simulation.chunk.BasicVerticalChunk;
import tfc.smallerunits.core.utils.math.HitboxScaling;
import tfc.smallerunits.level.SimpleTickerLevel;

import java.util.Set;
import java.util.function.BiConsumer;

public class EntityQol {
	protected static void forEachBlock(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Level level, BiConsumer<BlockPos, ChunkAccess> function) {
		BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
		for (int l1 = minX; l1 < maxX; ++l1) {
			for (int j2 = minZ; j2 < maxZ; ++j2) {
				int pX = SectionPos.blockToSectionCoord(l1);
				int pZ = SectionPos.blockToSectionCoord(j2);
				ChunkAccess chunk = level.getChunk(pX, pZ, ChunkStatus.FULL, false);
				if (chunk == null) continue;
				
				for (int i2 = minY; i2 < maxY; ++i2) {
					blockpos$mutableblockpos.set(l1, i2, j2);
					function.accept(blockpos$mutableblockpos, chunk);
				}
			}
		}
	}
	
	public static boolean inAnyFluid(AABB box, Level level, RegionPos regionPos) {
		box = box.move(0, -0.6, 0);
		Vec3 center = box.getCenter();
		AABB aabb = HitboxScaling.getOffsetAndScaledBox(box, new Vec3(center.x, box.minY, center.z), ((SimpleTickerLevel) level).getUPB(), regionPos);
		
		int minX = Mth.floor(aabb.minX);
		int minY = Mth.ceil(aabb.maxX);
		int minZ = Mth.floor(aabb.minY);
		int maxX = Mth.ceil(aabb.maxY);
		int maxY = Mth.floor(aabb.minZ);
		int maxZ = Mth.ceil(aabb.maxZ);
		
		BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
		for (int x = minX; x < maxX; ++x) {
			for (int z = minZ; z < maxZ; ++z) {
				{
					AABB box1 = new AABB(
							x, minY, z,
							(x + 1), maxY, (z + 1)
					);
					if (!box1.intersects(aabb)) {
						continue;
					}
				}
				
				int pX = SectionPos.blockToSectionCoord(x);
				int pZ = SectionPos.blockToSectionCoord(z);
				BasicVerticalChunk chunk = (BasicVerticalChunk) level.getChunk(pX, pZ, ChunkStatus.FULL, false);
				if (chunk == null) {
					z = (z | 0xF + 1);
					continue;
				}
				
				for (int y = minY; y < maxY; ++y) {
					int sectionIndex = chunk.getSectionIndex(y);
					LevelChunkSection section = chunk.getSectionNullable(sectionIndex);
					if (section == null || section.hasOnlyAir()) {
						y = (y | 0xF + 1);
						continue;
					}
					
					blockpos$mutableblockpos.set(x, y, z);
					if (!chunk.getBlockState(blockpos$mutableblockpos).getFluidState().isEmpty())
						return true;
				}
			}
		}
		
		return false;
	}
	
	public static void runSUFluidEyeCheck(Entity entity, Set<TagKey<Fluid>> fluidOnEyes, Level level, RegionPos regionPos) {
		fluidOnEyes.clear();
		// TODO: whatever the heck this is
//		Entity mount = entity.getVehicle();
//		if (mount instanceof Boat) {
//			Boat boat = (Boat) mount;
//			if (!boat.isUnderWater() && boat.getBoundingBox().maxY >= d0 && boat.getBoundingBox().minY <= d0) {
//				return;
//			}
//		}
		
		// TODO: don't scale the whole box
		AABB box = HitboxScaling.getOffsetAndScaledBox(entity.getBoundingBox(), entity.getPosition(0), ((SimpleTickerLevel) level).getUPB(), regionPos);
		double d0 = box.minY + (entity.getEyeHeight() * ((SimpleTickerLevel) level).getUPB());
		Vec3 vec = box.getCenter();
		BlockPos blockpos = new BlockPos((int) vec.x, (int) d0, (int) vec.z);
		FluidState fluidstate = level.getFluidState(blockpos);
		double d1 = (float) blockpos.getY() + fluidstate.getHeight(level, blockpos);
		if (d1 > d0) {
			fluidstate.getTags().forEach(fluidOnEyes::add);
		}
	}
	
	public static BlockState getSUBlockAtFeet(Entity entity, Level level, RegionPos regionPos) {
		// TODO: don't scale the whole bounding box
		AABB box = HitboxScaling.getOffsetAndScaledBox(entity.getBoundingBox(), entity.getPosition(0), ((SimpleTickerLevel) level).getUPB(), regionPos);
		double d0 = box.minY;
		Vec3 vec = box.getCenter();
		BlockPos blockpos = new BlockPos((int) vec.x, (int) d0, (int) vec.z);
		return level.getBlockState(blockpos);
	}
}

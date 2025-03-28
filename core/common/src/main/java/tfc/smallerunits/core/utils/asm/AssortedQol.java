package tfc.smallerunits.core.utils.asm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfc.smallerunits.core.UnitEdge;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.UnitSpaceBlock;
import tfc.smallerunits.core.client.abstraction.VanillaFrustum;
import tfc.smallerunits.core.client.render.TileRendererHelper;
import tfc.smallerunits.core.client.render.util.SUTesselator;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.core.data.capability.SUCapabilityManager;
import tfc.smallerunits.core.data.storage.RegionPos;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.core.utils.config.ClientConfig;
import tfc.smallerunits.core.utils.math.HitboxScaling;
import tfc.smallerunits.core.utils.selection.MutableVec3;
import tfc.smallerunits.core.utils.selection.UnitHitResult;
import tfc.smallerunits.core.utils.selection.UnitShape;
import tfc.smallerunits.plat.internal.ToolProvider;
import tfc.smallerunits.plat.util.PlatformProvider;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AssortedQol {
	public static FogType getFogType(Level level, RegionPos regionPos, Vec3 position, Vec3 camPos) {
		position = position.scale(1d / ((ITickerLevel) level).getUPB()).add(camPos);
		
		BlockPos pos = regionPos.toBlockPos();
		position = position.subtract(pos.getX(), pos.getY(), pos.getZ());
		position = position.scale(((ITickerLevel) level).getUPB());
		
		BlockPos ps = new BlockPos((int) position.x, (int) position.y, (int) position.z);
		BlockState block = level.getBlockState(ps);
		FluidState fluid = block.getFluidState();
		if (fluid.is(FluidTags.LAVA)) {
			if (position.y <= (double) (fluid.getHeight(level, ps) + (float) ps.getY())) {
				return FogType.LAVA;
			}
		} else if (fluid.is(FluidTags.WATER)) {
			if (position.y <= (double) (fluid.getHeight(level, ps) + (float) ps.getY())) {
				return FogType.WATER;
			}
		} else if (block.is(Blocks.POWDER_SNOW)) {
			return FogType.POWDER_SNOW;
		}
		
		return FogType.NONE;
	}
	
	public static void handleBlockInfo(HitResult block, CallbackInfoReturnable<List<String>> cir, List<String> strings) {
		if (block instanceof UnitHitResult result) {
			if (strings.get(strings.size() - 1).equals("smallerunits:unit_space")) {
				strings.remove(strings.size() - 1);
//				strings.remove(strings.size() - 1);
			} else {
				strings.add("");
			}
			
			Level level = Minecraft.getInstance().level;
			ISUCapability capability = SUCapabilityManager.getCapability(level, new ChunkPos(result.getBlockPos()));
			UnitSpace space = capability.getUnit(result.getBlockPos());
			
			Vec3 look = Minecraft.getInstance().cameraEntity.getViewVector(0);
			
			BlockPos blockpos;
			
			if (ClientConfig.DebugOptions.fastF3) {
				blockpos = space.getOffsetPos(result.geetBlockPos());
			} else {
				Vec3 start;
				Vec3 end;
				if (true) {
					start = block.getLocation().subtract(look.scale(1d / space.unitsPerBlock));
					end = block.getLocation().add(look.scale(1d / space.unitsPerBlock));
				} else {
					start = Minecraft.getInstance().cameraEntity.getEyePosition(0);
					end = Minecraft.getInstance().cameraEntity.getEyePosition(0).add(look.scale(20));
				}
				
				start = new Vec3(
						(start.x - (space.regionPos.x * 512)) * space.unitsPerBlock,
						(start.y - (space.regionPos.y * 512)) * space.unitsPerBlock,
						(start.z - (space.regionPos.z * 512)) * space.unitsPerBlock
				);
				end = new Vec3(
						(end.x - (space.regionPos.x * 512)) * space.unitsPerBlock,
						(end.y - (space.regionPos.y * 512)) * space.unitsPerBlock,
						(end.z - (space.regionPos.z * 512)) * space.unitsPerBlock
				);
				BlockHitResult result1 = space.getMyLevel().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, Minecraft.getInstance().cameraEntity));
				blockpos = result1.getBlockPos();
			}
			BlockState state = space.getMyLevel().getBlockState(blockpos);
			List<String> list = strings;

//			list.add(ChatFormatting.UNDERLINE + "Block: " + result.getBlockPos().getX() + ", " + result.getBlockPos().getY() + ", " + result.getBlockPos().getZ());
			list.add(ChatFormatting.ITALIC + "Targeted Small Block: " + blockpos.getX() + ", " + blockpos.getY() + ", " + blockpos.getZ());
			list.add(ChatFormatting.ITALIC + "World: " + level.dimension().location() + "|" + space.regionPos.x + "|" + space.regionPos.y + "|" + space.regionPos.z + "|");
			list.add(ChatFormatting.ITALIC + "Scale: 1/" + space.unitsPerBlock);
//			list.add(String.valueOf((Object) Registry.BLOCK.getKey(state.getBlock())));
			list.add(String.valueOf(state.getBlockHolder().unwrapKey().get().location()));
			
			for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
				Property<?> property = entry.getKey();
				Comparable<?> comparable = entry.getValue();
				String s = Util.getPropertyName(property, comparable);
				if (Boolean.TRUE.equals(comparable)) {
					s = ChatFormatting.GREEN + s;
				} else if (Boolean.FALSE.equals(comparable)) {
					s = ChatFormatting.RED + s;
				}
				
				boolean isNumber = true;
				for (char c : s.toCharArray()) {
					if (!Character.isDigit(c)) {
						isNumber = false;
						break;
					}
				}
				if (isNumber) s = ChatFormatting.GOLD + s;
				
				list.add(property.getName() + ": " + s);
			}
			
			for (Object o : state.getTags().toArray()) {
				list.add("#" + ((TagKey<Block>) o).location());
			}
		}
	}
	
	public static boolean scaleRender(double vd, AABB renderBox, ITickerLevel tickerWorld, BlockPos pPos, Vec3 pCameraPos) {
		double sd = ToolProvider.RESIZING.getActualSize(Minecraft.getInstance().player);
		double divisor = tickerWorld.getUPB();
		
		if (sd > (1d / divisor)) sd = 1;
//			vd /= sd;
		
		vd *= divisor;
		divisor *= sd;
		
		if (divisor <= 1.001) {
			divisor = tickerWorld.getUPB();
			double sz = Math.max(Math.max(renderBox.getXsize(), renderBox.getYsize()), renderBox.getZsize());
			if (sz <= 1)
				return Vec3.atCenterOf(pPos).closerThan(pCameraPos, vd / Math.cbrt(divisor));
			else
				return Vec3.atCenterOf(pPos).closerThan(pCameraPos, vd);
		}
		
		double sz = renderBox.getSize();
		
		if (sz < 1) sz = 1;
		divisor /= sz;
		if (divisor < 1) divisor = 1;
		
		// TODO: check for a better scaling algo?
		return Vec3.atCenterOf(pPos).closerThan(pCameraPos, vd / Math.sqrt(divisor));
	}
	
	public static void scaleVert(SUTesselator.TranslatingBufferBuilder translatingBufferBuilder, double pX, double pY, double pZ, float scl, MutableVec3 coords, MutableVec3 offset) {
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
		coords.set(
				(pX + camera.getPosition().x) * scl - camera.getPosition().x + offset.x,
				(pY + camera.getPosition().y) * scl - camera.getPosition().y + offset.y,
				(pZ + camera.getPosition().z) * scl - camera.getPosition().z + offset.z
		);
	}
	
	
	public static void handleRenderOutline(Consumer<VoxelShape> renderShape, Level level, PoseStack pPoseStack, VertexConsumer pConsumer, Entity pEntity, double pCamX, double pCamY, double pCamZ, BlockPos pPos, BlockState pState, CallbackInfo ci) {
		if (pState.getBlock() instanceof UnitSpaceBlock) {
			VoxelShape shape = pState.getShape(level, pPos, CollisionContext.of(pEntity));
			if (shape instanceof UnitShape) {
				ci.cancel();
				HitResult result = Minecraft.getInstance().hitResult;
				
				if (result instanceof UnitHitResult) {
					BlockPos pos = ((UnitHitResult) result).geetBlockPos();
					LevelChunk chnk = level.getChunkAt(pPos);
					UnitSpace space = SUCapabilityManager.getCapability(chnk).getUnit(pPos);
					BlockState state = space.getBlock(pos.getX(), pos.getY(), pos.getZ());
					
					pPoseStack.pushPose();
					pPoseStack.translate(
							(double) pPos.getX() - pCamX,
							(double) pPos.getY() - pCamY,
							(double) pPos.getZ() - pCamZ
					);
					
					// TODO: better handling
					BlockPos pz = space.getOffsetPos(((UnitHitResult) result).geetBlockPos());
					VoxelShape shape1 = state.getShape(space.getMyLevel(), pz, CollisionContext.of(pEntity));
					if (state.getBlock() instanceof UnitSpaceBlock) {
						pPoseStack.scale(1f / space.unitsPerBlock, 1f / space.unitsPerBlock, 1f / space.unitsPerBlock);
						BlockPos ps = ((UnitHitResult) result).geetBlockPos();
						pPoseStack.translate(
								-pz.getX() + ps.getX(),
								-pz.getY() + ps.getY(),
								-pz.getZ() + ps.getZ()
						);
						
						/* calculate reach distance */
						double reach = 7;
						if (pEntity instanceof LivingEntity le) {
							reach = PlatformProvider.UTILS.getReach(le);
						}
						
						Vec3 start = new Vec3(pCamX, pCamY, pCamZ);
						Vec3 end = start.add(pEntity.getViewVector(1).scale(reach));
						HitboxScaling.scale(start, ((ITickerLevel) space.getMyLevel()));
						HitboxScaling.scale(end, ((ITickerLevel) space.getMyLevel()));
						
						/* render recursive */
						Minecraft.getInstance().hitResult = shape1.clip(start, end, pz);
						handleRenderOutline(
								renderShape,
								space.getMyLevel(), pPoseStack, pConsumer,
								pEntity, 0, 0, 0,
								pz, state, ci
						);
						Minecraft.getInstance().hitResult = result;
					} else if (shape1.isEmpty() || state.getBlock() instanceof UnitEdge) {
						/* draw edge */
						int x = pos.getX();
						int y = pos.getY();
						int z = pos.getZ();
						
						double upbDouble = space.unitsPerBlock;
						AABB box = ((UnitHitResult) result).getSpecificBox();
						if (box == null) {
							box = new AABB(
									x / upbDouble, y / upbDouble, z / upbDouble,
									(x + 1) / upbDouble, (y + 1) / upbDouble, (z + 1) / upbDouble
							);
						}
						
//						BlockState state1 = level.getBlockState(pPos.relative(((UnitHitResult) result).getDirection().getOpposite()));
//						VoxelShape shape2 = state1.getShape(level, pPos.relative(((UnitHitResult) result).getDirection().getOpposite()));

//						MutableAABB bob = (MutableAABB) new MutableAABB(
//								box.minX,
//								box.minY,
//								box.minZ,
//								box.maxX,
//								box.maxY,
//								box.maxZ
//						).scale(1.0 / upbDouble);
						
						shape1 = Shapes.create(box);
//						shape1 = Shapes.joinUnoptimized(
//								shape1, shape2,
//								BooleanOp.AND
//						);
						
						renderShape.accept(shape1);
					} else {
						/* draw block */
						pPoseStack.scale(1f / space.unitsPerBlock, 1f / space.unitsPerBlock, 1f / space.unitsPerBlock);
						pPoseStack.translate(pos.getX(), pos.getY(), pos.getZ());
						renderShape.accept(shape1);
					}
					
					pPoseStack.popPose();
				}
			}
		}
	}
	
	public static void drawIndicatorsRecursive(UnitSpace unit, BlockPos origin, boolean hammerHeld, PoseStack stk, VanillaFrustum SU$Frustum) {
		TileRendererHelper.drawUnit(
				SU$Frustum,
				new BlockPos(0, 0, 0), unit.unitsPerBlock, unit.isNatural,
				hammerHeld, unit.isEmpty(), null, stk,
				LightTexture.pack(0, 0),
//				origin.getX(), origin.getY(), origin.getZ()
				-unit.pos.getX(), -unit.pos.getY(), -unit.pos.getZ()
		);
	}
	
	public static void setupMatrix(UnitSpace space, PoseStack stk) {
		stk.scale(1f / space.unitsPerBlock, 1f / space.unitsPerBlock, 1f / space.unitsPerBlock);
	}
	
	public static boolean isInSection(UnitSpace space, BlockPos origin) {
		int y = space.pos.getY();
		if (y < origin.getY() + 16 &&
				y >= origin.getY()) {
			return true;
		}
		return false;
	}
}

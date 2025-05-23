package tfc.smallerunits.core.mixin.dangit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfc.smallerunits.core.data.access.EntityAccessor;
import tfc.smallerunits.core.networking.hackery.NetworkingHacks;

import javax.annotation.Nullable;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityAccessor {
	@Shadow
	private Vec3 position;
	
	@Shadow
	private BlockPos blockPosition;
	
	@Shadow
	@Nullable
	private BlockState feetBlockState;
	
	@Shadow
	private ChunkPos chunkPosition;
	
	@Shadow
	public Level level;

	@Unique
	private float SU$motionScalar = 1;
	
	@Override
	public void setLevel(Level level) {
		this.level = level;
	}
	
	@Override
	public void setPosRawNoUpdate(double pX, double pY, double pZ) {
		if (this.position.x != pX || this.position.y != pY || this.position.z != pZ) {
			this.position = new Vec3(pX, pY, pZ);
			int i = Mth.floor(pX);
			int j = Mth.floor(pY);
			int k = Mth.floor(pZ);
			if (i != this.blockPosition.getX() || j != this.blockPosition.getY() || k != this.blockPosition.getZ()) {
				this.blockPosition = new BlockPos(i, j, k);
				this.feetBlockState = null;
				if (SectionPos.blockToSectionCoord(i) != this.chunkPosition.x || SectionPos.blockToSectionCoord(k) != this.chunkPosition.z) {
					this.chunkPosition = new ChunkPos(this.blockPosition);
				}
			}

			//TODO fix this
			/*
			GameEventListenerRenderer gameeventlistenerregistrar = this.updateDynamicGameEventListener();
			if (gameeventlistenerregistrar != null) {
				gameeventlistenerregistrar.onListenerMove(this.level);
			}
			 */
		}
	}

	@Override
	public void setMotionScalar(float scl) {
		SU$motionScalar = scl;
	}
	
	@ModifyVariable(method = "move", at = @At("HEAD"), index = 2, argsOnly = true)
	public Vec3 modifyVector(Vec3 value) {
		return new Vec3(value.x * SU$motionScalar, value.y * SU$motionScalar, value.z * SU$motionScalar);
	}
	
	@Unique
	private static final ThreadLocal<NetworkingHacks.LevelDescriptor> descriptor = ThreadLocal.withInitial(() -> null);
	
	@Unique
	int recursion = 0;
	
	@Unique
	private void moveOut() {
		if (SU$motionScalar != 1 && recursion == 0) {
			descriptor.set(NetworkingHacks.unitPos.get());
			NetworkingHacks.setPos(null);
		}
		recursion++;
	}
	
	@Unique
	private void moveIn() {
		if (SU$motionScalar != 1 && recursion == 1) {
			descriptor.set(NetworkingHacks.unitPos.get());
			NetworkingHacks.setPos(null);
		}
		recursion--;
	}
	
	@Inject(at = @At("HEAD"), method = "remove")
	public void preRemove(Entity.RemovalReason pReason, CallbackInfo ci) {
		moveOut();
	}
	
	@Inject(at = @At("RETURN"), method = "remove")
	public void postRemove(Entity.RemovalReason pReason, CallbackInfo ci) {
		moveIn();
	}
	
	@Inject(at = @At("HEAD"), method = "gameEvent(Lnet/minecraft/world/level/gameevent/GameEvent;Lnet/minecraft/world/entity/Entity;)V")
	public void preRemove(GameEvent p_146853_, Entity p_146854_, CallbackInfo ci) {
		moveOut();
	}
	
	@Inject(at = @At("RETURN"), method = "gameEvent(Lnet/minecraft/world/level/gameevent/GameEvent;Lnet/minecraft/world/entity/Entity;)V")
	public void postRemove(GameEvent p_146853_, Entity p_146854_, CallbackInfo ci) {
		moveIn();
	}
}

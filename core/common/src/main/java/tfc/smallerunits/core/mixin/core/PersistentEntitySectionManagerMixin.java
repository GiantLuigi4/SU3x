package tfc.smallerunits.core.mixin.core;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfc.smallerunits.core.data.access.EntityManagerAccessor;
import tfc.smallerunits.level.SimpleTickerLevel;

@Mixin(PersistentEntitySectionManager.class)
public abstract class PersistentEntitySectionManagerMixin<T extends EntityAccess> implements EntityManagerAccessor<T>  {
	@Shadow @Final private EntitySectionStorage<T> sectionStorage;
	
	@Shadow public abstract LongSet getAllChunksToSave();
	
	@Inject(at = @At("HEAD"), method = "getEffectiveStatus", cancellable = true)
	private static <T extends EntityAccess> void preGetEffectiveStatus(T pEntity, Visibility pVisibility, CallbackInfoReturnable<Visibility> cir) {
		if (pEntity instanceof Entity) {
			if (((Entity) pEntity).level() instanceof SimpleTickerLevel) {
				cir.setReturnValue(Visibility.TICKING);
			}
		}
	}
	
	@Override
	public EntitySectionStorage<T> getSections() {
		return sectionStorage;
	}
	
	@Override
	public LongSet $getAllChunksToSave() {
		return getAllChunksToSave();
	}
}

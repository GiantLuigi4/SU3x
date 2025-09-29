package tfc.smallerunits.sodium.mixin;

import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import tfc.smallerunits.sodium.WorldSliceAccessor;

@Mixin(WorldSlice.class)
public class WorldSliceMixin implements WorldSliceAccessor {
	@Shadow
	@Final
	private ClientLevel world;
	
	@Override
	public ClientLevel getLevel() {
		return world;
	}
}

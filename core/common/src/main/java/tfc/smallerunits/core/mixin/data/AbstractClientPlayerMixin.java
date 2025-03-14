package tfc.smallerunits.core.mixin.data;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.core.data.capability.SUCapabilityManager;
import tfc.smallerunits.core.data.tracking.ICanUseUnits;
import tfc.smallerunits.core.networking.SUNetworkRegistry;
import tfc.smallerunits.core.networking.hackery.NetworkingHacks;
import tfc.smallerunits.core.networking.sync.RemoveUnitPacketC2S;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.plat.net.PacketTarget;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin implements ICanUseUnits {
	@Shadow
	public ClientLevel clientLevel;
	@Unique
	HitResult hit;
	
	@Override
	public HitResult actualResult() {
		return hit;
	}
	
	@Override
	public void setResult(HitResult result) {
		hit = result;
	}
	
	@Override
	public void removeUnit() {
		if (hit != null && clientLevel instanceof ITickerLevel) {
			Level lvl = ((ITickerLevel) clientLevel).getParent();
			
			if (hit.getType().equals(HitResult.Type.BLOCK)) {
				if (hit instanceof BlockHitResult result) {
					ISUCapability capability = SUCapabilityManager.getCapability(lvl, new ChunkPos(result.getBlockPos()));
					UnitSpace space = capability.getUnit(result.getBlockPos());
					if (space != null && space.isEmpty()) {
//						capability.removeUnit(result.getBlockPos());
						lvl.removeBlock(result.getBlockPos(), false);
						// TODO: move the networking hacks position to the parent world (if I setup my networking hacks to support recursion)
						NetworkingHacks.LevelDescriptor descriptor = NetworkingHacks.unitPos.get();
						NetworkingHacks.setPos(descriptor.parent());
						SUNetworkRegistry.NETWORK_INSTANCE.send(PacketTarget.SERVER, new RemoveUnitPacketC2S(result.getBlockPos()));
						NetworkingHacks.setPos(descriptor);
					}
				}
			}
		}
	}
}

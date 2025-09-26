package tfc.smallerunits.fabric.mixin.core.gui.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import tfc.smallerunits.core.UnitSpace;
import tfc.smallerunits.core.data.access.SUScreenAttachments;
import tfc.smallerunits.core.networking.hackery.NetworkingHacks;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.core.utils.PositionalInfo;

@Mixin(Screen.class)
public class ScreenMixin implements SUScreenAttachments {
	@Unique
	PositionalInfo info;
	@Unique
	Level targetLevel;
	@Unique
	NetworkingHacks.LevelDescriptor descriptor;
	
	@Override
	public void update(Player player) {
		if (info != null) {
			synchronized (this) {
				info = new PositionalInfo(player);
			}
		}
	}
	
	@Override
	public void setup(PositionalInfo info, UnitSpace unit) {
		this.info = info;
		targetLevel = unit.getMyLevel();
		descriptor = ((ITickerLevel) unit.getMyLevel()).getDescriptor();
	}
	
	@Override
	public void setup(PositionalInfo info, Level targetLevel, NetworkingHacks.LevelDescriptor descriptor) {
		this.info = info;
		this.targetLevel = targetLevel;
		this.descriptor = descriptor;
	}
	
	@Override
	public void setup(SUScreenAttachments attachments) {
		this.info = attachments.getPositionalInfo();
		this.targetLevel = attachments.getTarget();
		this.descriptor = attachments.getDescriptor();
	}
	
	@Override
	public PositionalInfo getPositionalInfo() {
		return info;
	}
	
	@Override
	public Level getTarget() {
		return targetLevel;
	}
	
	@Override
	public NetworkingHacks.LevelDescriptor getDescriptor() {
		return descriptor;
	}
}
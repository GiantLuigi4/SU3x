package tfc.smallerunits.plat.itf.access;

import net.minecraft.network.Connection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface PacketListenerAccessor {
	void setWorld(Level lvl);
	
	Player getPlayer();
	
	Connection getConnection();
}

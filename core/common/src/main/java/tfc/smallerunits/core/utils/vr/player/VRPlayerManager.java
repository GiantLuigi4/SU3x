package tfc.smallerunits.core.utils.vr.player;
//TODO fix vivecraft compat
//import net.minecraft.world.entity.player.Player;
//import org.vivecraft.forge.Vivecraft;
//import tfc.smallerunits.core.SmallerUnits;
//import tfc.smallerunits.plat.util.PlatformUtils;
//import tfc.smallerunits.core.utils.IHateTheDistCleaner;
//
//public class VRPlayerManager {
//	public static SUVRPlayer getPlayer(Player player) {
//		SUVRPlayer vrPlayer = getVivecraft(player);
//		return vrPlayer;
//	}
//
//	protected static SUVRPlayer getVivecraft(Player player) {
//		if (SmallerUnits.isVivecraftPresent()) {
//			if (player.level().isClientSide && PlatformUtils.isClient()) {
//				if (IHateTheDistCleaner.isClientPlayer(player)) return Vivecraft.getPlayerClient();
//				else return Vivecraft.getOtherClient(player);
//			} else return Vivecraft.getPlayerJRBudda(player);
//		}
//		return null;
//	}
//}

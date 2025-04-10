package tfc.smallerunits.sodium.mixin;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfc.smallerunits.plat.util.PlatformProvider;
import tfc.smallerunits.sodium.render.SodiumRenderMode;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public class SodiumDebugOverlayMixin {
	@Unique
	private static String getSodiumName() {
		if (PlatformProvider.UTILS.isLoaded("embeddium")) return "Embeddium";
		else if (PlatformProvider.UTILS.isLoaded("rubidium")) return "Rubidium";
		else if (PlatformProvider.UTILS.isLoaded("sodium")) return "Sodium";
		else if (PlatformProvider.UTILS.isLoaded("magnesium")) return "Magnesium";
		return null;
	}
	
	@Unique
	private static boolean isSodiumPresent() {
		if (PlatformProvider.UTILS.isLoaded("embeddium")) return true;
		else if (PlatformProvider.UTILS.isLoaded("rubidium")) return true;
		else if (PlatformProvider.UTILS.isLoaded("sodium")) return true;
		else if (PlatformProvider.UTILS.isLoaded("magnesium")) return true;
		return false;
	}
	
	@Inject(at = @At("TAIL"), method = "getSystemInformation")
	public void addText(CallbackInfoReturnable<List<String>> cir) {
		if (isSodiumPresent()) {
			String name = getSodiumName();
			if (name == null) throw new RuntimeException("???");
			
			List<String> strings = cir.getReturnValue();
			boolean foundSodium = false;
			int index = -1;
			
			for (int i = 0; i < strings.size(); i++) {
				String string = strings.get(i);
				if (foundSodium && string.isEmpty()) {
					index = i;
					break;
				} else if (!foundSodium && string.startsWith(name)) foundSodium = true;
			}
			
			if (index == -1) index = strings.size();
			
			if (foundSodium) {
				strings.add(index, "SU Renderer: " +
						SodiumRenderMode.VANILLA.formatting +
						SodiumRenderMode.VANILLA.f3Text
				);
			}
		}
	}
}

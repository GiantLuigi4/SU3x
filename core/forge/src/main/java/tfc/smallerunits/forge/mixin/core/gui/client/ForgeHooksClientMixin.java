package tfc.smallerunits.forge.mixin.core.gui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ScreenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tfc.smallerunits.core.data.access.SUScreenAttachments;
import tfc.smallerunits.core.networking.hackery.NetworkingHacks;
import tfc.smallerunits.core.simulation.level.ITickerLevel;
import tfc.smallerunits.core.utils.PositionalInfo;

@Mixin(value = ForgeHooksClient.class, remap = false)
public class ForgeHooksClientMixin {
	@Unique
	private static final ThreadLocal<Screen> currentScreen = new ThreadLocal<>();
	
	@Inject(at = @At("HEAD"), method = "drawScreenInternal")
	private static void preDrawScreen(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		currentScreen.set(screen);
		if (Minecraft.getInstance().player != null) {
			SUScreenAttachments screenAttachments = ((SUScreenAttachments) screen);
			PositionalInfo info = screenAttachments.getPositionalInfo();
			if (info != null) {
				screenAttachments.update(Minecraft.getInstance().player);
				NetworkingHacks.setPos(((ITickerLevel) screenAttachments.getTarget()).getDescriptor());
				info.adjust(Minecraft.getInstance().player, Minecraft.getInstance().level, screenAttachments.getDescriptor(), false);
			}
		}
	}
	
	@Inject(at = @At("RETURN"), method = "drawScreenInternal")
	private static void postDrawScreen(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (Minecraft.getInstance().player != null) {
			SUScreenAttachments screenAttachments = ((SUScreenAttachments) screen);
			PositionalInfo info = screenAttachments.getPositionalInfo();
			if (info != null) {
				info.reset(Minecraft.getInstance().player);
				NetworkingHacks.unitPos.remove();
				
				if (Minecraft.getInstance().screen != null && Minecraft.getInstance().screen != currentScreen.get()) {
					SUScreenAttachments attachments = (SUScreenAttachments) Minecraft.getInstance().screen;
					attachments.setup(screenAttachments);
				}
			}
		}
	}
	
	@Inject(at = @At("HEAD"), method = "onScreenPotionSize")
	private static void preScreenPotionSize(Screen screen, int availableSpace, boolean compact, int horizontalOffset, CallbackInfoReturnable<ScreenEvent.RenderInventoryMobEffects> cir) {
		currentScreen.set(screen);
		if (Minecraft.getInstance().player != null) {
			SUScreenAttachments screenAttachments = ((SUScreenAttachments) screen);
			PositionalInfo info = screenAttachments.getPositionalInfo();
			if (info != null) {
				NetworkingHacks.setPos(((ITickerLevel) screenAttachments.getTarget()).getDescriptor());
				info.adjust(Minecraft.getInstance().player, Minecraft.getInstance().level, screenAttachments.getDescriptor(), false);
			}
		}
	}
	
	@Inject(at = @At("RETURN"), method = "onScreenPotionSize")
	private static void postScreenPotionSize(Screen screen, int availableSpace, boolean compact, int horizontalOffset, CallbackInfoReturnable<ScreenEvent.RenderInventoryMobEffects> cir) {
		if (Minecraft.getInstance().player != null) {
			SUScreenAttachments screenAttachments = ((SUScreenAttachments) screen);
			PositionalInfo info = screenAttachments.getPositionalInfo();
			if (info != null) {
				info.reset(Minecraft.getInstance().player);
				NetworkingHacks.unitPos.remove();
				
				if (Minecraft.getInstance().screen != null && Minecraft.getInstance().screen != currentScreen.get()) {
					SUScreenAttachments attachments = (SUScreenAttachments) Minecraft.getInstance().screen;
					attachments.setup(screenAttachments);
				}
			}
		}
	}
}

package tfc.smallerunits.flywheel.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(
		value = LevelRenderer.class,
		priority = 1001 /* apparently, this seems to allow me to inject where an overwrite happens? */
)
public class LevelRendererMixin {
	@Shadow @Nullable public ClientLevel level;
	
	@Inject(
			at = {@At("HEAD")},
			method = {"setupRender"}
	)
	private void setupRender(Camera camera, Frustum frustum, boolean queue, boolean isSpectator, CallbackInfo ci) {
//		for (Region value : ((RegionalAttachments) level).SU$getRegionMap().values()) {
//			for (Level valueLevel : value.getLevels()) {
//				if (valueLevel != null) {
//					MinecraftForge.EVENT_BUS.post(new BeginFrameEvent((ClientLevel) valueLevel, camera, frustum));// 46
//				}
//			}
//		}
	}
}

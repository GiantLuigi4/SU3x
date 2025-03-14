//package tfc.smallerunits.forge.mixin.compat.chisel_and_bits;
//
//import mod.chiselsandbits.block.entities.ChiseledBlockEntity;
//import mod.chiselsandbits.client.model.data.ChiseledBlockModelDataExecutor;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import tfc.smallerunits.simulation.level.client.ForgeTickerClientLevel;
//
//@Mixin(value = ChiseledBlockModelDataExecutor.class, remap = false)
//public class ChiselAndBitsMeshMixin {
//	@Inject(at = @At("HEAD"), method = "lambda$updateModelDataCore$5", cancellable = true)
//	private static void preUpdate(ChiseledBlockEntity tileEntity, CallbackInfo ci) {
//		//noinspection UnnecessaryLocalVariable
//		BlockEntity be = tileEntity; // yes, this is required... I don't now why
//		if (be.getLevel() instanceof ForgeTickerClientLevel fakeLevel) {
//			fakeLevel.getModelDataManager().requestRefresh(tileEntity);
//			fakeLevel.sendBlockUpdated(
//					be.getBlockPos(),
//					be.getBlockState(),
//					be.getBlockState(),
//					8
//			);
//			ci.cancel();
//		}
//	}
//}

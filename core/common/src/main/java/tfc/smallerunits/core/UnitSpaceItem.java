package tfc.smallerunits.core;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import tfc.smallerunits.core.data.capability.ISUCapability;
import tfc.smallerunits.core.data.capability.SUCapabilityManager;
import tfc.smallerunits.core.utils.config.ServerConfig;
import tfc.smallerunits.level.SimpleTickerLevel;
import tfc.smallerunits.plat.net.PacketTarget;
import tfc.smallerunits.plat.util.AbstractItem;
import tfc.smallerunits.plat.util.ClientInitContext;

import java.util.List;
import java.util.function.Consumer;

public class UnitSpaceItem extends AbstractItem {
	public UnitSpaceItem() {
		super(
				new Properties()
//						.rarity(Rarity.create("su", ChatFormatting.GREEN))
		);
	}
	
	@Override
	public void initializeClient(ClientInitContext ctx) {
		ctx.registerRenderer(SUItemRenderer::new);
	}
	
	@Override
	public InteractionResult useOn(UseOnContext pContext) {
		if (pContext.getLevel() instanceof SimpleTickerLevel) return InteractionResult.FAIL;
		
		BlockPlaceContext ctx = new BlockPlaceContext(pContext);
		BlockPos pos = ctx.getClickedPos();
		LevelChunk chunk = pContext.getLevel().getChunkAt(pos);
		ISUCapability capability = SUCapabilityManager.getCapability(chunk);
		if (capability.getUnit(pos) == null) {
			if (ctx.replacingClickedOnBlock() || pContext.getLevel().getBlockState(pos).isAir()) {
				UnitSpace space = capability.getOrMakeUnit(pos);
				if (pContext.getItemInHand().hasTag()) {
					CompoundTag tag = pContext.getItemInHand().getTag();
					assert tag != null;
					if (tag.contains("upb", Tag.TAG_INT))
						space.setUpb(Math.min(ServerConfig.SizeOptions.maxScale, Math.max(ServerConfig.SizeOptions.minScale, tag.getInt("upb"))));
					else space.setUpb(ServerConfig.SizeOptions.defaultScale);
				} else space.setUpb(ServerConfig.SizeOptions.defaultScale);
				pContext.getLevel().setBlockAndUpdate(pos, Registry.UNIT_SPACE.get().defaultBlockState());
				chunk.setUnsaved(true);
				if (chunk.getLevel() instanceof ServerLevel)
					space.sendSync(PacketTarget.trackingChunk(chunk));
				space.isNatural = false;
				space.tick();
				
				return InteractionResult.SUCCESS;
			}
		}
		return super.useOn(pContext);
	}
	
	@Override
	public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
		int upb = 4;
		if (pStack.hasTag()) {
			CompoundTag tag = pStack.getTag();
			if (tag.contains("upb", Tag.TAG_INT)) {
				upb = tag.getInt("upb");
			}
		}
		pTooltipComponents.add(
				Component.translatable("smallerunits.tooltip.scale", upb)
						.withStyle(ChatFormatting.GRAY)
		);
		super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
	}
	
	public void populateTab(Consumer<ItemStack> out) {
		for (int i = 2; i <= 16; i++) {
			ItemStack stack = new ItemStack(this);
			stack.getOrCreateTag().putInt("upb", i);
			out.accept(stack);
		}
	}
}

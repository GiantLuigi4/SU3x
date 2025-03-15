package tfc.smallerunits.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import tfc.smallerunits.plat.internal.ToolProvider;

import static tfc.smallerunits.core.utils.config.ServerConfig.GameplayOptions;

public class TileResizingItem extends Item {
	private final int scale;
	
	public TileResizingItem(int scale) {
		super(new Properties().stacksTo(1));
		this.scale = scale;
	}
	
	public int getScale() {
		return -scale;
	}
	
	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (GameplayOptions.resizeOther) {
			if (ToolProvider.RESIZING.isResizingModPresent()) {
				if (target instanceof Player && attacker instanceof ServerPlayer) {
					((ServerPlayer) attacker).getAdvancements().award(((ServerPlayer) attacker).level().getServer().getAdvancements().getAdvancement(new ResourceLocation("smallerunits:rude")), "strike_player");
				}
			}
			ToolProvider.RESIZING.resize(target, getScale());
		}
		return GameplayOptions.hurtOther;
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
		if (GameplayOptions.resizeSelf) {
			if (playerIn.isCrouching()) {
				ToolProvider.RESIZING.resize(playerIn, getScale());
			}
		}
		return super.use(worldIn, playerIn, handIn);
	}
}

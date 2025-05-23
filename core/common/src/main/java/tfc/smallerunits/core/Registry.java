package tfc.smallerunits.core;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import tfc.smallerunits.plat.PlatformRegistry;
import tfc.smallerunits.plat.util.PlatformProvider;

import java.util.function.Supplier;

public class Registry {
	public static final PlatformRegistry<Block> BLOCK_REGISTER = PlatformRegistry.makeRegistry(Block.class, "smallerunits");
	public static final PlatformRegistry<Item> ITEM_REGISTER = PlatformRegistry.makeRegistry(Item.class, "smallerunits");
	public static final PlatformRegistry<CreativeModeTab> TAB_REGISTER = PlatformRegistry.makeRegistry(CreativeModeTab.class, "smallerunits");
	public static final Supplier<Item> UNIT_SPACE_ITEM = ITEM_REGISTER.register("unit_space", UnitSpaceItem::new);
	public static final Supplier<Item> SHRINKER = ITEM_REGISTER.register("su_shrinker", () -> new TileResizingItem(-1));
	public static final Supplier<Item> GROWER = ITEM_REGISTER.register("su_grower", () -> new TileResizingItem(1));
	
	public static final Supplier<CreativeModeTab> TAB = TAB_REGISTER.register("su_tab", () ->
			PlatformProvider.UTILS.tab(
							"su_tab",
							() -> SHRINKER.get().getDefaultInstance()
					)
					.setTitle(Component.translatable("itemGroup.Smaller Units"))
					.addItem(() -> SHRINKER.get().getDefaultInstance())
					.addItem(() -> GROWER.get().getDefaultInstance())
					.addItems((out) -> ((UnitSpaceItem) UNIT_SPACE_ITEM.get()).populateTab(out))
					.build()
	);
	
	public static final Supplier<Block> UNIT_SPACE = BLOCK_REGISTER.register("unit_space", UnitSpaceBlock::new);
	// TODO: don't register this, maybe?
	public static final Supplier<Block> UNIT_EDGE = BLOCK_REGISTER.register("unit_edge", UnitEdge::new);
}

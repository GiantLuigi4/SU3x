package tfc.smallerunits.core.crafting;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import tfc.smallerunits.core.Registry;
import tfc.smallerunits.core.TileResizingItem;
import tfc.smallerunits.core.UnitSpaceItem;
import tfc.smallerunits.core.utils.config.ServerConfig;

public class RecipeUtils {
	public static NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
		NonNullList<ItemStack> nonnulllist = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
		
		for (int i = 0; i < nonnulllist.size(); ++i) {
			ItemStack item = inv.getItem(i);
			if (item.getItem() instanceof TileResizingItem) {
				ItemStack newStack = item.copy();
				nonnulllist.set(i, newStack);
			}
		}
		
		return nonnulllist;
	}
	
	public static boolean matchesShapelessResizing(CraftingContainer inventory) {
		int count = 0;
		UnitSpaceItem unitIfPresent = null;
		TileResizingItem hammer = null;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i).copy();
			count += stack.isEmpty() ? 0 : 1;
			if (count > 2) return false;
			else if (stack.getItem() instanceof UnitSpaceItem && unitIfPresent != null) return false;
			else if (stack.getItem() instanceof TileResizingItem && hammer != null) return false;
			else if (stack.getItem() instanceof UnitSpaceItem) unitIfPresent = ((UnitSpaceItem) stack.getItem());
			else if (stack.getItem() instanceof TileResizingItem) hammer = ((TileResizingItem) stack.getItem());
			else if (!stack.isEmpty()) return false;
		}
		if (hammer == null) return false;
		return hammer.getScale() > 0 || unitIfPresent != null;
	}
	
	public static Pair<ItemStack, ItemStack> getUnitAndHammer(CraftingContainer inventory) {
		int count = 0;
		ItemStack unitIfPresent = null;
		ItemStack hammer = null;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i).copy();
			count += stack.isEmpty() ? 0 : 1;
			if (count > 2) return null;
			else if (stack.getItem() instanceof UnitSpaceItem) unitIfPresent = stack;
			else if (stack.getItem() instanceof TileResizingItem) hammer = stack;
			else if (!stack.isEmpty()) return null;
		}
		if (unitIfPresent == null) {
			unitIfPresent = new ItemStack(Registry.UNIT_SPACE_ITEM.get());
			CompoundTag nbt = unitIfPresent.getOrCreateTag();
			nbt.putInt("upb", ServerConfig.SizeOptions.defaultScale - 1);
		}
		return Pair.of(unitIfPresent, hammer);
	}
}
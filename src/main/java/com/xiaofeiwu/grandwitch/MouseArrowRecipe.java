package com.xiaofeiwu.grandwitch;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Eight arrows round a potion of the mouse (to drink, to throw, or to linger: any), in the way tipped arrows are made, make eight arrows with the mouse on them. The bottle
 * comes back. (Not through the dragon's breath: this is for those who have not a dragon behind them.)
 */
public class MouseArrowRecipe extends CustomRecipe {

    public MouseArrowRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    static boolean potion(ItemStack s) {
        return (s.is(Items.POTION) || s.is(Items.SPLASH_POTION) || s.is(Items.LINGERING_POTION)) && PotionUtils.getPotion(s) == ModEffects.MOUSE_POTION.get();
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (container.getWidth() != 3 || container.getHeight() != 3) {
            return false;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack s = container.getItem(i);
            if (i == 4 ? !potion(s) : !s.is(Items.ARROW)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
        return PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW, 8), ModEffects.MOUSE_POTION.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> rest = super.getRemainingItems(container);
        if (container.getContainerSize() > 4 && potion(container.getItem(4))) {
            rest.set(4, new ItemStack(Items.GLASS_BOTTLE));
        }
        return rest;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MOUSE_ARROW.get();
    }
}

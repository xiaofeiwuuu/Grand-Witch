package com.xiaofeiwu.grandwitch;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.IBrewingRecipe;

/** An awkward potion and a plague virus, in a brewing stand, make a potion of the mouse. (Gunpowder, dragon's breath and so on then work on it as they do on any potion.) */
final class PlagueBrewing implements IBrewingRecipe {

    @Override
    public boolean isInput(ItemStack input) {
        return input.is(Items.POTION) && PotionUtils.getPotion(input) == Potions.AWKWARD;
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        return ingredient.is(ModItems.PLAGUE_VIRUS.get());
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        return isInput(input) && isIngredient(ingredient) ? ModEffects.drinkable() : ItemStack.EMPTY;
    }
}

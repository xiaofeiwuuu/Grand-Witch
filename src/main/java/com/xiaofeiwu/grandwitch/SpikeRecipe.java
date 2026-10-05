package com.xiaofeiwu.grandwitch;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** A potion of the mouse and a bit of food, anywhere in the grid, make a bit of food that looks as it did and has the potion in it; the bottle comes back. */
public class SpikeRecipe extends CustomRecipe {

    static final String SPIKED = "GrandWitchSpiked";

    public SpikeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    static boolean spiked(ItemStack s) {
        return !s.isEmpty() && s.hasTag() && s.getTag().getBoolean(SPIKED);
    }

    static boolean potion(ItemStack s) {
        return s.is(Items.POTION) && PotionUtils.getPotion(s) == ModEffects.MOUSE_POTION.get();
    }

    static boolean food(ItemStack s) {
        return s.isEdible() && !s.is(Items.POTION) && !spiked(s);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        int potions = 0, foods = 0, others = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (potion(s)) {
                potions++;
            } else if (food(s)) {
                foods++;
            } else {
                others++;
            }
        }
        return potions == 1 && foods == 1 && others == 0;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (food(s)) {
                ItemStack out = new ItemStack(s.getItem());
                CompoundTag tag = out.getOrCreateTag();
                tag.putBoolean(SPIKED, true);
                ListTag lore = new ListTag();
                lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("item.grandwitch.spiked_lore").withStyle(net.minecraft.ChatFormatting.GRAY))));
                tag.getCompound("display");                           // (made on use)
                CompoundTag display = out.getOrCreateTagElement("display");
                display.put("Lore", lore);
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> rest = super.getRemainingItems(container);
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (potion(container.getItem(i))) {
                rest.set(i, new ItemStack(Items.GLASS_BOTTLE));
            }
        }
        return rest;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SPIKE.get();
    }
}

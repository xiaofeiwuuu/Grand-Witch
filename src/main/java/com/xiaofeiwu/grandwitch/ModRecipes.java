package com.xiaofeiwu.grandwitch;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {

    static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, GrandWitchMod.MODID);

    public static final RegistryObject<RecipeSerializer<SpikeRecipe>> SPIKE = RECIPES.register("spike", () -> new SimpleCraftingRecipeSerializer<>(SpikeRecipe::new));

    public static final RegistryObject<RecipeSerializer<MouseArrowRecipe>> MOUSE_ARROW = RECIPES.register("mouse_arrow", () -> new SimpleCraftingRecipeSerializer<>(MouseArrowRecipe::new));

    private ModRecipes() {
    }
}

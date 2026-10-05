package com.xiaofeiwu.grandwitch;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GrandWitchMod.MODID)
public class GrandWitchMod {

    public static final String MODID = "grandwitch";

    public GrandWitchMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
            ModNetwork.register();
            net.minecraftforge.common.brewing.BrewingRecipeRegistry.addRecipe(new PlagueBrewing());
        }));
        ModBlocks.BLOCKS.register(bus);
        ModRecipes.RECIPES.register(bus);
        ModFeatures.FEATURES.register(bus);
        ModEffects.EFFECTS.register(bus);
        ModEffects.POTIONS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, WitchConfig.SPEC);
    }
}

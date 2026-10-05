package com.xiaofeiwu.grandwitch;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GrandWitchMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {

    private ModEvents() {
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GRAND_WITCH.get(), GrandWitch.createAttributes().build());
        event.put(ModEntities.VILLAGE_MOUSE.get(), VillageMouse.createAttributes().build());
    }

    /** A wild mouse comes up on any ground, in any light (the switch is in the config; the list of biomes is in the data pack). */
    @SubscribeEvent
    public static void placements(net.minecraftforge.event.entity.SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.VILLAGE_MOUSE.get(), net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawn, pos, random) -> WitchConfig.WILD_MICE.get() && net.minecraft.world.entity.Mob.checkMobSpawnRules(type, level, spawn, pos, random),
                net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void tabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.GRAND_WITCH_SPAWN_EGG);
            event.accept(ModItems.MOUSE_SPAWN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.MOUSE_HOLE);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.WOODEN_BROOM);
            event.accept(ModItems.GOLDEN_BROOM);
            event.accept(WitchDiary.create());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.PLAGUE_VIRUS);
        }
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ModEffects.drinkable());
            event.accept(ModEffects.splash());
            event.accept(ModItems.ANTIDOTE);
        }
    }
}

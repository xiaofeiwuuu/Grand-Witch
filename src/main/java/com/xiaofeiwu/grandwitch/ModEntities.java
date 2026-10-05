package com.xiaofeiwu.grandwitch;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {

    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, GrandWitchMod.MODID);

    /** MISC: she never spawns by herself, only when {@link WitchVisits} sends her, so she does not touch the monster limits. */
    public static final RegistryObject<EntityType<GrandWitch>> GRAND_WITCH = ENTITIES.register("grand_witch",
            () -> EntityType.Builder.of(GrandWitch::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(10).build("grand_witch"));

    /** A child villager turned into a mouse. */
    public static final RegistryObject<EntityType<VillageMouse>> VILLAGE_MOUSE = ENTITIES.register("village_mouse",
            () -> EntityType.Builder.of(VillageMouse::new, MobCategory.AMBIENT).sized(0.4F, 0.4F).clientTrackingRange(8).build("village_mouse"));

    /** A broom with someone on it. */
    public static final RegistryObject<EntityType<BroomEntity>> BROOM = ENTITIES.register("broom",
            () -> EntityType.Builder.<BroomEntity>of(BroomEntity::new, MobCategory.MISC).sized(1.0F, 0.4F).clientTrackingRange(10).updateInterval(1).build("broom"));

    private ModEntities() {
    }
}

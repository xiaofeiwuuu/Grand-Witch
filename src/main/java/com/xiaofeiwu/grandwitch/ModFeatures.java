package com.xiaofeiwu.grandwitch;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFeatures {

    static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, GrandWitchMod.MODID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> MOUSE_HOLE = FEATURES.register("mouse_hole", MouseHoleFeature::new);

    private ModFeatures() {
    }
}

package com.xiaofeiwu.grandwitch.client;

import com.xiaofeiwu.grandwitch.GrandWitchMod;
import com.xiaofeiwu.grandwitch.ModEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GrandWitchMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    static final ModelLayerLocation MOUSE = new ModelLayerLocation(new ResourceLocation(GrandWitchMod.MODID, "mouse"), "main");
    static final ModelLayerLocation GIRL_GEAR = new ModelLayerLocation(new ResourceLocation(GrandWitchMod.MODID, "grand_witch"), "girl_gear");
    static final ModelLayerLocation BROOM = new ModelLayerLocation(new ResourceLocation(GrandWitchMod.MODID, "broom"), "main");
    static final ModelLayerLocation REACH_ARM = new ModelLayerLocation(new ResourceLocation(GrandWitchMod.MODID, "grand_witch"), "reach_arm");
    static final ModelLayerLocation WITCH_HAT = new ModelLayerLocation(new ResourceLocation(GrandWitchMod.MODID, "grand_witch"), "hat");

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MOUSE, MouseModel::definition);
        event.registerLayerDefinition(WITCH_HAT, WitchHatLayer::definition);
        event.registerLayerDefinition(BROOM, BroomModel::definition);
        event.registerLayerDefinition(REACH_ARM, ReachArm::definition);
        event.registerLayerDefinition(GIRL_GEAR, GirlGearLayer::definition);
    }

    /** The grass on a tunnel is coloured as the grass of the place is. */
    @SubscribeEvent
    public static void colours(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level != null && pos != null ? net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(level, pos) : net.minecraft.world.level.GrassColor.getDefaultColor(),
                com.xiaofeiwu.grandwitch.ModBlocks.MOUSE_TUNNEL.get());
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GRAND_WITCH.get(), GrandWitchRenderer::new);
        event.registerEntityRenderer(ModEntities.VILLAGE_MOUSE.get(), VillageMouseRenderer::new);
        event.registerEntityRenderer(ModEntities.BROOM.get(), BroomRenderer::new);
    }
}

package com.xiaofeiwu.grandwitch.client;

import com.xiaofeiwu.grandwitch.GrandWitchMod;
import com.xiaofeiwu.grandwitch.VillageMouse;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** A child that is a mouse: the mouse, in a warmer fur than a player's. */
public class VillageMouseRenderer extends MobRenderer<VillageMouse, VillageMouseModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(GrandWitchMod.MODID, "textures/entity/mouse_child.png");

    public VillageMouseRenderer(EntityRendererProvider.Context context) {
        super(context, new VillageMouseModel(context.bakeLayer(ClientSetup.MOUSE)), 0.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(VillageMouse mouse) {
        return TEXTURE;
    }
}

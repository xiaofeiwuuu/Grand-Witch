package com.xiaofeiwu.grandwitch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xiaofeiwu.grandwitch.BroomEntity;
import com.xiaofeiwu.grandwitch.BroomKind;
import com.xiaofeiwu.grandwitch.GrandWitchMod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** A broom, wooden or golden, lying level under whoever is on it and turned the way it goes. */
public class BroomRenderer extends EntityRenderer<BroomEntity> {

    private static final ResourceLocation WOOD = new ResourceLocation(GrandWitchMod.MODID, "textures/entity/broom_wooden.png");
    private static final ResourceLocation GOLD = new ResourceLocation(GrandWitchMod.MODID, "textures/entity/broom_golden.png");

    private final BroomModel model;

    public BroomRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BroomModel(context.bakeLayer(ClientSetup.BROOM));
        this.shadowRadius = 0.3F;
    }

    @Override
    public void render(BroomEntity broom, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0F, 0.35F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        pose.scale(-1.0F, -1.0F, 1.0F);
        model.part().render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(broom))), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(broom, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(BroomEntity broom) {
        return broom.kind() == BroomKind.GOLD ? GOLD : WOOD;
    }
}

package com.xiaofeiwu.grandwitch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xiaofeiwu.grandwitch.GrandWitch;
import com.xiaofeiwu.grandwitch.GrandWitchMod;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** What the witch has with her when she is dressed as a girl: a wicker basket with a loaf and an apple in it, hanging from her right hand, and a straw hat on the one with the plaits. */
public class GirlGearLayer extends RenderLayer<GrandWitch, WitchModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(GrandWitchMod.MODID, "textures/entity/girl_gear.png");

    private final ModelPart basket;
    private final ModelPart strawHat;

    public GirlGearLayer(RenderLayerParent<GrandWitch, WitchModel> parent, ModelPart root) {
        super(parent);
        this.basket = root.getChild("basket");
        this.strawHat = root.getChild("straw_hat");
    }

    static LayerDefinition definition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // in the right arm's space: the arm is 12 long from the shoulder, and the handle is in the hand; the basket hangs under it
        root.addOrReplaceChild("basket", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, 0.0F, -3.5F, 7.0F, 4.0F, 7.0F)
                        .texOffs(0, 12).addBox(-3.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F)
                        .texOffs(0, 12).addBox(2.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F)
                        .texOffs(0, 18).addBox(-3.5F, -5.0F, -0.5F, 7.0F, 1.0F, 1.0F)
                        .texOffs(0, 21).addBox(-3.0F, -1.5F, -1.5F, 4.0F, 2.0F, 2.0F)
                        .texOffs(0, 26).addBox(0.5F, -1.0F, 0.5F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-1.0F, 16.0F, 0.0F));
        // in the head's space
        root.addOrReplaceChild("straw_hat", CubeListBuilder.create()
                        .texOffs(0, 31).addBox(-6.0F, -9.0F, -6.0F, 12.0F, 1.0F, 12.0F)
                        .texOffs(0, 45).addBox(-3.5F, -12.0F, -3.5F, 7.0F, 3.0F, 7.0F),
                PartPose.rotation(0.0F, 0.0F, -0.05F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, GrandWitch witch, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if ((witch.girl() != 1 && witch.girl() != 2) || witch.isInvisible()) {
            return;
        }
        var vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(witch, 0.0F);
        pose.pushPose();
        getParentModel().rightArm.translateAndRotate(pose);
        basket.xRot = -getParentModel().rightArm.xRot;        // it hangs straight down whatever the arm does
        basket.render(pose, vc, light, overlay);
        pose.popPose();
        if (witch.girl() == 2) {
            pose.pushPose();
            getParentModel().head.translateAndRotate(pose);
            strawHat.render(pose, vc, light, overlay);
            pose.popPose();
        }
    }
}

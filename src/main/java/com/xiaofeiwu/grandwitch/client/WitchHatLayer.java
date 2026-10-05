package com.xiaofeiwu.grandwitch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xiaofeiwu.grandwitch.GrandWitch;
import com.xiaofeiwu.grandwitch.GrandWitchMod;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** A tall pointed hat, in black with a purple band, made of a brim and four boxes each smaller than the one under it. */
public class WitchHatLayer extends RenderLayer<GrandWitch, WitchModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(GrandWitchMod.MODID, "textures/entity/grand_witch_gear.png");

    private final ModelPart hat;

    public WitchHatLayer(RenderLayerParent<GrandWitch, WitchModel> parent, ModelPart root) {
        super(parent);
        this.hat = root.getChild("hat");
    }

    static LayerDefinition definition() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("hat", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.0F, -9.0F, -6.0F, 12.0F, 1.0F, 12.0F)
                        .texOffs(0, 14).addBox(-4.0F, -13.0F, -4.0F, 8.0F, 4.0F, 8.0F)
                        .texOffs(0, 27).addBox(-3.0F, -17.0F, -3.0F, 6.0F, 4.0F, 6.0F)
                        .texOffs(0, 38).addBox(-2.0F, -21.0F, -2.0F, 4.0F, 4.0F, 4.0F)
                        .texOffs(0, 47).addBox(-1.0F, -24.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, GrandWitch witch, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (witch.isInvisible() || witch.girl() > 0) {
            return;
        }
        pose.pushPose();
        getParentModel().head.translateAndRotate(pose);
        hat.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, LivingEntityRenderer.getOverlayCoords(witch, 0.0F));
        pose.popPose();
    }
}

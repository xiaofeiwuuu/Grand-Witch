package com.xiaofeiwu.grandwitch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xiaofeiwu.grandwitch.VillageMouse;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** The mouse of {@link MouseModel}, as a model for an entity that walks by itself. */
final class VillageMouseModel extends EntityModel<VillageMouse> {

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart[] legs;

    VillageMouseModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
        this.legs = new ModelPart[]{root.getChild("leg_fl"), root.getChild("leg_fr"), root.getChild("leg_bl"), root.getChild("leg_br")};
    }

    @Override
    public void setupAnim(VillageMouse mouse, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = Mth.clamp(netHeadYaw, -60.0F, 60.0F) * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float swing = Mth.cos(limbSwing * 1.6F) * Math.min(1.0F, limbSwingAmount) * 1.1F;
        legs[0].xRot = swing;
        legs[1].xRot = -swing;
        legs[2].xRot = -swing;
        legs[3].xRot = swing;
        tail.yRot = Mth.sin(ageInTicks * 0.15F + limbSwing) * (0.25F + Math.min(1.0F, limbSwingAmount) * 0.5F);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        root.render(pose, vc, light, overlay, r, g, b, a);
    }
}

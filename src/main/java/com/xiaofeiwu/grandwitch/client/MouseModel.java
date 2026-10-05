package com.xiaofeiwu.grandwitch.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** A grey mouse about half a block long, with ears, a pink nose and a long thin tail; it stands on the ground at y = 24, as the game's models do. */
final class MouseModel {

    final ModelPart root;
    final ModelPart head;
    final ModelPart tail;
    final ModelPart[] legs;      // front left, front right, back left, back right

    MouseModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
        this.legs = new ModelPart[]{root.getChild("leg_fl"), root.getChild("leg_fr"), root.getChild("leg_bl"), root.getChild("leg_br")};
    }

    static LayerDefinition definition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -4.0F, -3.0F, 4.0F, 4.0F, 6.0F), PartPose.offset(0.0F, 22.0F, 0.0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 10).addBox(-1.5F, -1.5F, -3.0F, 3.0F, 3.0F, 3.0F)
                .texOffs(0, 19).addBox(-0.5F, -0.5F, -4.0F, 1.0F, 1.0F, 1.0F), PartPose.offset(0.0F, 20.0F, -3.0F));
        head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F), PartPose.offset(1.6F, -1.2F, -1.2F));
        head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F), PartPose.offset(-1.6F, -1.2F, -1.2F));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 21).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F);
        root.addOrReplaceChild("leg_fl", leg, PartPose.offset(1.2F, 22.0F, -2.0F));
        root.addOrReplaceChild("leg_fr", leg, PartPose.offset(-1.2F, 22.0F, -2.0F));
        root.addOrReplaceChild("leg_bl", leg, PartPose.offset(1.2F, 22.0F, 2.0F));
        root.addOrReplaceChild("leg_br", leg, PartPose.offset(-1.2F, 22.0F, 2.0F));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 24).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 7.0F, new CubeDeformation(-0.25F)), PartPose.offset(0.0F, 21.0F, 3.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }
}

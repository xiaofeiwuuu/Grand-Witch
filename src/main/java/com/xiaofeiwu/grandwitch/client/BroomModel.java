package com.xiaofeiwu.grandwitch.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** A broom: a long thin handle, and at the back of it a bunch of bristles bound on with a band. The front of it is -z. */
final class BroomModel {

    final ModelPart root;

    BroomModel(ModelPart root) {
        this.root = root;
    }

    static LayerDefinition definition() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("broom", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-0.5F, -0.5F, -12.0F, 1.0F, 1.0F, 26.0F)
                        .texOffs(0, 28).addBox(-3.0F, -2.5F, 12.0F, 6.0F, 5.0F, 10.0F)
                        .texOffs(34, 28).addBox(-1.2F, -1.2F, 10.5F, 2.4F, 2.4F, 2.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    ModelPart part() {
        return root.getChild("broom");
    }
}

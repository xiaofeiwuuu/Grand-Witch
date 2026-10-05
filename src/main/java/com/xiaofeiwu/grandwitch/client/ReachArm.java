package com.xiaofeiwu.grandwitch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xiaofeiwu.grandwitch.GrandWitch;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/** The witch's arm into a tunnel: a straight piece of arm for each stretch between the places it goes through, so it bends where the tunnel does. Drawn in the world's directions. */
final class ReachArm {

    private ReachArm() {
    }

    static LayerDefinition definition() {
        MeshDefinition mesh = new MeshDefinition();
        // as the player's right arm is on the skin, 4 wide and 12 long, with its long way along y
        mesh.getRoot().addOrReplaceChild("arm", CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** {@code pose} is at the witch's feet, not turned. */
    static void render(GrandWitch witch, ModelPart root, ResourceLocation texture, PoseStack pose, MultiBufferSource buffers, int light) {
        long[] blocks = witch.armPath();
        int out = witch.armTicks();
        if (blocks.length == 0 || out <= 0) {
            return;
        }
        ModelPart arm = root.getChild("arm");
        // from the hand, hanging down in front of her, in along the tunnel
        double yaw = Math.toRadians(witch.getYRot());
        List<Vec3> points = new ArrayList<>();
        points.add(new Vec3(-Math.sin(yaw) * 0.75D, 0.15D, Math.cos(yaw) * 0.75D));
        Vec3 feet = witch.position();
        for (long l : blocks) {
            BlockPos p = BlockPos.of(l);
            points.add(new Vec3(p.getX() + 0.5D - feet.x, p.getY() + 0.25D - feet.y, p.getZ() + 0.5D - feet.z));
        }
        double total = 0.0D;
        for (int i = 1; i < points.size(); i++) {
            total += points.get(i).distanceTo(points.get(i - 1));
        }
        double left = total * Math.min(1.0D, out / (double) GrandWitch.ARM_FULL);
        var buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        for (int i = 1; i < points.size() && left > 0.0D; i++) {
            Vec3 a = points.get(i - 1), d = points.get(i).subtract(a);
            double length = d.length();
            if (length < 1.0E-4D) {
                continue;
            }
            double shown = Math.min(length, left);
            left -= shown;
            pose.pushPose();
            pose.translate(a.x, a.y, a.z);
            pose.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) (d.x / length), (float) (d.y / length), (float) (d.z / length)));
            pose.scale(1.0F, (float) (shown / 0.75D), 1.0F);          // the arm is 12 pixels, three quarters of a block, long
            arm.render(pose, buffer, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
}

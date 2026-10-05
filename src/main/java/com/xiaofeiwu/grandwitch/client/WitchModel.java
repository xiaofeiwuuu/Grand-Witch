package com.xiaofeiwu.grandwitch.client;

import com.xiaofeiwu.grandwitch.GrandWitch;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** The player's shape, as the witch has it; when she is a girl with a basket, her right arm is held out in front, bent, for it to hang from. */
public class WitchModel extends HumanoidModel<GrandWitch> {

    /** How far she leans forward from the hips, in degrees, when she is down at a tunnel. */
    static final float PRONE_LEAN = 77.0F;

    public WitchModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(GrandWitch witch, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(witch, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (witch.girl() == 1 || witch.girl() == 2) {
            rightArm.xRot = -0.85F + (float) Math.sin(limbSwing * 0.6662F) * 0.08F * Math.min(1.0F, limbSwingAmount * 3.0F);
            rightArm.yRot = -0.12F;
            rightArm.zRot = 0.05F;
        }
        // down at a tunnel's mouth: the whole of her is leant forward from the hips by the renderer, and the legs and the arms are put back to hang straight down
        rightArm.visible = true;
        if (witch.isProne()) {
            float lean = PRONE_LEAN * Mth.DEG_TO_RAD;
            rightLeg.xRot = leftLeg.xRot = -lean;
            rightLeg.yRot = -0.05F;
            leftLeg.yRot = 0.05F;
            leftArm.xRot = -lean - 0.15F;
            rightArm.xRot = -lean - 0.15F;
            rightArm.yRot = leftArm.yRot = 0.0F;
            rightArm.zRot = leftArm.zRot = 0.0F;
            head.xRot = -0.5F;
            rightArm.visible = witch.armTicks() <= 0;       // the one that is in the tunnel is drawn long, by the renderer
        }
    }
}

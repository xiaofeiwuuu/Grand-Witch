package com.xiaofeiwu.grandwitch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.xiaofeiwu.grandwitch.GrandWitch;
import com.xiaofeiwu.grandwitch.GrandWitchMod;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

/** A witch in a long purple coat, black gloves and a pointed hat; while she is disguised, the villager or trader she looks like is drawn in her place. */
public class GrandWitchRenderer extends HumanoidMobRenderer<GrandWitch, WitchModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(GrandWitchMod.MODID, "textures/entity/grand_witch.png");
    private static final ResourceLocation RED_GIRL = new ResourceLocation(GrandWitchMod.MODID, "textures/entity/girl_red.png");
    private static final ResourceLocation APRON_GIRL = new ResourceLocation(GrandWitchMod.MODID, "textures/entity/girl_apron.png");
    private static final ResourceLocation GRANNY = new ResourceLocation(GrandWitchMod.MODID, "textures/entity/granny.png");
    private static final ResourceLocation NANNY = new ResourceLocation(GrandWitchMod.MODID, "textures/entity/nanny.png");
    private static final Set<EntityType<?>> BROKEN = new HashSet<>();

    private final MouseForm mouseForm;
    private final net.minecraft.client.model.geom.ModelPart reachArm;

    /** The witch as a mouse: the mouse of the game's own mod, in a dark purple fur. */
    private static final class MouseForm extends net.minecraft.client.renderer.entity.MobRenderer<GrandWitch, WitchMouseModel> {

        private static final ResourceLocation TEXTURE = new ResourceLocation(GrandWitchMod.MODID, "textures/entity/mouse_witch.png");

        MouseForm(EntityRendererProvider.Context context) {
            super(context, new WitchMouseModel(context.bakeLayer(ClientSetup.MOUSE)), 0.2F);
        }

        @Override
        public ResourceLocation getTextureLocation(GrandWitch witch) {
            return TEXTURE;
        }
    }

    public GrandWitchRenderer(EntityRendererProvider.Context context) {
        super(context, new WitchModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.mouseForm = new MouseForm(context);
        this.reachArm = context.bakeLayer(ClientSetup.REACH_ARM);
        addLayer(new WitchHatLayer(this, context.bakeLayer(ClientSetup.WITCH_HAT)));
        addLayer(new GirlGearLayer(this, context.bakeLayer(ClientSetup.GIRL_GEAR)));
    }

    @Override
    public void render(GrandWitch witch, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (witch.isMouseForm()) {
            mouseForm.render(witch, yaw, partialTick, pose, buffers, light);
            return;
        }
        EntityType<?> type = witch.disguiseType();
        if (type != null && !BROKEN.contains(type)) {
            Entity dummy = witch.disguiseDummy();
            if (dummy != null) {
                try {
                    dummy.setYRot(witch.getYRot());
                    dummy.yRotO = witch.yRotO;
                    dummy.setXRot(witch.getXRot());
                    dummy.xRotO = witch.xRotO;
                    if (dummy instanceof LivingEntity living) {
                        living.yBodyRot = witch.yBodyRot;
                        living.yBodyRotO = witch.yBodyRotO;
                        living.yHeadRot = witch.yHeadRot;
                        living.yHeadRotO = witch.yHeadRotO;
                    }
                    entityRenderDispatcher.getRenderer(dummy).render(dummy, yaw, partialTick, pose, buffers, light);
                    return;
                } catch (RuntimeException e) {
                    BROKEN.add(type);
                    LogUtils.getLogger().warn("Grand Witch: could not draw her as {}, she will look like herself", type, e);
                }
            }
        }
        super.render(witch, yaw, partialTick, pose, buffers, light);
        if (witch.isProne()) {
            ReachArm.render(witch, reachArm, getTextureLocation(witch), pose, buffers, light);
        }
    }

    /** Bent over at the waist, down at a tunnel's mouth: she is leant forward about her hips (the model puts her legs and arms back to hang down). */
    @Override
    protected void setupRotations(GrandWitch witch, PoseStack pose, float ageInTicks, float yaw, float partialTick) {
        super.setupRotations(witch, pose, ageInTicks, yaw, partialTick);
        if (witch.isProne()) {
            pose.translate(0.0F, 0.75F, 0.0F);
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-WitchModel.PRONE_LEAN));
            pose.translate(0.0F, -0.75F, 0.0F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(GrandWitch witch) {
        return switch (witch.girl()) {
            case 1 -> RED_GIRL;
            case 2 -> APRON_GIRL;
            case 3 -> GRANNY;
            case 4 -> NANNY;
            default -> TEXTURE;
        };
    }
}

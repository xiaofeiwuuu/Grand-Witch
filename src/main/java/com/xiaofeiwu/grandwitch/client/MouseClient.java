package com.xiaofeiwu.grandwitch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xiaofeiwu.grandwitch.GrandWitchMod;
import com.xiaofeiwu.grandwitch.Mice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * What a mouse looks like, to others and to itself: a player is drawn as a mouse and not as a player (the name over it is not drawn), the arm
 * in front of the eyes is not drawn, and the whole view has a cold bluish cast with dark corners, as a mouse's would, who sees little colour.
 */
@Mod.EventBusSubscriber(modid = GrandWitchMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MouseClient {

    private static final ResourceLocation TEXTURE = new ResourceLocation(GrandWitchMod.MODID, "textures/entity/mouse.png");
    private static MouseModel model;

    private static final ResourceLocation VISION = new ResourceLocation(GrandWitchMod.MODID, "shaders/post/mouse_vision.json");
    private static boolean visionOn;
    private static boolean visionFailed;

    private MouseClient() {
    }

    /** From {@link com.xiaofeiwu.grandwitch.MousePacket}: the state, and the box to match it. */
    public static void apply(UUID id, boolean mouse) {
        Mice.setClient(id, mouse);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Player p = mc.level.getPlayerByUUID(id);
            if (p != null) {
                p.refreshDimensions();
            }
        }
    }

    private static MouseModel model() {
        if (model == null) {
            model = new MouseModel(Minecraft.getInstance().getEntityModels().bakeLayer(ClientSetup.MOUSE));
        }
        return model;
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Mice.clearClient();
        model = null;
        visionOn = false;
        visionFailed = false;
    }

    @SubscribeEvent
    public static void drawPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!Mice.isMouse(player)) {
            return;
        }
        event.setCanceled(true);
        MouseModel m = model();
        float pt = event.getPartialTick();
        float bodyYaw = Mth.rotLerp(pt, player.yBodyRotO, player.yBodyRot);
        float headYaw = Mth.rotLerp(pt, player.yHeadRotO, player.yHeadRot);
        float pitch = Mth.lerp(pt, player.xRotO, player.getXRot());
        float walkPos = player.walkAnimation.position(pt);
        float walkSpeed = Math.min(1.0F, player.walkAnimation.speed(pt));
        m.head.yRot = Mth.clamp(headYaw - bodyYaw, -60.0F, 60.0F) * Mth.DEG_TO_RAD;
        m.head.xRot = pitch * Mth.DEG_TO_RAD;
        float swing = Mth.cos(walkPos * 1.6F) * walkSpeed * 1.1F;
        m.legs[0].xRot = swing;
        m.legs[1].xRot = -swing;
        m.legs[2].xRot = -swing;
        m.legs[3].xRot = swing;
        m.tail.yRot = Mth.sin(player.tickCount * 0.15F + walkPos) * (0.25F + walkSpeed * 0.5F);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        VertexConsumer vc = event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        m.root.render(pose, vc, event.getPackedLight(), LivingEntityRenderer.getOverlayCoords(player, 0.0F));
        pose.popPose();
    }

    /** Colour drained and leaning one way, with red and yellow left bright, while the player is a mouse: a post effect, loaded and taken off with it. */
    @SubscribeEvent
    public static void vision(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean mouse = mc.player != null && Mice.isMouse(mc.player);
        if (mouse) {
            boolean ours = mc.gameRenderer.currentEffect() != null && VISION.toString().equals(mc.gameRenderer.currentEffect().getName());
            if (visionOn && !ours) {
                visionOn = false;                    // something else has taken the screen's effect
                visionFailed = true;
            }
            if (!visionOn && !visionFailed) {
                mc.gameRenderer.loadEffect(VISION);
                visionOn = mc.gameRenderer.currentEffect() != null && VISION.toString().equals(mc.gameRenderer.currentEffect().getName());
                visionFailed = !visionOn;            // it could not be loaded (the game says why in its log): not tried again every tick
            }
        } else if (visionOn || visionFailed) {
            if (visionOn && mc.gameRenderer.currentEffect() != null && VISION.toString().equals(mc.gameRenderer.currentEffect().getName())) {
                mc.gameRenderer.shutdownEffect();
            }
            visionOn = false;
            visionFailed = false;
        }
    }

    /** A narrower view: a mouse's eyes are low and close to what it looks at. */
    @SubscribeEvent
    public static void fov(net.minecraftforge.client.event.ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && event.getCamera().getEntity() == mc.player && Mice.isMouse(mc.player)) {
            event.setFOV(event.getFOV() * 0.85D);
        }
    }

    @SubscribeEvent
    public static void hideHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && Mice.isMouse(mc.player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void tint(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !Mice.isMouse(mc.player) || mc.options.hideGui) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        if (!visionOn) {
            g.fill(0, 0, w, h, 0x26406A8C);       // with no post effect to do it, a cast over the whole of it
        }
        // dark at the edges all round: it sees a narrow way ahead
        int edge = Math.max(30, Math.min(w, h) / 5);
        int dark = 0x90000010, none = 0x00000010;
        g.fillGradient(0, 0, w, edge, dark, none);
        g.fillGradient(0, h - edge, w, h, none, dark);
        for (int i = 0; i < edge; i += 2) {
            int alpha = (int) (0x70 * (1.0F - (float) i / edge));
            g.fill(i, edge, i + 2, h - edge, alpha << 24 | 0x000010);
            g.fill(w - i - 2, edge, w - i, h - edge, alpha << 24 | 0x000010);
        }
    }
}

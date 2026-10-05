package com.xiaofeiwu.grandwitch.client;

import com.xiaofeiwu.grandwitch.GrandWitch;
import com.xiaofeiwu.grandwitch.GrandWitchMod;
import com.xiaofeiwu.grandwitch.Mice;
import com.xiaofeiwu.grandwitch.WitchConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * A mouse knows what is dangerous: while the player is one, the witch, the cats and the monsters within 20 blocks of it glow, through walls, for it alone (the
 * glow is set on this client's copy of them, and not by the server, so no one else sees it).
 */
@Mod.EventBusSubscriber(modid = GrandWitchMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DangerSense {

    private static final double RANGE = 20.0D;
    private static final Set<Integer> LIT = new HashSet<>();
    private static Method setFlag;

    private DangerSense() {
    }

    private static boolean dangerous(Entity e) {
        if (e instanceof GrandWitch witch) {
            return !witch.isMouseForm();
        }
        return e instanceof Cat || (e instanceof Enemy && e.isAlive());
    }

    /** The glowing flag is bit 6 of an entity's flags, and is not open to us to set. */
    private static void glow(Entity e, boolean on) {
        try {
            if (setFlag == null) {
                setFlag = ObfuscationReflectionHelper.findMethod(Entity.class, "m_20115_", int.class, boolean.class);
            }
            setFlag.invoke(e, 6, on);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            // only a glow: not worth the game
        }
    }

    private static void clear(Minecraft mc) {
        if (mc.level != null) {
            for (int id : LIT) {
                Entity e = mc.level.getEntity(id);
                if (e != null && !(e instanceof net.minecraft.world.entity.LivingEntity l && l.hasEffect(MobEffects.GLOWING))) {
                    glow(e, false);
                }
            }
        }
        LIT.clear();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !WitchConfig.dangerSense() || !Mice.isMouse(mc.player)) {
            if (!LIT.isEmpty()) {
                clear(mc);
            }
            return;
        }
        Set<Integer> now = new HashSet<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e != mc.player && dangerous(e) && e.distanceToSqr(mc.player) <= RANGE * RANGE) {
                glow(e, true);                           // every tick: the server's copy of the flags, when it sends them, puts it out
                now.add(e.getId());
            }
        }
        for (int id : LIT) {
            if (!now.contains(id)) {
                Entity e = mc.level.getEntity(id);
                if (e != null && !(e instanceof net.minecraft.world.entity.LivingEntity l && l.hasEffect(MobEffects.GLOWING))) {
                    glow(e, false);
                }
            }
        }
        LIT.clear();
        LIT.addAll(now);
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LIT.clear();
    }
}

package com.xiaofeiwu.grandwitch;

import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * What the witch throws: a splash potion whose one effect, at once, makes a mouse of whoever it lands on: a player, the witch, or any creature at all (but not a boss),
 * by whoever throws it, shoots it on an arrow, or drinks it.
 */
public final class ModEffects {

    static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, GrandWitchMod.MODID);
    static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, GrandWitchMod.MODID);

    public static final RegistryObject<MobEffect> MOUSE_FORM = EFFECTS.register("mouse_form", MouseForm::new);
    public static final RegistryObject<Potion> MOUSE_POTION = POTIONS.register("mouse", () -> new Potion(new MobEffectInstance(MOUSE_FORM.get(), 1)));

    /** The potion, to be drunk. */
    public static ItemStack drinkable() {
        return net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(net.minecraft.world.item.Items.POTION), MOUSE_POTION.get());
    }

    /** The potion, to be thrown. */
    public static ItemStack splash() {
        return net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(net.minecraft.world.item.Items.SPLASH_POTION), MOUSE_POTION.get());
    }

    private ModEffects() {
    }

    static final class MouseForm extends InstantenousMobEffect {

        MouseForm() {
            super(MobEffectCategory.HARMFUL, 0xB89AA8);
        }

        /**
         * An arrow with the potion on it does not go by the instant way (a drink or a splash does): it puts the effect on what it hits, for a tick, and the effect is
         * then ticked. It is not told who shot it.
         */
        @Override
        public void applyEffectTick(LivingEntity target, int amplifier) {
            applyInstantenousEffect(null, null, target, amplifier, 1.0D);
        }

        @Override
        public void applyInstantenousEffect(Entity source, Entity thrower, LivingEntity target, int amplifier, double strength) {
            if (target.level().isClientSide) {
                return;
            }
            if (target instanceof Player player) {
                // a shield held up keeps it off
                // drunk, or thrown at oneself, it is no witch's doing: nothing she did is undone by her death, and a shield does not matter
                boolean own = thrower == null || thrower == player;
                if (!player.isSpectator() && !Mice.isMouse(player) && (own || !player.isBlocking())) {
                    Mice.become(player, own ? null : thrower.getUUID(), WitchConfig.MOUSE_SECONDS.get());
                }
            } else if (target instanceof GrandWitch witch) {
                // thrown at the witch (by anyone but a witch): she is a mouse
                if (!(thrower instanceof GrandWitch)) {
                    witch.becomeMouse(thrower);
                }
            } else if (target instanceof net.minecraft.world.entity.npc.AbstractVillager child && thrower instanceof GrandWitch witch) {
                witch.turnChild(child);                           // hers: she counts it
            } else if (target instanceof net.minecraft.world.entity.Mob mob) {
                turnMob(mob, thrower);
            }
        }

        /** Whatever it is, if it is not a boss and not a mouse already: a mouse that is it still, and is it again when the time is up (or milk is given to it). */
        private static void turnMob(net.minecraft.world.entity.Mob mob, Entity thrower) {
            if (!(mob.level() instanceof net.minecraft.server.level.ServerLevel server) || mob instanceof VillageMouse || mob instanceof GrandWitch
                    || mob.getType().is(net.minecraftforge.common.Tags.EntityTypes.BOSSES) || mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                    || mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss) {
                return;
            }
            mob.ejectPassengers();                                // what is on it, and what it is on, is not made a copy of
            mob.stopRiding();
            VillageMouse mouse = VillageMouse.from(server, mob, thrower instanceof GrandWitch ? thrower.getUUID() : null, WitchConfig.CHILD_MOUSE_SECONDS.get());
            if (mouse == null) {
                return;
            }
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ(), 20, 0.25D, 0.4D, 0.25D, 0.03D);
            mob.discard();
            server.addFreshEntity(mouse);
        }
    }
}

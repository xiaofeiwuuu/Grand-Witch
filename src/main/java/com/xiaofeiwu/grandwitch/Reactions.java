package com.xiaofeiwu.grandwitch;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;
import java.util.List;

/**
 * How the world takes to her, which is the way to see through her: cats hiss and wolves growl at her, an iron golem watches her and goes for her
 * once she has shown herself, and villagers run from her then.
 */
@Mod.EventBusSubscriber(modid = GrandWitchMod.MODID)
public final class Reactions {

    private Reactions() {
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof Cat cat) {
            cat.goalSelector.addGoal(2, new HissGoal(cat, SoundEvents.CAT_HISS));
        } else if (event.getEntity() instanceof Wolf wolf) {
            wolf.goalSelector.addGoal(2, new HissGoal(wolf, SoundEvents.WOLF_GROWL));
        } else if (event.getEntity() instanceof IronGolem golem) {
            golem.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(golem, GrandWitch.class, 10, true, false, witch -> witch instanceof GrandWitch w && !w.isDisguised()));
            golem.goalSelector.addGoal(8, new LookAtPlayerGoal(golem, GrandWitch.class, 8.0F));
        } else if (event.getEntity() instanceof Villager villager) {
            villager.goalSelector.addGoal(1, new FollowNannyGoal(villager));
            villager.goalSelector.addGoal(1, new AvoidEntityGoal<>(villager, GrandWitch.class, 14.0F, 0.6D, 0.8D, witch -> witch instanceof GrandWitch w && !w.isDisguised()));
        }
    }

    /** A child that the nanny has taken by the hand follows her, wherever she goes, as long as she is about and has not been found out. */
    static final class FollowNannyGoal extends Goal {

        private final Villager child;
        private GrandWitch nanny;

        FollowNannyGoal(Villager child) {
            this.child = child;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!child.isBaby() || !child.getPersistentData().hasUUID(GrandWitch.LED) || !(child.level() instanceof net.minecraft.server.level.ServerLevel server)) {
                return false;
            }
            nanny = server.getEntity(child.getPersistentData().getUUID(GrandWitch.LED)) instanceof GrandWitch w && w.isAlive() && w.following().contains(child.getUUID()) ? w : null;
            if (nanny == null) {
                child.getPersistentData().remove(GrandWitch.LED);        // she is gone, or has let go of it
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return nanny != null && nanny.isAlive() && nanny.following().contains(child.getUUID()) && child.getPersistentData().hasUUID(GrandWitch.LED);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (nanny == null) {
                return;
            }
            child.getLookControl().setLookAt(nanny, 30.0F, 30.0F);
            if (child.tickCount % 5 == 0) {
                if (child.distanceToSqr(nanny) > 2.5D * 2.5D) {
                    // the village folk's own way of going to a place, and the plain one beside it, in case what they do of their own overrules it
                    net.minecraft.world.entity.ai.behavior.BehaviorUtils.setWalkAndLookTargetMemories(child, nanny, 0.8F, 2);
                    child.getNavigation().moveTo(nanny, 0.8D);
                } else {
                    child.getNavigation().stop();
                }
            }
        }
    }

    /** It stops and faces her and makes the noise it makes at what it does not trust, for as long as she is near. */
    static final class HissGoal extends Goal {

        private final PathfinderMob mob;
        private final SoundEvent sound;
        private GrandWitch witch;
        private long nextLook;

        HissGoal(PathfinderMob mob, SoundEvent sound) {
            this.mob = mob;
            this.sound = sound;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            long now = mob.level().getGameTime();
            if (now < nextLook || mob.getTarget() != null) {
                return false;
            }
            nextLook = now + 10;
            List<GrandWitch> near = mob.level().getEntitiesOfClass(GrandWitch.class, mob.getBoundingBox().inflate(8.0D), w -> w.isAlive() && !w.isRaging());
            witch = near.isEmpty() ? null : near.get(0);
            return witch != null;
        }

        @Override
        public boolean canContinueToUse() {
            return witch != null && witch.isAlive() && !witch.isRaging() && mob.getTarget() == null && mob.distanceToSqr(witch) < 10.0D * 10.0D;
        }

        @Override
        public void start() {
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            mob.getLookControl().setLookAt(witch, 30.0F, 30.0F);
            if (mob.tickCount % 40 == 0) {
                mob.playSound(sound, 1.0F, 1.0F);
            }
        }

        @Override
        public void stop() {
            witch = null;
        }
    }
}

package com.xiaofeiwu.grandwitch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Sends the Grand Witch: to children (or grown villagers) wherever they are in the loaded world, now and then, one witch at a time. */
@Mod.EventBusSubscriber(modid = GrandWitchMod.MODID)
public final class WitchVisits {

    /** Test hook: the roll, in place of a random one. */
    static Double rollOverride;

    /** How far from the children she comes in, nearest and farthest, when she has no house (a test shortens it to fit in its room). */
    static double[] distance = {24.0D, 34.0D};

    /** How far from the children a place for her house is looked for (a test shortens it). */
    static double[] siteRange = {16.0D, 48.0D};

    private WitchVisits() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (level.getGameTime() % (WitchConfig.CHECK_SECONDS.get() * 20L) != 0) {
            return;
        }
        sweep(level);
    }

    /**
     * The look, every so often: the children of the loaded part of the world, wherever they are and whether or not a player is by them, in groups (those
     * within 48 blocks of each other), each group having its chance of a visit; and, if there are no children, the grown villagers and traders, with a smaller one.
     * At most four groups are tried, and one witch comes.
     * @return the witch that came, or null
     */
    static GrandWitch sweep(ServerLevel level) {
        if (!WitchConfig.ENABLED.get()) {
            return null;
        }
        List<Entity> children = new ArrayList<>(), grown = new ArrayList<>();
        for (AbstractVillager v : level.getEntities(EntityTypeTest.forClass(AbstractVillager.class), x -> x.isAlive())) {
            (v.isBaby() ? children : grown).add(v);
        }
        if (children.isEmpty() && WitchConfig.TURNS_ADULTS.get()) {
            children = grown;                  // children first; the grown ones only when there are none
        }
        for (int tries = 0; tries < 4 && !children.isEmpty(); tries++) {
            Entity seed = children.get(level.random.nextInt(children.size()));
            final Entity s = seed;
            children.removeIf(c -> c.distanceToSqr(s) <= 48.0D * 48.0D);        // the group it is of: not tried again at this look
            if (!playerNear(level, seed.blockPosition())) {
                continue;
            }
            GrandWitch witch = visitAround(level, seed.blockPosition(), false);
            if (witch != null) {
                return witch;
            }
        }
        return null;
    }

    private static boolean playerNear(ServerLevel level, BlockPos pos) {
        int range = WitchConfig.PLAYER_RANGE.get();
        if (range <= 0) {
            return true;
        }
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D) <= (double) range * range) {
                return true;
            }
        }
        return false;
    }

    /** For the command, and for a player: the witch comes to the children round them. @param force ignore the chance and the switch; the one-at-a-time still holds unless {@code force}. */
    static GrandWitch visit(ServerLevel level, Player player, boolean force) {
        if (player.isSpectator() || Mice.isMouse(player)) {
            return null;
        }
        return visitAround(level, player.blockPosition(), force);
    }

    /** She comes to a place: if there are children (or, for a smaller chance, grown villagers) within 48 blocks of it, and no other witch within 160. */
    static GrandWitch visitAround(ServerLevel level, BlockPos center, boolean force) {
        double chance = 1.0D;
        if (!force) {
            if (!WitchConfig.ENABLED.get()) {
                return null;
            }
            AABB box = new AABB(center).inflate(48.0D);
            boolean child = !level.getEntitiesOfClass(Villager.class, box, Villager::isBaby).isEmpty();
            boolean grown = WitchConfig.TURNS_ADULTS.get() && !level.getEntitiesOfClass(AbstractVillager.class, box, v -> !v.isBaby()).isEmpty();
            if (!child && !grown) {
                return null;                 // no one for her to turn, no witch
            }
            chance = child ? WitchConfig.CHANCE.get() : WitchConfig.ADULT_CHANCE.get();
            if (!level.getEntitiesOfClass(GrandWitch.class, new AABB(center).inflate(160.0D)).isEmpty()) {
                return null;
            }
            double roll = rollOverride != null ? rollOverride : level.random.nextDouble();
            if (roll >= chance) {
                return null;
            }
        }
        BlockPos at = null;
        BlockPos hutFloor = null;
        if (WitchConfig.HUT.get()) {
            WitchHut.Hut old = WitchHut.ownerless(level, center, 160);
            if (old != null) {
                at = old.pos().offset(0, 1, 5);                       // a house of hers that stands empty: she comes to it
                hutFloor = old.pos();
            } else if (!WitchHut.hutNear(level, center, 160)) {
                // none stands near: a place for one is looked for, in many places, and it is built where it fits
                for (int site = 0; site < 30 && hutFloor == null; site++) {
                    double angle = level.random.nextDouble() * Math.PI * 2;
                    double dist = siteRange[0] + level.random.nextDouble() * (siteRange[1] - siteRange[0]);
                    BlockPos ground = groundNear(level, (int) Math.floor(center.getX() + 0.5D + Math.cos(angle) * dist), (int) Math.floor(center.getZ() + 0.5D + Math.sin(angle) * dist), center.getY());
                    if (ground != null) {
                        BlockPos door = WitchHut.build(level, ground.below());
                        if (door != null) {
                            at = door;
                            hutFloor = ground.below();
                        }
                    }
                }
            }
        }
        if (at == null) {
            // no house: she comes to a place in the open round the children
            for (int attempt = 0; attempt < 10 && at == null; attempt++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double dist = distance[0] + level.random.nextDouble() * (distance[1] - distance[0]);
                at = groundNear(level, (int) Math.floor(center.getX() + 0.5D + Math.cos(angle) * dist), (int) Math.floor(center.getZ() + 0.5D + Math.sin(angle) * dist), center.getY());
            }
        }
        if (at == null) {
            return null;
        }
        GrandWitch witch = ModEntities.GRAND_WITCH.get().create(level);
        if (witch == null) {
            return null;
        }
        witch.moveTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        witch.dressUp();
        level.addFreshEntity(witch);
        if (hutFloor != null) {
            WitchHut.bind(level, hutFloor, witch);
        }
        return witch;
    }

    static BlockPos groundNear(ServerLevel level, int x, int z, int aroundY) {
        if (!level.hasChunkAt(new BlockPos(x, aroundY, z))) {
            return null;
        }
        for (int offset = 0; offset <= 10; offset++) {
            for (int sign = 1; sign >= -1; sign -= 2) {
                if (offset == 0 && sign == -1) {
                    continue;
                }
                BlockPos pos = new BlockPos(x, aroundY + offset * sign, z);
                if (!level.getBlockState(pos).blocksMotion() && !level.getBlockState(pos.above()).blocksMotion() && level.getFluidState(pos).isEmpty()
                        && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                    return pos;
                }
            }
        }
        return null;
    }
}

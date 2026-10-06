package com.xiaofeiwu.grandwitch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.entity.npc.Villager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * The Grand Witch. She comes to a place where there are child villagers, looking like a villager or a wandering trader, walks up to a player and
 * offers to swap food: what she gives back is ordinary food with a mark on it that cannot be seen, and whoever eats it is a mouse ({@link Mice}).
 * She shows what she is when she is hit, or when there is a mouse about, and then she is in a rage for as long as there is one: she runs it down and
 * stamps on it.
 */
public class GrandWitch extends PathfinderMob {

    private static final EntityDataAccessor<String> DISGUISE = SynchedEntityData.defineId(GrandWitch.class, EntityDataSerializers.STRING);
    /** 0: not dressed as a girl; 1: the girl in the red hood; 2: the girl with the apron and the plaits. Both carry a basket of food. */
    private static final EntityDataAccessor<Integer> GIRL = SynchedEntityData.defineId(GrandWitch.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MOUSE_FORM = SynchedEntityData.defineId(GrandWitch.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PRONE = SynchedEntityData.defineId(GrandWitch.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<net.minecraft.nbt.CompoundTag> ARM = SynchedEntityData.defineId(GrandWitch.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> ARM_TICKS = SynchedEntityData.defineId(GrandWitch.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RAGE = SynchedEntityData.defineId(GrandWitch.class, EntityDataSerializers.BOOLEAN);
    private static final UUID RAGE_SPEED_ID = UUID.fromString("b3a6d0c4-52e1-4f7b-9c18-6d02a4e7f5b3");
    private static final double SEE_MOUSE = 32.0D;

    private EntityType<?> disguiseType;
    private Entity disguiseDummy;
    private int rageTicks;
    private int giveUp;
    private int tradeCooldown;
    private int askCooldown;
    private WitchKind kind = WitchKind.GIFT;
    private final java.util.List<UUID> following = new ArrayList<>();
    private BlockPos nannyDest;
    private int nannyTicks;
    private int nannyCooldown;
    private boolean leading;
    private final java.util.Map<UUID, Long> cursedAlready = new java.util.HashMap<>();

    /** The child that is led away by a nanny carries the uuid of the nanny under this key. */
    static final String LED = "GrandWitchLed";

    /** Test hook: how long, in ticks, the nanny leads before she makes mice of them wherever they are. */
    static int nannyTimeout = 3600;
    private int stompCooldown;
    private int calm;

    /** How long she must have been left alone, in ticks, before she dresses up again (a test shortens it). */
    static int calmTicks = 600;

    /** Test hook: players that are not in the world's list, whom she is to look for a mouse among. */
    final List<Player> extraVictims = new ArrayList<>();

    public GrandWitch(EntityType<? extends GrandWitch> type, Level level) {
        super(type, level);
        this.xpReward = 25;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // a mouse: afraid of players, cats and golems, and of nothing else, and has no tricks
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(this, Player.class, 14.0F, 1.2D, 1.7D) {
            @Override
            public boolean canUse() {
                return isMouseForm() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isMouseForm() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(this, net.minecraft.world.entity.animal.Cat.class, 12.0F, 1.2D, 1.7D) {
            @Override
            public boolean canUse() {
                return isMouseForm() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isMouseForm() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(this, net.minecraft.world.entity.animal.IronGolem.class, 12.0F, 1.2D, 1.7D) {
            @Override
            public boolean canUse() {
                return isMouseForm() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isMouseForm() && super.canContinueToUse();
            }
        });
        // she opens a door that is in her way (as a villager does) and shuts it behind her
        if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
        }
        goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true));
        goalSelector.addGoal(0, new ReachGoal(this));
        goalSelector.addGoal(1, new StompGoal(this));
        goalSelector.addGoal(1, new GuardGoal(this));
        goalSelector.addGoal(2, new NannyGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, true) {
            @Override
            public boolean canUse() {
                return !isMouseForm() && super.canUse();
            }
        });
        goalSelector.addGoal(2, new ZombieGoal(this));
        goalSelector.addGoal(3, new ApproachGoal(this));
        goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 0.9D));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    // ------------------------------------------------------------------ looks ------------------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DISGUISE, "");
        entityData.define(RAGE, false);
        entityData.define(PRONE, false);
        entityData.define(ARM, new net.minecraft.nbt.CompoundTag());
        entityData.define(ARM_TICKS, 0);
        entityData.define(MOUSE_FORM, false);
        entityData.define(GIRL, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DISGUISE.equals(key)) {
            disguiseType = resolve(entityData.get(DISGUISE));
            disguiseDummy = null;
            refreshDimensions();
        } else if (GIRL.equals(key) || MOUSE_FORM.equals(key)) {
            refreshDimensions();
        }
    }

    private static EntityType<?> resolve(String id) {
        ResourceLocation key = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        return key != null && ForgeRegistries.ENTITY_TYPES.containsKey(key) ? ForgeRegistries.ENTITY_TYPES.getValue(key) : null;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (isMouseForm()) {
            return EntityDimensions.scalable(0.4F, 0.4F);
        }
        if (girl() > 0) {
            return EntityDimensions.scalable(0.6F, 1.75F);
        }
        return disguiseType != null ? disguiseType.getDimensions() : super.getDimensions(pose);
    }

    /** Dressed as anything: a villager, a trader, or a girl with a basket. */
    public boolean isDisguised() {
        return disguiseType != null || girl() > 0;
    }

    public int girl() {
        return entityData.get(GIRL);
    }

    /** What she is taken for, for what she says. */
    Component disguiseName() {
        if (girl() > 0) {
            return Component.translatable(girl() == 3 ? "entity.grandwitch.granny" : girl() == 4 ? "entity.grandwitch.nanny" : "entity.grandwitch.girl");
        }
        return disguiseType != null ? disguiseType.getDescription() : getName();
    }

    /** A girl with a basket of food: 1 in a red hood, 2 with an apron and plaits. */
    public void dressAsGirl(int look) {
        if (level().isClientSide) {
            return;
        }
        disguiseType = null;
        disguiseDummy = null;
        entityData.set(DISGUISE, "");
        entityData.set(GIRL, look);
        refreshDimensions();
    }

    public EntityType<?> disguiseType() {
        return disguiseType;
    }

    public boolean isRaging() {
        return entityData.get(RAGE);
    }

    /** The stand-in drawn in her place, on the client. */
    public Entity disguiseDummy() {
        if (disguiseDummy == null && disguiseType != null) {
            disguiseDummy = disguiseType.create(level());
        }
        return disguiseDummy;
    }

    public void disguiseAs(EntityType<?> type) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (key == null || level().isClientSide) {
            return;
        }
        disguiseType = type;
        disguiseDummy = null;
        entityData.set(GIRL, 0);
        entityData.set(DISGUISE, key.toString());
        refreshDimensions();
    }

    /** The children she led are let go. */
    void releaseFollowers() {
        if (level() instanceof ServerLevel server) {
            for (UUID id : following) {
                var kid = server.getEntity(id);
                if (kid != null) {
                    kid.getPersistentData().remove(LED);
                }
            }
        }
        following.clear();
        nannyDest = null;
        nannyTicks = 0;
        leading = false;
    }

    java.util.List<UUID> following() {
        return following;
    }

    /** She shows what she is. */
    public void reveal() {
        if (!isDisguised() || level().isClientSide) {
            return;
        }
        releaseFollowers();
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        disguiseType = null;
        disguiseDummy = null;
        entityData.set(DISGUISE, "");
        entityData.set(GIRL, 0);
        refreshDimensions();
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() / 2, getZ(), 25, 0.3D, 0.5D, 0.3D, 0.03D);
            server.playSound(null, blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1.2F, 0.8F);
            alarm();
        }
    }

    private void updateDummy() {
        Entity d = disguiseDummy();
        if (d == null) {
            return;
        }
        d.xo = d.getX();
        d.yo = d.getY();
        d.zo = d.getZ();
        d.setPos(getX(), getY(), getZ());
        d.setOnGround(onGround());
        d.tickCount = tickCount;
        if (d instanceof LivingEntity living) {
            living.calculateEntityAnimation(false);
        }
    }

    // ------------------------------------------------------------------ the swap ------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isDisguised() || Mice.isMouse(player)) {
            return super.mobInteract(player, hand);
        }
        if (!level().isClientSide && feed(player, hand)) {
            return InteractionResult.CONSUME;
        }
        if (!level().isClientSide) {
            switch (kind) {
                case GIFT -> offer(player);
                case POTION -> sell(player, hand);
                case CURSE -> curseGift(player);
                case NANNY -> player.displayClientMessage(Component.translatable("message.grandwitch.nanny.chat", disguiseName()), false);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    WitchKind kind() {
        return kind;
    }

    /** A potion for emeralds: one that looks like a good one of the game's own (they are all alike to look at, and carry a mark that cannot be seen). */
    boolean sell(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        int price = WitchConfig.POTION_PRICE.get();
        Component me = disguiseName();
        if (!held.is(Items.EMERALD) || held.getCount() < price) {
            if (askCooldown <= 0) {
                askCooldown = 20;                    // she says what it costs, but not at every click
                player.displayClientMessage(Component.translatable("message.grandwitch.potion.ask", me, price), false);
            }
            return false;
        }
        if (tradeCooldown > 0) {
            return false;
        }
        tradeCooldown = 20;
        if (!player.getAbilities().instabuild) {
            held.shrink(price);
        }
        net.minecraft.world.item.alchemy.Potion[] pool = {net.minecraft.world.item.alchemy.Potions.STRONG_REGENERATION, net.minecraft.world.item.alchemy.Potions.STRONG_SWIFTNESS,
                net.minecraft.world.item.alchemy.Potions.STRONG_STRENGTH, net.minecraft.world.item.alchemy.Potions.LONG_NIGHT_VISION, net.minecraft.world.item.alchemy.Potions.LONG_FIRE_RESISTANCE,
                net.minecraft.world.item.alchemy.Potions.STRONG_HEALING};
        ItemStack potion = net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), pool[random.nextInt(pool.length)]);
        potion.getOrCreateTag().putBoolean(Mice.FOOD, true);
        potion.getOrCreateTag().putUUID("Witch", getUUID());
        if (!player.getInventory().add(potion)) {
            player.drop(potion, false);
        }
        player.displayClientMessage(Component.translatable("message.grandwitch.potion.sold", me), false);
        level().playSound(null, blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    /** A good thing, with a curse on it, to one player once in ten minutes. */
    boolean curseGift(Player player) {
        long now = level().getGameTime();
        Long last = cursedAlready.get(player.getUUID());
        Component me = disguiseName();
        if (last != null && now - last < 12000L) {
            if (tradeCooldown <= 0) {
                tradeCooldown = 20;
                player.displayClientMessage(Component.translatable("message.grandwitch.curse.enough", me), false);
            }
            return false;
        }
        cursedAlready.put(player.getUUID(), now);
        ItemStack gift = switch (random.nextInt(5)) {
            case 0 -> {
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 2);
                yield sword;
            }
            case 1 -> {
                ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
                pick.enchant(net.minecraft.world.item.enchantment.Enchantments.BLOCK_EFFICIENCY, 2);
                pick.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 2);
                yield pick;
            }
            case 2 -> new ItemStack(Items.GOLDEN_APPLE, 3);
            case 3 -> new ItemStack(Items.ENDER_PEARL, 4);
            default -> new ItemStack(Items.DIAMOND, 3);
        };
        Curses.curse(gift, getUUID());
        if (!player.getInventory().add(gift)) {
            player.drop(gift, false);
        }
        player.displayClientMessage(Component.translatable("message.grandwitch.curse.gift", me), false);
        level().playSound(null, blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    /** Who she has given something to, and when: one gift to a player in five minutes. */
    private final java.util.Map<UUID, Long> given = new java.util.HashMap<>();
    private final java.util.Set<UUID> called = new java.util.HashSet<>();

    /** She gives the player something to eat: ordinary food, with a mark on it that cannot be seen. */
    boolean offer(Player player) {
        Component me = disguiseName();
        long now = level().getGameTime();
        Long last = given.get(player.getUUID());
        if (last != null && now - last < 6000L) {
            if (tradeCooldown <= 0) {
                tradeCooldown = 20;                  // she does not say it at every click
                player.displayClientMessage(Component.translatable("message.grandwitch.enough", me), false);
            }
            return false;
        }
        given.put(player.getUUID(), now);
        ItemStack gift = Mice.poisoned(random, getUUID());
        if (!player.getInventory().add(gift)) {
            player.drop(gift, false);
        }
        player.displayClientMessage(Component.translatable("message.grandwitch.offer", me), false);
        level().playSound(null, blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    /** As a player comes near she calls to them, once, so that they know there is something to be had from her. */
    private void callNearby() {
        if (!isDisguised()) {
            return;
        }
        if (kind == WitchKind.NANNY) {
            return;                          // she does not call anyone: she has the children to see to
        }
        for (Player p : level().players()) {
            if (!Mice.isMouse(p) && !p.isSpectator() && distanceToSqr(p) < 4.5D * 4.5D && called.add(p.getUUID())) {
                p.displayClientMessage(switch (kind) {
                    case POTION -> Component.translatable("message.grandwitch.call.potion", disguiseName(), WitchConfig.POTION_PRICE.get());
                    case CURSE -> Component.translatable("message.grandwitch.call.curse", disguiseName());
                    default -> Component.translatable("message.grandwitch.call", disguiseName());
                }, false);
            }
        }
    }

    // ------------------------------------------------------------------ rage ------------------------------------------------------------------

    static boolean isMouseLike(LivingEntity e) {
        return e instanceof VillageMouse || Mice.isMouse(e);
    }

    /** The nearest mouse, a player or a child, within her sight. */
    /** Mice in her cage, by her house, are hers: they are not what sets her raging. */
    boolean exempt(LivingEntity e) {
        return e instanceof VillageMouse m && m.isCaged() && home != null && m.blockPosition().distSqr(home) < 36.0D;
    }

    /** A mouse put in the cage in her house, on the west side, in one of its three places. */
    void cageMouse(VillageMouse mouse) {
        if (home == null) {
            return;
        }
        mouse.moveTo(home.getX() - 2 + 0.5D, home.getY() + 1.0D, home.getZ() - random.nextInt(2) + 0.5D, random.nextFloat() * 360.0F, 0.0F);
        mouse.setCaged(true);
        mouse.getNavigation().stop();
    }

    LivingEntity findVictim() {
        List<LivingEntity> all = new ArrayList<>(level().players());
        all.addAll(extraVictims);
        all.addAll(level().getEntitiesOfClass(VillageMouse.class, getBoundingBox().inflate(SEE_MOUSE)));
        LivingEntity best = null;
        for (LivingEntity p : all) {
            if (isMouseLike(p) && !exempt(p) && !MouseHoleBlock.hidden(p) && !p.isSpectator() && p.isAlive() && distanceToSqr(p) <= SEE_MOUSE * SEE_MOUSE && (best == null || distanceToSqr(p) < distanceToSqr(best))) {
                best = p;
            }
        }
        return best;
    }

    private BlockPos home;
    private int alertTicks;
    private Player alertWho;

    /** Her house, with {@code floor} the middle of its floor: she keeps within 40 blocks of it when there is nothing to do. */
    void setHome(BlockPos floor) {
        home = floor;
        restrictTo(floor, 40);
    }

    BlockPos home() {
        return home;
    }

    int alertTicks() {
        return alertTicks;
    }

    /**
     * Someone is in her house. She shows what she is and goes for them for thirty seconds, in a rage; and if she is far off she is at the door at once.
     * @return whether she took it up (she does not, if she is on it already)
     */
    boolean alert(BlockPos floor, Player intruder) {
        if (!(level() instanceof ServerLevel server) || !WitchConfig.HUT_ALARM.get() || alertTicks > 0) {
            return false;
        }
        alertTicks = 600;
        alertWho = intruder;
        reveal();
        rageTicks = Math.max(rageTicks, 600);
        setRage(true);
        setTarget(intruder);
        if (WitchConfig.BLINK.get() && distanceToSqr(floor.getX() + 0.5D, floor.getY(), floor.getZ() + 0.5D) > 40.0D * 40.0D) {
            blinkTo(floor.offset(0, 1, 5));
        }
        server.playSound(null, floor, SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 3.0F, 0.6F);
        intruder.displayClientMessage(Component.translatable("message.grandwitch.trespass"), true);
        return true;
    }

    /** She is there, in a puff of smoke at both ends. */
    void blinkTo(BlockPos spot) {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(), 25, 0.3D, 0.5D, 0.3D, 0.03D);
            moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, getYRot(), 0.0F);
            getNavigation().stop();
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(), 25, 0.3D, 0.5D, 0.3D, 0.03D);
        }
    }

    private int turnCooldown;
    private int zombieCooldown;
    private int turned;
    private int adultsTurned;
    private int potionCooldown = 100;
    private int thrown;
    private long lastAlarm;

    String runningGoalNames() {
        return goalSelector.getRunningGoals().map(w -> w.getGoal().getClass().getSimpleName()).toList().toString();
    }

    int thrown() {
        return thrown;
    }

    /** The nearest player in her sight who is not a mouse yet, 4 to 16 blocks off. */
    Player throwTarget() {
        List<Player> all = new ArrayList<>(level().players());
        all.addAll(extraVictims);
        Player best = null;
        for (Player p : all) {
            double d = distanceToSqr(p);
            if (!Mice.isMouse(p) && !p.isSpectator() && p.isAlive() && d >= 16.0D && d <= 256.0D && hasLineOfSight(p) && (best == null || d < distanceToSqr(best))) {
                best = p;
            }
        }
        return best;
    }

    /** A splash potion at the player, aimed as the game's witch aims, a little ahead of where they are going. */
    void throwPotion(LivingEntity target) {
        net.minecraft.world.phys.Vec3 v = target.getDeltaMovement();
        double dx = target.getX() + v.x - getX();
        double dy = target.getEyeY() - 1.1D - getY();
        double dz = target.getZ() + v.z - getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        net.minecraft.world.entity.projectile.ThrownPotion potion = new net.minecraft.world.entity.projectile.ThrownPotion(level(), this);
        potion.setItem(net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(net.minecraft.world.item.Items.SPLASH_POTION), ModEffects.MOUSE_POTION.get()));
        potion.setXRot(potion.getXRot() - 20.0F);
        potion.shoot(dx, dy + flat * 0.2D, dz, 0.75F, 8.0F);
        level().addFreshEntity(potion);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 1.0F, 0.8F + random.nextFloat() * 0.4F);
        swing(InteractionHand.MAIN_HAND);
        thrown++;
    }

    /** Every bell within 48 blocks rings (four at the most), she shines for ten seconds, and the players near are told. At most every twenty seconds. */
    void alarm() {
        if (!(level() instanceof ServerLevel server) || !WitchConfig.ALARM.get()) {
            return;
        }
        long now = server.getGameTime();
        if (lastAlarm != 0L && now - lastAlarm < 400L) {
            return;
        }
        int rung = 0;
        net.minecraft.world.level.ChunkPos here = new net.minecraft.world.level.ChunkPos(blockPosition());
        for (int cx = here.x - 3; cx <= here.x + 3 && rung < 4; cx++) {
            for (int cz = here.z - 3; cz <= here.z + 3 && rung < 4; cz++) {
                net.minecraft.world.level.chunk.LevelChunk chunk = server.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (net.minecraft.world.level.block.entity.BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
                    if (rung < 4 && be instanceof net.minecraft.world.level.block.entity.BellBlockEntity && be.getBlockPos().distSqr(blockPosition()) <= 48.0D * 48.0D
                            && server.getBlockState(be.getBlockPos()).getBlock() instanceof net.minecraft.world.level.block.BellBlock bell && bell.attemptToRing(server, be.getBlockPos(), null)) {
                        rung++;
                    }
                }
            }
        }
        if (rung > 0) {
            lastAlarm = now;
            addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 200, 0, false, false));
            for (ServerPlayer p : server.players()) {
                if (p.distanceToSqr(this) <= 64.0D * 64.0D) {
                    p.displayClientMessage(Component.translatable("message.grandwitch.alarm"), true);
                }
            }
        }
    }

    // ------------------------------------------------------------------ the witch as a mouse ------------------------------------------------------------------

    private int mouseTicks;
    private float healthBefore = 60.0F;
    private static final UUID MOUSE_SPEED_ID = UUID.fromString("c2f9a3d1-4b7e-4a55-91ce-0d6b8e7f2a64");

    /** Test hook: the roll made when she is offered what is spiked, in place of a random one. */
    static Double suspicionOverride;

    // ------------------------------------------------------------------ the broom ------------------------------------------------------------------

    private net.minecraft.world.phys.Vec3 flyTarget;
    private int flyTicks;

    /** Whether she is bent over, down at the mouth of a tunnel, with her arm in it or about to be. */
    public boolean isProne() {
        return entityData.get(PRONE);
    }

    /** The blocks of tunnel her arm goes along, from the mouth in. */
    public long[] armPath() {
        return entityData.get(ARM).getLongArray("p");
    }

    /** How far out her arm is, in ticks (it takes {@link #ARM_FULL} to be all the way). */
    public int armTicks() {
        return entityData.get(ARM_TICKS);
    }

    public static final int ARM_FULL = 12;

    /** Test hook: the chance she gets hold of one that is at the sixth or seventh block, when it is not negative. */
    static float gropeLuck = -1.0F;

    /** What the reaching did, tick by tick (the last few things): for a test to say where it stopped. */
    final StringBuilder reachTrail = new StringBuilder();

    void trail(String what) {
        if (reachTrail.length() > 600) {
            reachTrail.delete(0, 300);
        }
        reachTrail.append(tickCount).append(':').append(what).append(' ');
    }

    /** Why she was last not going to reach into a hole (for a test to say). */
    String reachWhy = "not asked yet";

    /** One in this many times she is asked, she tries for a mouse that is six or seven blocks in; a test lowers it. */
    static int gropeOneIn = 40;

    private LivingEntity lastVictim;
    private int lastSeen;
    private int reachCooldown;

    /** Test hook, and what she does when she has lost sight of one: she is after this one. */
    void hunt(LivingEntity mouse) {
        lastVictim = mouse;
        lastSeen = tickCount;
        reveal();
    }

    /** Whether she is on her broom, low over the ground. */
    public boolean onBroom() {
        return getVehicle() instanceof BroomEntity;
    }

    net.minecraft.world.phys.Vec3 flyTarget() {
        return flyTarget;
    }

    /**
     * Going after someone: by a broom if she has shown what she is and they are more than 6 blocks off (it is kept up for eight ticks after each time this is
     * asked, so that it is asked again and again while she goes), and else on foot.
     * @return whether it is by broom
     */
    boolean chase(LivingEntity target, double onFoot) {
        if (WitchConfig.BROOM.get() && !isDisguised() && !isMouseForm() && distanceToSqr(target) > 36.0D) {
            flyTarget = target.position();
            flyTicks = 8;
            return true;
        }
        getNavigation().moveTo(target, onFoot);
        return false;
    }

    /** She calls up a broom and sits on it, or gets off it (and it is gone) when she has no one far off to go after. */
    private void tickBroom() {
        if (flyTicks > 0 && flyTarget != null && !isDisguised() && !isMouseForm()) {
            flyTicks--;
            if (!onBroom() && level() instanceof ServerLevel server) {
                BroomEntity broom = ModEntities.BROOM.get().create(server);
                if (broom != null) {
                    broom.setItem(new ItemStack(ModItems.WOODEN_BROOM.get()));
                    broom.markSummoned();
                    broom.moveTo(getX(), getY() + 0.1D, getZ(), getYRot(), 0.0F);
                    server.addFreshEntity(broom);
                    getNavigation().stop();
                    startRiding(broom, true);
                }
            }
        } else if (getVehicle() instanceof BroomEntity broom) {
            stopRiding();
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.POOF, broom.getX(), broom.getY() + 0.3D, broom.getZ(), 10, 0.3D, 0.1D, 0.3D, 0.02D);
            }
            broom.discard();
            flyTicks = 0;
        }
    }

    public boolean isMouseForm() {
        return entityData.get(MOUSE_FORM);
    }

    /** Test hook: her time as a mouse. */
    void mouseTicksForTest(int ticks) {
        mouseTicks = ticks;
    }

    int mouseTicksLeft() {
        return mouseTicks;
    }

    private static boolean spiked(ItemStack stack) {
        return SpikeRecipe.spiked(stack);
    }

    /**
     * She is a mouse: small and quick, with ten health (and the health she had is kept for when she is herself), afraid, with none of her tricks, and not dressed
     * as anyone. The bells ring, and she shines, for she is easy to lose.
     */
    void becomeMouse(Entity by) {
        if (level().isClientSide || isMouseForm()) {
            return;
        }
        reveal();
        flyTicks = 0;
        tickBroom();                                          // off her broom, if she was on it
        setRage(false);
        rageTicks = 0;
        alertTicks = 0;
        alertWho = null;
        setTarget(null);
        healthBefore = getHealth();
        entityData.set(MOUSE_FORM, true);
        mouseTicks = WitchConfig.WITCH_MOUSE_SECONDS.get() * 20;
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        maxHealth.setBaseValue(10.0D);
        setHealth(10.0F);
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(MOUSE_SPEED_ID) == null) {
            speed.addTransientModifier(new AttributeModifier(MOUSE_SPEED_ID, "witch mouse", 0.6D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        getNavigation().stop();
        refreshDimensions();
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(), 30, 0.3D, 0.6D, 0.3D, 0.04D);
            server.playSound(null, blockPosition(), SoundEvents.WITCH_DRINK, SoundSource.HOSTILE, 1.2F, 1.5F);
            for (ServerPlayer p : server.players()) {
                if (p.distanceToSqr(this) <= 48.0D * 48.0D) {
                    p.displayClientMessage(Component.translatable("message.grandwitch.witch_mouse"), false);
                }
            }
        }
        alarm();
    }

    /** She is herself again, with the health she had before. */
    void revertFromMouse() {
        if (level().isClientSide || !isMouseForm()) {
            return;
        }
        entityData.set(MOUSE_FORM, false);
        mouseTicks = 0;
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(MOUSE_SPEED_ID) != null) {
            speed.removeModifier(MOUSE_SPEED_ID);
        }
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(60.0D);
        setHealth(Math.max(1.0F, healthBefore));
        refreshDimensions();
        calm = 0;
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(), 30, 0.3D, 0.6D, 0.3D, 0.04D);
            for (ServerPlayer p : server.players()) {
                if (p.distanceToSqr(this) <= 48.0D * 48.0D) {
                    p.displayClientMessage(Component.translatable("message.grandwitch.witch_back"), false);
                }
            }
        }
    }

    /**
     * Offered what is spiked while she is dressed up, she smells it and shows what she is (30 times in 100), or she eats it, with thanks, and is a mouse.
     * @return whether it was taken up (it is not, if she is already revealed, or it is not spiked)
     */
    boolean feed(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!isDisguised() || isMouseForm() || !spiked(held)) {
            return false;
        }
        Component me = disguiseName();
        double roll = suspicionOverride != null ? suspicionOverride : random.nextDouble();
        if (roll < WitchConfig.SUSPICION.get()) {
            player.displayClientMessage(Component.translatable("message.grandwitch.feed.suspect", me), false);
            reveal();
            setTarget(player);
            return true;                                      // spat out: the player keeps it
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        player.displayClientMessage(Component.translatable("message.grandwitch.feed.eat", me), false);
        level().playSound(null, blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        becomeMouse(player);
        return true;
    }

    int turned() {
        return turned;
    }

    /** A child villager is turned into a mouse: a puff of smoke, and in its place a mouse that is the child still. */
    VillageMouse turnChild(net.minecraft.world.entity.npc.AbstractVillager child) {
        if (!(level() instanceof ServerLevel server)) {
            return null;
        }
        if (!child.isBaby() && (!WitchConfig.TURNS_ADULTS.get() || adultsTurned >= WitchConfig.ADULTS_PER_VISIT.get())) {
            return null;
        }
        VillageMouse mouse = VillageMouse.from(server, child, getUUID(), WitchConfig.CHILD_MOUSE_SECONDS.get());
        if (mouse == null) {
            return null;
        }
        server.sendParticles(ParticleTypes.POOF, child.getX(), child.getY() + 0.6D, child.getZ(), 20, 0.25D, 0.4D, 0.25D, 0.03D);
        server.playSound(null, child.blockPosition(), SoundEvents.WITCH_DRINK, SoundSource.HOSTILE, 1.0F, 1.2F);
        if (!child.isBaby()) {
            adultsTurned++;
        }
        child.discard();
        server.addFreshEntity(mouse);
        turned++;
        turnCooldown = 1200;
        for (ServerPlayer p : server.players()) {
            if (p.distanceToSqr(this) <= 40.0D * 40.0D) {
                p.displayClientMessage(Component.translatable("message.grandwitch.child_turned"), true);
            }
        }
        return mouse;
    }

    /** A baby zombie is turned into a mouse, for good (it is only the zombie again if she dies or milk is given to it). */
    boolean turnZombie(net.minecraft.world.entity.monster.Zombie zombie) {
        if (!(level() instanceof ServerLevel server)) {
            return false;
        }
        VillageMouse mouse = VillageMouse.from(server, zombie, getUUID(), 0);
        if (mouse == null) {
            return false;
        }
        server.sendParticles(ParticleTypes.POOF, zombie.getX(), zombie.getY() + 0.5D, zombie.getZ(), 20, 0.2D, 0.3D, 0.2D, 0.03D);
        server.playSound(null, zombie.blockPosition(), SoundEvents.WITCH_DRINK, SoundSource.HOSTILE, 1.0F, 1.4F);
        zombie.discard();
        server.addFreshEntity(mouse);
        zombieCooldown = 100;
        return true;
    }

    int rageTicks() {
        return rageTicks;
    }

    private void setRage(boolean on) {
        if (isRaging() == on) {
            return;
        }
        entityData.set(RAGE, on);
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (on && speed.getModifier(RAGE_SPEED_ID) == null) {
                speed.addTransientModifier(new AttributeModifier(RAGE_SPEED_ID, "rage", 0.4D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            } else if (!on && speed.getModifier(RAGE_SPEED_ID) != null) {
                speed.removeModifier(RAGE_SPEED_ID);
            }
        }
    }

    /** Her foot comes down on a mouse. */
    void stomp(LivingEntity victim) {
        swing(InteractionHand.MAIN_HAND);
        if (victim.hurt(damageSources().mobAttack(this), WitchConfig.STOMP_DAMAGE.get().floatValue()) && victim instanceof Player player) {
            Mice.flatten(player);
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, victim.getX(), victim.getY() + 0.05D, victim.getZ(), 8, 0.2D, 0.02D, 0.2D, 0.02D);
            server.playSound(null, victim.blockPosition(), SoundEvents.GENERIC_BIG_FALL, SoundSource.HOSTILE, 1.0F, 0.7F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            updateDummy();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isMouseForm()) {
            if (--mouseTicks <= 0) {
                revertFromMouse();
            }
            return;                                           // she has none of her tricks, and does not go away while she is one
        }
        tickBroom();
        if (tradeCooldown > 0) {
            tradeCooldown--;
        }
        if (stompCooldown > 0) {
            stompCooldown--;
        }
        if (zombieCooldown > 0) {
            zombieCooldown--;
        }
        if (reachCooldown > 0) {
            reachCooldown--;
        }
        if (turnCooldown > 0) {
            turnCooldown--;
        }
        if (nannyCooldown > 0) {
            nannyCooldown--;
        }
        if (askCooldown > 0) {
            askCooldown--;
        }
        if (alertTicks > 0 && --alertTicks == 0) {
            if (alertWho != null && getTarget() == alertWho) {
                setTarget(null);
            }
            alertWho = null;
        }
        if (potionCooldown > 0) {
            potionCooldown--;
        } else if (tickCount % 10 == 0 && !isDisguised() && WitchConfig.POTIONS.get()) {
            Player target = throwTarget();
            if (target != null) {
                throwPotion(target);
                potionCooldown = WitchConfig.POTION_SECONDS.get() * 20 + random.nextInt(40);
            }
        }
        if (tickCount % 20 == 0) {
            callNearby();
        }
        if (tickCount % 5 == 0) {
            LivingEntity seen = findVictim();
            if (seen != null) {
                lastVictim = seen;
                lastSeen = tickCount;
                rageTicks = 100;
                reveal();
                setRage(true);
            } else if (rageTicks > 0) {
                rageTicks -= 5;
                if (rageTicks <= 0 && alertWho != null && Mice.isMouse(alertWho)) {
                    giveUp = 600;                                 // the mouse got away: half a minute more, and she goes
                }
            } else {
                setRage(false);
            }
            if (giveUp > 0 && (giveUp -= 5) <= 0 && !isRaging() && level() instanceof ServerLevel gone) {
                gone.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(), 25, 0.3D, 0.5D, 0.3D, 0.03D);
                discard();
                return;
            }
        }
        // left alone for a while, she dresses up again: she is no use to herself as a witch that nobody is afraid of
        if (!isDisguised() && !isRaging() && getTarget() == null && rageTicks <= 0) {
            if (++calm >= calmTicks) {
                calm = 0;
                dressUp();
            }
        } else {
            calm = 0;
        }
        // she does not stay for ever when nothing comes of it
        if (tickCount > WitchConfig.STAY_SECONDS.get() * 20 && !isRaging() && level() instanceof ServerLevel server && getTarget() == null) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(), 25, 0.3D, 0.5D, 0.3D, 0.03D);
            discard();
        }
    }

    /** A villager or a wandering trader, as she was when she came. */
    void dressUp() {
        dressUp(pickKind());
    }

    /** Which of the four she is: by the shares in the setting; the nanny only where there are children. */
    WitchKind pickKind() {
        int gift = WitchConfig.W_GIFT.get(), potion = WitchConfig.W_POTION.get(), curse = WitchConfig.W_CURSE.get();
        int nanny = level().getEntitiesOfClass(Villager.class, getBoundingBox().inflate(48.0D), Villager::isBaby).isEmpty() ? 0 : WitchConfig.W_NANNY.get();
        int total = gift + potion + curse + nanny;
        if (total <= 0) {
            return WitchKind.GIFT;
        }
        int roll = random.nextInt(total);
        return roll < gift ? WitchKind.GIFT : roll < gift + potion ? WitchKind.POTION : roll < gift + potion + curse ? WitchKind.CURSE : WitchKind.NANNY;
    }

    void dressUp(WitchKind as) {
        kind = as;
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        switch (as) {
            case GIFT -> {
                int pick = random.nextInt(8);          // most often the girl with a basket, which is what she would be in the story
                if (pick < 3) {
                    dressAsGirl(1);
                } else if (pick < 6) {
                    dressAsGirl(2);
                } else {
                    disguiseAs(EntityType.VILLAGER);
                }
            }
            case POTION -> {
                dressAsGirl(3);
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));        // a stick to lean on
            }
            case CURSE -> disguiseAs(EntityType.WANDERING_TRADER);
            case NANNY -> dressAsGirl(4);
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() / 2, getZ(), 25, 0.3D, 0.5D, 0.3D, 0.03D);
        }
    }

    /** One that comes from an egg or a command is dressed up too: a witch that is already showing what she is has nothing to offer. */
    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType type,
                                                                  net.minecraft.world.entity.SpawnGroupData data, CompoundTag tag) {
        if (!isDisguised()) {
            dressUp();
        }
        return super.finalizeSpawn(level, difficulty, type, data, tag);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        calm = 0;
        reveal();
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel server) {
            // what she did is undone
            for (ServerPlayer p : server.players()) {
                if (getUUID().equals(Mice.madeBy(p))) {
                    Mice.cure(p, true);
                }
            }
            for (Player p : extraVictims) {
                if (getUUID().equals(Mice.madeBy(p))) {
                    Mice.cure(p, true);
                }
            }
            for (Entity e : iterable(server)) {
                if (e instanceof VillageMouse m && getUUID().equals(m.maker())) {
                    m.revert();
                }
            }
        }
        super.die(source);
    }

    // ------------------------------------------------------------------ the rest ------------------------------------------------------------------

    /** What is left of her: a potion of the mouse, to drink; and now and then a second, to throw. */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (random.nextDouble() < WitchConfig.VIRUS_WITCH_CHANCE.get()) {
            spawnAtLocation(new ItemStack(ModItems.PLAGUE_VIRUS.get()));
        }
        spawnAtLocation(ModEffects.drinkable());
        if (random.nextInt(4) == 0) {
            spawnAtLocation(ModEffects.splash());
        }
        int broom = random.nextInt(10);                     // a broom of hers, now and then: a wooden one in two, a golden one in ten
        if (broom < 5) {
            spawnAtLocation(new ItemStack(ModItems.WOODEN_BROOM.get()));
        } else if (broom == 9) {
            spawnAtLocation(new ItemStack(ModItems.GOLDEN_BROOM.get()));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isDisguised() ? SoundEvents.VILLAGER_AMBIENT : SoundEvents.WITCH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITCH_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Disguise", entityData.get(DISGUISE));
        tag.putInt("Girl", girl());
        tag.putInt("Kind", kind.ordinal());
        tag.putBoolean("MouseForm", isMouseForm());
        tag.putInt("MouseTicks", mouseTicks);
        tag.putFloat("HealthBefore", healthBefore);
        if (home != null) {
            tag.putLong("Home", home.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Home")) {
            setHome(BlockPos.of(tag.getLong("Home")));
        }
        kind = WitchKind.values()[Math.max(0, Math.min(WitchKind.values().length - 1, tag.getInt("Kind")))];
        if (tag.getBoolean("MouseForm")) {
            healthBefore = tag.getFloat("HealthBefore");
            entityData.set(MOUSE_FORM, true);
            mouseTicks = Math.max(1, tag.getInt("MouseTicks"));
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(10.0D);
            getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(new AttributeModifier(MOUSE_SPEED_ID, "witch mouse", 0.6D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        EntityType<?> type = resolve(tag.getString("Disguise"));
        if (type != null) {
            disguiseAs(type);
        } else if (tag.getInt("Girl") > 0) {
            dressAsGirl(tag.getInt("Girl"));
            if (tag.getInt("Girl") == 3) {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
            }
        }
    }

    /** One that is drawn in the guide or by a test: nothing but its looks. */
    public static GrandWitch plain(Level level) {
        return new GrandWitch(ModEntities.GRAND_WITCH.get(), level);
    }

    // ------------------------------------------------------------------ goals ------------------------------------------------------------------

    /** She goes for the mouse and stamps on it, and does not give up while it can be seen. */
    private static final class StompGoal extends Goal {

        private final GrandWitch witch;
        private LivingEntity victim;

        StompGoal(GrandWitch witch) {
            this.witch = witch;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            victim = witch.isMouseForm() ? null : witch.findVictim();
            return victim != null;
        }

        @Override
        public boolean canContinueToUse() {
            return victim != null && victim.isAlive() && isMouseLike(victim) && !witch.exempt(victim) && !MouseHoleBlock.hidden(victim) && witch.distanceToSqr(victim) <= 40.0D * 40.0D;
        }

        @Override
        public void stop() {
            victim = null;
            witch.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (victim == null) {
                return;                                        // on every other tick a goal is ticked without being asked whether it goes on: what it was after may be gone
            }
            witch.getLookControl().setLookAt(victim, 30.0F, 30.0F);
            if (witch.tickCount % 4 == 0 || witch.getNavigation().isDone()) {
                witch.chase(victim, 1.0D);
            }
            double dx = victim.getX() - witch.getX(), dz = victim.getZ() - witch.getZ();
            if (dx * dx + dz * dz < 1.5D * 1.5D && Math.abs(victim.getY() - witch.getY()) < 1.8D && witch.stompCooldown <= 0) {
                witch.stompCooldown = 14;
                witch.stomp(victim);
            }
        }
    }

    private static List<Entity> iterable(ServerLevel server) {
        List<Entity> all = new ArrayList<>();
        server.getAllEntities().forEach(all::add);
        return all;
    }

    /** She goes for whoever is in her house, and strikes when she is on them. */
    private static final class GuardGoal extends Goal {

        private final GrandWitch witch;
        private int cooldown;
        private int hiddenTicks;

        GuardGoal(GrandWitch witch) {
            this.witch = witch;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !witch.isMouseForm() && witch.alertTicks > 0 && witch.alertWho != null && witch.alertWho.isAlive() && !witch.alertWho.isSpectator();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Player who = witch.alertWho;
            if (who == null) {
                return;                                        // the alert has run out (this is ticked on every other tick without being asked whether it goes on)
            }
            // someone in a tunnel she cannot reach her arm into: she does not stand over them for the rest of the thirty seconds, but gives up after five
            hiddenTicks = MouseHoleBlock.hidden(who) ? hiddenTicks + 1 : 0;
            if (hiddenTicks > 100) {
                hiddenTicks = 0;
                witch.alertTicks = 0;
                witch.alertWho = null;
                witch.setTarget(null);
                return;
            }
            witch.getLookControl().setLookAt(who, 30.0F, 30.0F);
            if (witch.tickCount % 4 == 0 || witch.getNavigation().isDone()) {
                witch.chase(who, 1.0D);
            }
            if (cooldown > 0) {
                cooldown--;
            }
            double dx = who.getX() - witch.getX(), dz = who.getZ() - witch.getZ();
            if (cooldown <= 0 && dx * dx + dz * dz < 1.7D * 1.7D && Math.abs(who.getY() - witch.getY()) < 2.0D) {
                cooldown = 20;
                witch.swing(InteractionHand.MAIN_HAND);
                witch.doHurtTarget(who);
            }
        }

        @Override
        public void stop() {
            witch.getNavigation().stop();
        }
    }

    /**
     * The nanny: finds the children of the village within 24 blocks and has them follow her, up to the number set, and then leads them, slowly enough for them, to her
     * house (or, with none, a long way off), where they are made mice. She is found out, and lets go of them, as soon as she is hit.
     */
    private static final class NannyGoal extends Goal {

        private final GrandWitch witch;

        NannyGoal(GrandWitch witch) {
            this.witch = witch;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private Villager nearestFree() {
            Villager best = null;
            for (Villager v : witch.level().getEntitiesOfClass(Villager.class, witch.getBoundingBox().inflate(24.0D), x -> x.isBaby() && x.isAlive() && !x.getPersistentData().hasUUID(LED))) {
                if (best == null || witch.distanceToSqr(v) < witch.distanceToSqr(best)) {
                    best = v;
                }
            }
            return best;
        }

        @Override
        public boolean canUse() {
            return !witch.isMouseForm() && witch.kind == WitchKind.NANNY && witch.isDisguised() && !witch.isRaging() && witch.level() instanceof ServerLevel
                    && (!witch.following.isEmpty() || (witch.nannyCooldown <= 0 && nearestFree() != null));
        }

        @Override
        public boolean canContinueToUse() {
            return witch.kind == WitchKind.NANNY && witch.isDisguised() && !witch.isRaging() && (!witch.following.isEmpty() || witch.nannyTicks < 400);
        }

        @Override
        public void start() {
            witch.nannyTicks = 0;
            witch.leading = false;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private BlockPos destination(ServerLevel server) {
            if (witch.home != null) {
                return witch.home.offset(0, 1, 5);
            }
            for (int tries = 0; tries < 10; tries++) {
                double angle = witch.random.nextDouble() * Math.PI * 2;
                BlockPos far = WitchVisits.groundNear(server, (int) Math.floor(witch.getX() + Math.cos(angle) * 50.0D), (int) Math.floor(witch.getZ() + Math.sin(angle) * 50.0D), witch.blockPosition().getY());
                if (far != null) {
                    return far;
                }
            }
            return witch.blockPosition();
        }

        @Override
        public void tick() {
            if (!(witch.level() instanceof ServerLevel server)) {
                return;
            }
            witch.nannyTicks++;
            // those that are lost, or gone, are let go
            if (witch.tickCount % 20 == 0) {
                witch.following.removeIf(id -> {
                    var kid = server.getEntity(id);
                    boolean lost = kid == null || !kid.isAlive() || kid.distanceToSqr(witch) > 40.0D * 40.0D;
                    if (lost && kid != null) {
                        kid.getPersistentData().remove(LED);
                    }
                    return lost;
                });
            }
            if (!witch.leading) {
                Villager next = witch.following.size() < WitchConfig.NANNY_MAX.get() ? nearestFree() : null;
                if (next != null && witch.nannyTicks < 400) {
                    witch.getLookControl().setLookAt(next, 30.0F, 30.0F);
                    if (witch.distanceToSqr(next) <= 2.6D * 2.6D) {
                        next.getPersistentData().putUUID(LED, witch.getUUID());
                        witch.following.add(next.getUUID());
                        witch.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.2F);
                        for (ServerPlayer p : server.players()) {
                            if (p.distanceToSqr(witch) <= 32.0D * 32.0D) {
                                p.displayClientMessage(Component.translatable("message.grandwitch.nanny.calls", witch.disguiseName()), false);
                            }
                        }
                    } else if (witch.tickCount % 10 == 0 || witch.getNavigation().isDone()) {
                        witch.getNavigation().moveTo(next, 0.9D);
                    }
                    return;
                }
                if (witch.following.isEmpty()) {
                    witch.nannyCooldown = 600;            // no one to take: she tries again in half a minute
                    return;
                }
                witch.leading = true;
                witch.nannyTicks = 0;
                witch.nannyDest = destination(server);
            }
            // on the way, slowly, for the children
            BlockPos dest = witch.nannyDest;
            if (witch.tickCount % 10 == 0 || witch.getNavigation().isDone()) {
                witch.getNavigation().moveTo(dest.getX() + 0.5D, dest.getY(), dest.getZ() + 0.5D, 0.8D);
            }
            if (witch.distanceToSqr(dest.getX() + 0.5D, dest.getY(), dest.getZ() + 0.5D) <= 5.0D * 5.0D || witch.nannyTicks > nannyTimeout) {
                // there: the children are made mice
                List<UUID> ids = new ArrayList<>(witch.following);
                witch.following.clear();
                for (UUID id : ids) {
                    if (server.getEntity(id) instanceof Villager kid && kid.isAlive()) {
                        kid.getPersistentData().remove(LED);
                        VillageMouse made = witch.turnChild(kid);
                        if (made != null) {
                            witch.cageMouse(made);               // into the cage, in her house, if she has one
                        }
                    }
                }
                witch.leading = false;
                witch.nannyDest = null;
                witch.nannyCooldown = 2400;
            }
        }

        @Override
        public void stop() {
            witch.getNavigation().stop();
        }
    }

    /** Disguised, she walks up to a child villager if there is one near (and turns it), or else to the nearest player, to offer something. */
    /**
     * A mouse she was after has gone into a tunnel. If there is an opening at the side of the tunnel within 5 blocks of tunnel of it, she goes there, she goes there, bends
     * over, waits a moment (long enough to be heard and to get away from), puts her arm in along the tunnel, round any corner there is, and pulls the mouse out.
     */
    private static final class ReachGoal extends Goal {

        static final int REACH = 4;                              // tunnel blocks in from the mouth, the mouth's own being the first of five

        private final GrandWitch witch;
        private LivingEntity victim;
        private List<BlockPos> path = List.of();
        private Direction facing = Direction.NORTH;
        private net.minecraft.world.phys.Vec3 stand = net.minecraft.world.phys.Vec3.ZERO;
        private int phase;
        private int ticks;
        private int giveUpWalking;
        private List<BlockPos> fullPath = List.of();
        private boolean grope;                       // the mouse is just out of reach (the sixth or seventh block): she feels about in there and finds nothing

        ReachGoal(GrandWitch witch) {
            this.witch = witch;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private static boolean net(Level level, BlockPos p) {
            BlockState s = level.getBlockState(p);
            return s.is(ModBlocks.MOUSE_HOLE.get()) || s.is(ModBlocks.MOUSE_TUNNEL.get());
        }

        private static boolean free(Level level, BlockPos p) {
            return level.getBlockState(p).getCollisionShape(level, p).isEmpty();
        }

        /** Whether the block here, a tunnel or a hole, is open on this side: a hole at its two ends, a tunnel where its side is not shut. */
        private static boolean open(Level level, BlockPos pos, Direction side) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.MOUSE_HOLE.get())) {
                return state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING).getAxis() == side.getAxis();
            }
            return state.is(ModBlocks.MOUSE_TUNNEL.get()) && MouseTunnelBlock.isOpen(state, side);
        }

        /**
         * A side she could lie down at to reach in at this block flat along the ground: a free place next to it that the tunnel is open to, with ground under it. Which way
         * from the block that place is; nearest her first.
         */
        private Direction sideFor(Level level, BlockPos mouth) {
            BlockState state = level.getBlockState(mouth);
            List<Direction> ways = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
            ways.sort(java.util.Comparator.comparingDouble(d -> witch.distanceToSqr(mouth.getX() + 0.5D + d.getStepX(), mouth.getY(), mouth.getZ() + 0.5D + d.getStepZ())));
            for (Direction d : ways) {
                if (!open(level, mouth, d)) {
                    continue;                                 // a hole is open at its two ends, a tunnel where it is not shut
                }
                BlockPos front = mouth.relative(d);
                if (!net(level, front) && free(level, front) && free(level, front.above()) && level.getBlockState(front.below()).isFaceSturdy(level, front.below(), Direction.UP)) {
                    return d;
                }
            }
            return null;
        }

        /** Looks for a way to reach the victim, by an opening at the side of the tunnel and only so (the top of it is the ground: there is no way in there); fills in the path, how she stands and which way she faces. */
        private boolean plan(LivingEntity v, boolean tryFurther) {
            Level level = witch.level();
            BlockPos start = v.blockPosition();
            if (!net(level, start)) {
                return false;
            }
            Map<BlockPos, BlockPos> toward = new HashMap<>();
            Map<BlockPos, Integer> depth = new HashMap<>();
            List<BlockPos> order = new ArrayList<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            toward.put(start, null);
            depth.put(start, 0);
            queue.add(start);
            while (!queue.isEmpty()) {
                BlockPos b = queue.poll();
                order.add(b);
                if (depth.get(b) < REACH + 2) {
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        BlockPos n = b.relative(d);
                        if (!toward.containsKey(n) && net(level, n) && open(level, b, d) && open(level, n, d.getOpposite())) {      // only by the ways that are open at both ends
                            toward.put(n, b);
                            depth.put(n, depth.get(b) + 1);
                            queue.add(n);
                        }
                    }
                }
            }
            BlockPos further = null;
            Direction furtherOut = null;
            for (BlockPos b : order) {                         // the nearest way in from the side, lying along the ground in front of it
                Direction out = sideFor(level, b);
                if (out == null) {
                    continue;
                }
                if (depth.get(b) > REACH) {                    // it is the sixth or seventh block from here: not to be taken, but to be felt for
                    if (further == null) {
                        further = b;
                        furtherOut = out;
                    }
                    continue;
                }
                setStand(b, out, toward, false);
                return true;
            }
            if (further != null && tryFurther) {
                setStand(further, furtherOut, toward, true);
                return true;
            }
            return false;
        }

        private void setStand(BlockPos mouth, Direction out, Map<BlockPos, BlockPos> toward, boolean feeling) {
            List<BlockPos> found = new ArrayList<>();
            for (BlockPos p = mouth; p != null; p = toward.get(p)) {
                found.add(p);
            }
            fullPath = found;
            path = feeling ? found.subList(0, Math.min(found.size(), REACH + 1)) : found;     // all of her arm, and no more
            grope = feeling;
            BlockPos front = mouth.relative(out);
            facing = out.getOpposite();
            stand = new net.minecraft.world.phys.Vec3(front.getX() + 0.5D + out.getStepX() * 0.3D, front.getY(), front.getZ() + 0.5D + out.getStepZ() * 0.3D);
        }

        @Override
        public boolean canUse() {
            if (!WitchConfig.WITCH_REACH.get()) {
                witch.reachWhy = "setting off";
                return false;
            }
            if (witch.isMouseForm() || witch.isDisguised() || witch.onBroom() || witch.reachCooldown > 0) {
                witch.reachWhy = (witch.isMouseForm() ? "mouse-form " : "") + (witch.isDisguised() ? "disguised " : "") + (witch.onBroom() ? "on-broom " : "") + (witch.reachCooldown > 0 ? "cooldown " + witch.reachCooldown : "");
                return false;
            }
            LivingEntity v = witch.lastVictim;
            if (v == null || !v.isAlive() || v.isSpectator() || witch.tickCount - witch.lastSeen > 600 || !isMouseLike(v) || witch.exempt(v) || !MouseHoleBlock.hidden(v)) {
                witch.reachWhy = v == null ? "no victim" : !v.isAlive() ? "victim dead" : witch.tickCount - witch.lastSeen > 600 ? "victim forgotten" : witch.exempt(v) ? "victim exempt" : !MouseHoleBlock.hidden(v) ? "victim not hidden at " + v.blockPosition() : "other";
                return false;
            }
            // one that is 6 or 7 blocks in is only tried for now and then, at random
            boolean roll = witch.random.nextInt(gropeOneIn) == 0;
            if (!plan(v, roll)) {
                witch.reachWhy = "no way in (roll " + roll + ")";
                return false;
            }
            witch.reachWhy = "ok";
            victim = v;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return phase >= 0;
        }

        @Override
        public void start() {
            witch.trail("start" + (grope ? "(grope)" : "") + "@" + stand.x + "," + stand.z);
            phase = 0;
            ticks = 0;
            giveUpWalking = 0;
        }

        private void end(int cooldown) {
            witch.trail("end(" + cooldown + ") from phase " + phase);
            phase = -1;
            witch.reachCooldown = cooldown;
        }

        @Override
        public void tick() {
            if (victim == null || !victim.isAlive()) {
                end(40);
                return;
            }
            if (phase == 0) {
                // to where she lies down
                if (++giveUpWalking > 200) {
                    end(100);
                    return;
                }
                double dx = stand.x - witch.getX(), dz = stand.z - witch.getZ();
                double near = dx * dx + dz * dz;
                // a path ends within a half block or so of where it was sent, and not on it: near enough, and she is put where she lies
                if ((near < 0.7D * 0.7D || (witch.getNavigation().isDone() && near < 1.5D * 1.5D)) && Math.abs(stand.y - witch.getY()) < 1.2D) {
                    witch.getNavigation().stop();
                    witch.setPos(stand.x, witch.getY(), stand.z);
                    witch.trail("arrived");
                    phase = 1;
                    ticks = 0;
                } else {
                    if (witch.tickCount % 10 == 0 || witch.getNavigation().isDone()) {
                        witch.getNavigation().moveTo(stand.x, stand.y, stand.z, 1.1D);
                    }
                    if (witch.tickCount % 10 == 0 && !MouseHoleBlock.hidden(victim)) {
                        end(20);                      // it has come out: she goes for it as she does
                    }
                }
                return;
            }
            // lying over the mouth, her head toward the way in
            witch.getNavigation().stop();
            witch.setDeltaMovement(0.0D, witch.getDeltaMovement().y, 0.0D);
            witch.setYRot(facing.toYRot());
            witch.yBodyRot = facing.toYRot();
            witch.yHeadRot = facing.toYRot();
            witch.entityData.set(PRONE, true);
            ticks++;
            if (ticks % 5 == 0) {
                net.minecraft.world.phys.Vec3 lying = stand;
                Direction lyingFacing = facing;
                if (!MouseHoleBlock.hidden(victim) || !plan(victim, true) || stand.distanceToSqr(lying) > 1.5D * 1.5D) {
                    end(60);                          // it has got out of reach, or has come out
                    return;
                }
                stand = lying;                       // she stays where she is lying, and has her arm go the new way
                facing = lyingFacing;
            }
            if (phase == 1 && ticks >= 40) {
                net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                tag.putLongArray("p", path.stream().mapToLong(BlockPos::asLong).toArray());
                witch.entityData.set(ARM, tag);
                phase = 2;
                ticks = 0;
                witch.trail("arm in");
            } else if (phase == 2) {
                witch.entityData.set(ARM_TICKS, Math.min(ticks, ARM_FULL));
                if (ticks >= ARM_FULL) {
                    // at the sixth block she gets hold of it one time in five, at the seventh one in ten, by stretching; and else she finds nothing, and it hears her
                    float luck = gropeLuck >= 0.0F ? gropeLuck : fullPath.size() <= REACH + 2 ? 0.2F : 0.1F;
                    if (grope && witch.random.nextFloat() < luck) {
                        path = fullPath;
                        net.minecraft.nbt.CompoundTag all = new net.minecraft.nbt.CompoundTag();
                        all.putLongArray("p", path.stream().mapToLong(BlockPos::asLong).toArray());
                        witch.entityData.set(ARM, all);
                        grope = false;
                        pull();
                    } else if (grope) {
                        witch.level().playSound(null, witch.blockPosition(), SoundEvents.WITCH_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.7F);
                    } else {
                        pull();
                    }
                    phase = 3;
                    ticks = 0;
                }
            } else if (phase == 3 && ticks >= 10) {
                end(grope ? 100 : 200);
            }
        }

        /** The mouse is taken by the scruff and brought up out of the tunnel, to where she is. */
        private void pull() {
            BlockPos mouth = path.get(0);
            if (witch.level() instanceof ServerLevel server && MouseHoleBlock.hidden(victim)) {
                victim.teleportTo(mouth.getX() + 0.5D, mouth.getY() + 1.0D, mouth.getZ() + 0.5D);
                victim.setDeltaMovement(0.0D, 0.1D, 0.0D);
                victim.hurtMarked = true;
                server.sendParticles(ParticleTypes.POOF, mouth.getX() + 0.5D, mouth.getY() + 1.0D, mouth.getZ() + 0.5D, 12, 0.2D, 0.1D, 0.2D, 0.02D);
                server.playSound(null, mouth, SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1.0F, 1.2F);
                witch.lastSeen = witch.tickCount;
                witch.rageTicks = 100;
                witch.setRage(true);
            }
        }

        @Override
        public void stop() {
            phase = -1;
            witch.entityData.set(PRONE, false);
            witch.entityData.set(ARM, new net.minecraft.nbt.CompoundTag());
            witch.entityData.set(ARM_TICKS, 0);
            witch.getNavigation().stop();
            victim = null;
        }
    }

    /** She dislikes a baby zombie: she goes up to the nearest within 24 blocks and makes a mouse of it. */
    private static final class ZombieGoal extends Goal {
        private final GrandWitch witch;
        private net.minecraft.world.entity.monster.Zombie target;
        private int near;

        ZombieGoal(GrandWitch witch) {
            this.witch = witch;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!WitchConfig.TURNS_BABY_ZOMBIES.get() || witch.isRaging() || witch.isMouseForm() || witch.zombieCooldown > 0 || witch.onBroom()) {
                return false;
            }
            target = null;
            for (var z : witch.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class, witch.getBoundingBox().inflate(24.0D), z -> z.isBaby() && z.isAlive())) {
                if (target == null || witch.distanceToSqr(z) < witch.distanceToSqr(target)) {
                    target = z;
                }
            }
            near = 0;
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && !witch.isRaging() && !witch.isMouseForm() && witch.distanceToSqr(target) < 32.0D * 32.0D;
        }

        @Override
        public void tick() {
            witch.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (witch.distanceToSqr(target) <= 4.0D * 4.0D) {
                witch.getNavigation().stop();
                if (++near >= 20) {
                    witch.turnZombie(target);
                    target = null;
                }
            } else if (witch.tickCount % 10 == 0 || witch.getNavigation().isDone()) {
                witch.getNavigation().moveTo(target, 1.0D);
            }
        }

        @Override
        public void stop() {
            witch.getNavigation().stop();
            target = null;
        }
    }

    private static final class ApproachGoal extends Goal {

        private final GrandWitch witch;
        private LivingEntity target;
        private int near;

        ApproachGoal(GrandWitch witch) {
            this.witch = witch;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!witch.isDisguised() || witch.isRaging() || witch.kind == WitchKind.NANNY) {
                return false;
            }
            target = null;
            if (witch.kind == WitchKind.GIFT && WitchConfig.TURNS_CHILDREN.get() && witch.turnCooldown <= 0) {
                for (Villager v : witch.level().getEntitiesOfClass(Villager.class, witch.getBoundingBox().inflate(40.0D), Villager::isBaby)) {
                    if (target == null || witch.distanceToSqr(v) < witch.distanceToSqr(target)) {
                        target = v;
                    }
                }
                // no child about: a grown villager or trader, as many as she is allowed
                if (target == null && WitchConfig.TURNS_ADULTS.get() && witch.adultsTurned < WitchConfig.ADULTS_PER_VISIT.get()) {
                    for (net.minecraft.world.entity.npc.AbstractVillager v : witch.level().getEntitiesOfClass(net.minecraft.world.entity.npc.AbstractVillager.class, witch.getBoundingBox().inflate(40.0D))) {
                        if (target == null || witch.distanceToSqr(v) < witch.distanceToSqr(target)) {
                            target = v;
                        }
                    }
                }
            }
            if (target == null) {
                Player p = witch.level().getNearestPlayer(witch, 48.0D);
                if (p != null && !p.isSpectator() && !Mice.isMouse(p) && witch.distanceToSqr(p) > 3.0D * 3.0D) {
                    target = p;
                }
            }
            near = 0;
            return target != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (!witch.isDisguised() || witch.isRaging() || target == null || !target.isAlive() || witch.distanceToSqr(target) > 56.0D * 56.0D) {
                return false;
            }
            return target instanceof net.minecraft.world.entity.npc.AbstractVillager || witch.distanceToSqr(target) > 2.5D * 2.5D;
        }

        @Override
        public void tick() {
            if (target instanceof net.minecraft.world.entity.npc.AbstractVillager child) {
                if (witch.distanceToSqr(child) <= 2.2D * 2.2D) {
                    witch.getNavigation().stop();
                    witch.getLookControl().setLookAt(child, 30.0F, 30.0F);
                    if (++near >= 40) {
                        witch.turnChild(child);
                        target = null;
                    }
                    return;
                }
            }
            if (witch.tickCount % 10 == 0 || witch.getNavigation().isDone()) {
                witch.getNavigation().moveTo(target, 0.9D);
            }
        }

        @Override
        public void stop() {
            witch.getNavigation().stop();
            target = null;
        }
    }
}

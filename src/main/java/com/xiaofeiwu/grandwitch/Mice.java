package com.xiaofeiwu.grandwitch;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A player who has eaten what the Grand Witch gave is a mouse: a box 0.4 across and high with the eyes 0.3 up, sixty percent faster, a weak hand,
 * little hurt by a fall, hunted by cats, and drawn as a mouse. It is kept on the player ({@code getPersistentData}) with the game time it ends and
 * the witch that did it; it ends by itself, with milk, with the witch's death, or with the player's own.
 * The clients are told by {@link MousePacket}; what they know is kept here too, so that the size of the box comes out the same on both sides.
 */
@Mod.EventBusSubscriber(modid = GrandWitchMod.MODID)
public final class Mice {

    static final String KEY = "GrandWitchMouse";
    static final String FOOD = "GrandWitchFood";

    private static final UUID SPEED_ID = UUID.fromString("7c1b6d52-9a0e-4a63-8f0b-2e8d3c9b41a1");
    private static final UUID HAND_ID = UUID.fromString("0f4a98d1-3b7c-4e2a-a6d4-51c7e2b8f3a2");
    private static final Set<UUID> CLIENT = ConcurrentHashMap.newKeySet();
    /** What a player has begun to eat or drink of hers: whose it is, and whether it is a potion (and not food). */
    private record Gift(UUID witch, boolean potion) {
    }

    private static final Map<UUID, Gift> EATING = new ConcurrentHashMap<>();

    /** Test hook: the roll made when a sold potion is drunk, in place of a random one. */
    static Double potionRollOverride;

    private Mice() {
    }

    public static boolean isMouse(Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        return player.level().isClientSide ? CLIENT.contains(player.getUUID()) : player.getPersistentData().contains(KEY);
    }

    public static void setClient(UUID player, boolean mouse) {
        if (mouse) {
            CLIENT.add(player);
        } else {
            CLIENT.remove(player);
        }
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    /** The witch (if any) whose food made this player a mouse. */
    static UUID madeBy(Player player) {
        CompoundTag tag = player.getPersistentData().getCompound(KEY);
        return tag.hasUUID("Witch") ? tag.getUUID("Witch") : null;
    }

    // ------------------------------------------------------------------ becoming one, and ceasing to be ------------------------------------------------------------------

    /** Server side. {@code witch} is who did it, or null. */
    public static void become(Player player, UUID witch, int seconds) {
        if (player.level().isClientSide) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putLong("Until", player.level().getGameTime() + seconds * 20L);
        if (witch != null) {
            tag.putUUID("Witch", witch);
        }
        player.getPersistentData().put(KEY, tag);
        if (player.getVehicle() instanceof BroomEntity) {
            player.stopRiding();                              // a mouse is not on a broom
        }
        refresh(player);
        player.displayClientMessage(Component.translatable("message.grandwitch.mouse"), false);
    }

    /** Server side. */
    public static void cure(Player player, boolean say) {
        if (player.level().isClientSide || !player.getPersistentData().contains(KEY)) {
            return;
        }
        player.getPersistentData().remove(KEY);
        refresh(player);
        if (say) {
            player.displayClientMessage(Component.translatable("message.grandwitch.cured"), false);
        }
    }

    private static boolean isOurs(MobEffectInstance e) {
        return e.isAmbient() && !e.isVisible() && !e.showIcon() && e.getAmplifier() == 0;
    }

    /** Makes the player's attributes, box and what the clients know agree with the state kept. */
    static void refresh(Player player) {
        boolean mouse = player.getPersistentData().contains(KEY);
        modify(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, "mouse speed", WitchConfig.MOUSE_SPEED.get(), mouse);
        modify(player.getAttribute(Attributes.ATTACK_DAMAGE), HAND_ID, "mouse hand", -0.7D, mouse);
        MobEffectInstance sight = player.getEffect(MobEffects.NIGHT_VISION);
        if (!mouse && sight != null && isOurs(sight)) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
        player.refreshDimensions();
        if (player instanceof ServerPlayer sp) {
            ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp), new MousePacket(sp.getUUID(), mouse));
        }
    }

    private static void modify(AttributeInstance attribute, UUID id, String name, double amount, boolean on) {
        if (attribute == null) {
            return;
        }
        if (on && attribute.getModifier(id) == null) {
            attribute.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (!on && attribute.getModifier(id) != null) {
            attribute.removeModifier(id);
        }
    }

    // ------------------------------------------------------------------ the box ------------------------------------------------------------------

    @SubscribeEvent
    public static void size(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player player && isMouse(player)) {
            event.setNewSize(EntityDimensions.scalable(0.4F, 0.4F));
            event.setNewEyeHeight(0.3F);
        }
    }

    // ------------------------------------------------------------------ the food ------------------------------------------------------------------

    /** What she gives: ordinary food, with a mark on it that cannot be seen. */
    static ItemStack poisoned(net.minecraft.util.RandomSource random, UUID witch) {
        var items = new net.minecraft.world.item.Item[]{Items.COOKIE, Items.BREAD, Items.APPLE, Items.BAKED_POTATO, Items.PUMPKIN_PIE};
        ItemStack stack = new ItemStack(items[random.nextInt(items.length)]);
        stack.getOrCreateTag().putBoolean(FOOD, true);
        stack.getOrCreateTag().putUUID("Witch", witch);
        return stack;
    }

    static boolean isPoisoned(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().getBoolean(FOOD);
    }

    @SubscribeEvent
    public static void startEating(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player && isPoisoned(event.getItem())) {
            CompoundTag tag = event.getItem().getTag();
            EATING.put(player.getUUID(), new Gift(tag.hasUUID("Witch") ? tag.getUUID("Witch") : new UUID(0L, 0L), event.getItem().is(Items.POTION)));
        }
    }

    @SubscribeEvent
    public static void stopEating(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EATING.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void finishEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Gift gift = EATING.remove(player.getUUID());
        if (gift != null && gift.potion()) {
            drunk(player, gift.witch());
        } else if (gift != null) {
            eaten(player, gift.witch());
        } else if (event.getItem().is(Items.MILK_BUCKET) && WitchConfig.MILK_CURES.get()) {
            cure(player, true);
        }
    }

    /** Test hook: the roll made when a gift is eaten, in place of a random one (0 is the mouse, 1 a reward). */
    static Double giftRollOverride;
    /** Test hook: which of the rewards, in place of a random one. */
    static Integer rewardOverride;

    static final int REWARDS = 5;

    /**
     * A gift of hers is eaten. What it turns out to be is decided now, and not when it was given, so that no two of them can be told apart by anything
     * they carry: the mouse, or one of five rewards.
     */
    static void eaten(Player player, UUID witch) {
        double roll = giftRollOverride != null ? giftRollOverride : player.getRandom().nextDouble();
        if (roll < WitchConfig.POISON_CHANCE.get()) {
            ate(player, witch);
        } else {
            reward(player, rewardOverride != null ? rewardOverride : player.getRandom().nextInt(REWARDS));
        }
    }

    /** A potion of the old woman's is drunk: it does what it looked to do, and it may be the mouse as well. */
    static void drunk(Player player, UUID witch) {
        double roll = potionRollOverride != null ? potionRollOverride : player.getRandom().nextDouble();
        if (roll < WitchConfig.POTION_POISON.get() && !isMouse(player)) {
            become(player, witch.getMostSignificantBits() == 0L && witch.getLeastSignificantBits() == 0L ? null : witch, WitchConfig.MOUSE_SECONDS.get());
        }
    }

    /** What the gift was, when it was a good one. */
    static void reward(Player player, int which) {
        switch (which) {
            case 0 -> give(player, new ItemStack(Items.GOLDEN_APPLE), "message.grandwitch.reward.apple");
            case 1 -> give(player, new ItemStack(Items.EMERALD, 2 + player.getRandom().nextInt(3)), "message.grandwitch.reward.emeralds");
            case 2 -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120 * 20, 1));
                player.displayClientMessage(Component.translatable("message.grandwitch.reward.heal"), false);
            }
            case 3 -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 180 * 20, 1));
                player.displayClientMessage(Component.translatable("message.grandwitch.reward.speed"), false);
            }
            default -> {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 300 * 20, 1));
                player.displayClientMessage(Component.translatable("message.grandwitch.reward.luck"), false);
            }
        }
    }

    private static void give(Player player, ItemStack stack, String message) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.displayClientMessage(Component.translatable(message), false);
    }

    /** She is eaten: the player is a mouse for as long as is set. */
    static void ate(Player player, UUID witch) {
        if (!isMouse(player)) {
            become(player, witch.getMostSignificantBits() == 0L && witch.getLeastSignificantBits() == 0L ? null : witch, WitchConfig.MOUSE_SECONDS.get());
        }
    }

    // ------------------------------------------------------------------ keeping it right ------------------------------------------------------------------

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || event.player.tickCount % 20 != 0) {
            return;
        }
        Player player = event.player;
        CompoundTag tag = player.getPersistentData().getCompound(KEY);
        long now = player.level().getGameTime();
        if (tag.contains("Until") && now >= tag.getLong("Until")) {
            cure(player, true);
        }
        // it sees in the dark: a quiet effect, kept up while it lasts, and taken off with it (not one that was a potion's)
        if (isMouse(player) && WitchConfig.NIGHT_VISION.get()) {
            MobEffectInstance have = player.getEffect(MobEffects.NIGHT_VISION);
            if (have == null || (isOurs(have) && have.getDuration() < 300)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
            }
        }
        // a mouse squeaks now and then by itself, unless it is keeping still and quiet
        if (isMouse(player)) {
            long next = NEXT_SQUEAK.computeIfAbsent(player.getUUID(), k -> now + squeakGap(player));
            if (now >= next) {
                NEXT_SQUEAK.put(player.getUUID(), now + squeakGap(player));
                if (!player.isShiftKeyDown()) {
                    squeak(player);
                }
            }
        } else {
            NEXT_SQUEAK.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        refresh(event.getEntity());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent event) {
        refresh(event.getEntity());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        refresh(event.getEntity());
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        Player now = event.getEntity();
        if (event.isWasDeath()) {
            now.getPersistentData().remove(KEY);                 // dying ends it
        } else if (event.getOriginal().getPersistentData().contains(KEY)) {
            now.getPersistentData().put(KEY, event.getOriginal().getPersistentData().getCompound(KEY).copy());
        }
    }

    @SubscribeEvent
    public static void startedTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player target && event.getEntity() instanceof ServerPlayer watcher) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> watcher), new MousePacket(target.getUUID(), isMouse(target)));
        }
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            cure(player, false);
        }
    }

    /** A fall that is a long way for a player is nothing to a mouse. */
    @SubscribeEvent
    public static void fell(LivingFallEvent event) {
        if (isMouse(event.getEntity())) {
            event.setDistance(event.getDistance() * 0.25F);
        }
    }

    // ------------------------------------------------------------------ nibbling ------------------------------------------------------------------

    private static final Map<UUID, Long> NIBBLED = new ConcurrentHashMap<>();

    /** The container at a place that a mouse could get into, or null. (Not the ender chest: that is the player's own.) */
    private static Container containerAt(net.minecraft.world.level.Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof Container c && !(be instanceof EnderChestBlockEntity) ? c : null;
    }

    /**
     * A mouse that is crouching and uses a chest does not open it: it takes one bit of food out of it and eats it, at once, so that nothing in the
     * chest moves but that. It is not stealing as a thief steals: nothing here rings a bell or goes in a ledger.
     * @return whether it ate something
     */
    public static boolean nibble(Player player, BlockPos pos) {
        Container container = containerAt(player.level(), pos);
        long now = player.level().getGameTime();
        Long last = NIBBLED.get(player.getUUID());
        if (container == null || (last != null && now - last < 20L)) {
            return false;
        }
        // the antidote is not food, but it is what a mouse most wants out of a chest: it is drunk on the spot, and the bottle is left where it was
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(ModItems.ANTIDOTE.get())) {
                NIBBLED.put(player.getUUID(), now);
                container.removeItem(i, 1);
                if (container.getItem(i).isEmpty()) {
                    container.setItem(i, new ItemStack(Items.GLASS_BOTTLE));
                }
                container.setChanged();
                cure(player, true);
                if (player.level() instanceof ServerLevel server) {
                    server.playSound(null, pos, SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.8F, 1.4F);
                }
                return true;
            }
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            var food = stack.getFoodProperties(player);
            if (stack.isEdible() && food != null && !isPoisoned(stack)) {
                NIBBLED.put(player.getUUID(), now);
                ItemStack bit = container.removeItem(i, 1);
                container.setChanged();
                player.getFoodData().eat(food.getNutrition(), food.getSaturationModifier());
                if (player.level() instanceof ServerLevel server) {
                    server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, bit), pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 8, 0.2D, 0.1D, 0.2D, 0.05D);
                    server.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.6F, 1.6F);
                }
                return true;
            }
        }
        NIBBLED.put(player.getUUID(), now);
        player.displayClientMessage(Component.translatable("message.grandwitch.nothing_to_eat"), true);
        return false;
    }

    /**
     * What a mouse can do with a block: nothing, but nibble what is in a chest if it is crouching. No door opens to it, no button gives, no table or
     * furnace or bed is used, no block is put down, and no bucket is emptied. What it holds can still be eaten or drunk.
     */
    @SubscribeEvent
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!isMouse(player)) {
            return;
        }
        if (player.isShiftKeyDown() && containerAt(player.level(), event.getPos()) != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!player.level().isClientSide && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
                nibble(player, event.getPos());
            }
            return;
        }
        event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
        if (cannotPlace(event.getItemStack())) {
            event.setUseItem(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }

    private static boolean cannotPlace(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem || stack.getItem() instanceof net.minecraft.world.item.BucketItem;
    }

    @SubscribeEvent
    public static void useItem(PlayerInteractEvent.RightClickItem event) {
        if (isMouse(event.getEntity()) && event.getItemStack().getItem() instanceof net.minecraft.world.item.BucketItem) {
            event.setCanceled(true);                          // not water or lava out of a bucket (milk is not one)
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    /** What a mouse can dig: the soft ground, the dirt kind. */
    static boolean diggable(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.DIRT) && !state.is(ModBlocks.MOUSE_TUNNEL.get());       // a tunnel is of that kind, so that plants will stand on it, but it is dug already
    }

    /** Level with the mouse: a tunnel is a cross, open on each side and not up or down, so what is dug under it or over it could not be got to. */
    static boolean sameLevel(Player player, BlockPos pos) {
        return pos.getY() == player.blockPosition().getY();
    }

    @SubscribeEvent
    public static void startBreaking(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (isMouse(player) && !(diggable(player.level().getBlockState(event.getPos())) && sameLevel(player, event.getPos()))) {
            event.setCanceled(true);
        }
    }

    /** A mouse digs slowly. */
    @SubscribeEvent
    public static void digSpeed(net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed event) {
        if (isMouse(event.getEntity())) {
            event.setNewSpeed(event.getNewSpeed() * 0.6F);
        }
    }

    /**
     * When a mouse breaks a block, it breaks nothing: soft ground becomes a tunnel (as many as it likes), and anything else stays as it is.
     * This is taken after the others and not when one of them has stopped the breaking, so that what protects a place from being dug still does.
     */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOW)
    public static void broke(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!isMouse(player)) {
            return;
        }
        event.setCanceled(true);
        if (!(event.getLevel() instanceof ServerLevel level) || !diggable(event.getState()) || !sameLevel(player, event.getPos())) {
            if (!player.level().isClientSide && diggable(event.getState())) {
                player.displayClientMessage(Component.translatable("message.grandwitch.dig_level"), true);
            }
            return;
        }
        BlockPos pos = event.getPos();
        level.setBlock(pos, ModBlocks.MOUSE_TUNNEL.get().defaultBlockState().setValue(MouseTunnelBlock.GRASSY, event.getState().is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)), 3);       // dug out of grass, it keeps its grass
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, event.getState()), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 10, 0.25D, 0.25D, 0.25D, 0.05D);
        level.playSound(null, pos, event.getState().getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    // ------------------------------------------------------------------ squeaking ------------------------------------------------------------------

    private static final Map<UUID, Long> SQUEAKED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_SQUEAK = new ConcurrentHashMap<>();

    /** Ticks until the next squeak: the configured time, give or take a third. */
    private static long squeakGap(Player player) {
        return (long) (WitchConfig.SQUEAK_SECONDS.get() * 20.0D * (0.7D + 0.6D * player.getRandom().nextDouble()));
    }

    /** Test hook: the next squeak is due at once. */
    static void squeakNow(Player player) {
        NEXT_SQUEAK.put(player.getUUID(), 0L);
    }

    /** A mouse that is hurt cries out. */
    @SubscribeEvent
    public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide && isMouse(player)) {
            squeak(player);
        }
    }
    private static final String LURE = "GrandWitchLure";
    private static final String LURE_TARGET = "GrandWitchLureTarget";

    /**
     * The mouse squeaks, and every cat within 32 blocks turns to it for ten seconds, and to nothing else in that time: a cat that was after
     * another mouse is drawn off it. A cat does not go for its own owner.
     * @return whether any cat heard it
     */
    public static boolean squeak(Player player) {
        if (!isMouse(player) || !(player.level() instanceof ServerLevel server)) {
            return false;
        }
        long now = server.getGameTime();
        Long last = SQUEAKED.get(player.getUUID());
        if (last != null && now - last < 160L) {
            return false;
        }
        SQUEAKED.put(player.getUUID(), now);
        server.playSound(null, player.blockPosition(), SoundEvents.BAT_AMBIENT, SoundSource.PLAYERS, 0.8F, 2.0F);
        int heard = 0;
        for (Cat cat : server.getEntitiesOfClass(Cat.class, player.getBoundingBox().inflate(32.0D))) {
            if (cat.isTame() && player.getUUID().equals(cat.getOwnerUUID())) {
                continue;
            }
            cat.setTarget(player);
            cat.getPersistentData().putLong(LURE, now + 200L);
            cat.getPersistentData().putUUID(LURE_TARGET, player.getUUID());
            heard++;
        }
        if (heard > 0) {
            player.displayClientMessage(Component.translatable("message.grandwitch.squeak_heard", heard), true);
        }
        return heard > 0;
    }

    /** A cat that was sent after someone gives it up when the ten seconds are over. */
    @SubscribeEvent
    public static void catLoses(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Cat cat) || cat.level().isClientSide || cat.tickCount % 10 != 0) {
            return;
        }
        long until = cat.getPersistentData().getLong(LURE);
        if (until > 0L && cat.level().getGameTime() >= until) {
            cat.getPersistentData().remove(LURE);
            cat.getPersistentData().remove(LURE_TARGET);
        }
    }

    // ------------------------------------------------------------------ cats ------------------------------------------------------------------

    static boolean catHunts(Cat cat, LivingEntity target) {
        if (target instanceof GrandWitch w) {
            return WitchConfig.CATS_HUNT.get() && w.isMouseForm();         // the witch, when she is a mouse
        }
        // a cat that has been drawn by a squeak is after the one that squeaked, and no one else, while it lasts
        CompoundTag data = cat.getPersistentData();
        if (data.getLong(LURE) > cat.level().getGameTime() && data.hasUUID(LURE_TARGET) && !data.getUUID(LURE_TARGET).equals(target.getUUID())) {
            return false;
        }
        return WitchConfig.CATS_HUNT.get() && isMouse(target) && !(cat.isTame() && target.getUUID().equals(cat.getOwnerUUID()));
    }

    @SubscribeEvent
    public static void catJoins(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Cat cat)) {
            return;
        }
        cat.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(cat, Player.class, 10, true, false, target -> catHunts(cat, target)));
        cat.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(cat, VillageMouse.class, 10, true, false, target -> WitchConfig.CATS_HUNT.get()));
        cat.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(cat, GrandWitch.class, 10, true, false, target -> WitchConfig.CATS_HUNT.get() && target instanceof GrandWitch w && w.isMouseForm()));
        cat.goalSelector.addGoal(3, new MeleeAttackGoal(cat, 1.5D, true));
    }

    /** What a stamp does besides the hurt: the mouse is pressed to the ground a moment. */
    static void flatten(Player player) {
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
    }
}

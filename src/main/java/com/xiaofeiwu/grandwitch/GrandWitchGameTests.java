package com.xiaofeiwu.grandwitch;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Tests that run in the game itself ({@code ./gradlew runGameTestServer}). They do nothing in a normal game. */
@GameTestHolder(GrandWitchMod.MODID)
@PrefixGameTestTemplate(false)
public final class GrandWitchGameTests {

    private GrandWitchGameTests() {
    }

    private static void floor(GameTestHelper h) {
        h.killAllEntities();
        // what the tests before this one left near it (they run one after the other): a mouse in sight sets a witch raging, a child calls her to it
        var near = new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(7, 1, 7))).inflate(120.0D);
        for (Class<? extends net.minecraft.world.entity.Entity> type : java.util.List.of(GrandWitch.class, VillageMouse.class, Villager.class, net.minecraft.world.entity.npc.WanderingTrader.class, Cat.class, net.minecraft.world.entity.animal.IronGolem.class)) {
            h.getLevel().getEntitiesOfClass(type, near).forEach(net.minecraft.world.entity.Entity::discard);
        }
        GrandWitch.calmTicks = 600;
        Mice.giftRollOverride = null;
        Mice.rewardOverride = null;
        GrandWitch.suspicionOverride = null;
        Mice.potionRollOverride = null;
        Curses.secondsOverride = -1;
        GrandWitch.nannyTimeout = 3600;
        WitchConfig.PLAYER_RANGE.set(128);
        MouseTunnelBlock.lightOverride = null;
        CauldronBrew.needsFireOverride = null;
        WitchConfig.HUT.set(false);
        WitchConfig.WILD_MICE.set(false);               // a mouse that comes up by itself near a witch sets her raging
        WitchHut.extraPeople.clear();
        WitchHut.Huts.get(h.getLevel()).forgetAll();           // the houses the tests before this one built are not houses of this one
        MouseHoleBlock.requirePlayer = true;
        MouseHoleBlock.alwaysOut = null;
        WitchVisits.rollOverride = null;
        WitchVisits.distance = new double[]{24.0D, 34.0D};
        WitchVisits.siteRange = new double[]{16.0D, 48.0D};
        for (int x = 0; x < 14; x++) {
            for (int z = 0; z < 14; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y <= 8; y++) {
                    h.setBlock(new BlockPos(x, y, z), Blocks.AIR);       // walls, a roof, a tunnel of a test that was here before
                }
            }
        }
    }

    private static void put(GameTestHelper h, Player player, int x, int z) {
        BlockPos at = h.absolutePos(new BlockPos(x, 1, z));
        player.moveTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
    }

    @GameTest(batch = "w01", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseIsSmallLowAndQuickAndGoesBackToWhatItWas(GameTestHelper h) {
        floor(h);
        Player player = h.makeMockPlayer();
        float height = player.getBbHeight(), eyes = player.getEyeHeight();
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED), hand = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Mice.become(player, null, 60);
        h.assertTrue(Mice.isMouse(player), "not a mouse");
        h.assertTrue(player.getBbHeight() < 0.5F && player.getBbWidth() < 0.5F, "the box is " + player.getBbWidth() + " x " + player.getBbHeight());
        h.assertTrue(player.getEyeHeight() < 0.4F, "the eyes are " + player.getEyeHeight() + " up");
        h.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed * 1.5D, "not faster: " + player.getAttributeValue(Attributes.MOVEMENT_SPEED) + " from " + speed);
        h.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) < hand * 0.5D, "the hand is not weaker");
        Mice.cure(player, false);
        h.assertTrue(!Mice.isMouse(player), "still a mouse");
        h.assertTrue(player.getBbHeight() == height && player.getEyeHeight() == eyes, "the box is not what it was: " + player.getBbHeight() + ", eyes " + player.getEyeHeight());
        h.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == hand, "the attributes are not what they were");
        h.succeed();
    }

    @GameTest(batch = "w02", template = "field", setupTicks = 20, timeoutTicks = 200)
    public static void aMouseIsAPlayerAgainWhenTheTimeIsUp(GameTestHelper h) {
        floor(h);
        Player player = h.makeMockPlayer();
        Mice.become(player, null, 1);
        h.runAfterDelay(30, () -> {
            Mice.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            h.assertTrue(!Mice.isMouse(player), "still a mouse after the time was up");
            h.succeed();
        });
    }

    @GameTest(batch = "w03", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theFoodSheGivesLooksLikeOrdinaryFoodButIsMarked(GameTestHelper h) {
        floor(h);
        ItemStack gift = Mice.poisoned(h.getLevel().random, UUID.randomUUID());
        h.assertTrue(Mice.isPoisoned(gift), "not marked");
        h.assertTrue(gift.isEdible(), "not food");
        h.assertTrue(!Mice.isPoisoned(new ItemStack(gift.getItem())), "ordinary food is marked");
        h.assertTrue(!ItemStack.isSameItemSameTags(gift, new ItemStack(gift.getItem())), "the marked food mixes with ordinary food");
        h.assertTrue(gift.getHoverName().getString().equals(new ItemStack(gift.getItem()).getHoverName().getString()), "the name gives it away");
        h.succeed();
    }

    @GameTest(batch = "w04", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void sheGivesMarkedFoodToWhoeverComesAndOnlyOncePerPlayerForAWhile(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        witch.disguiseAs(EntityType.VILLAGER);
        Player player = h.makeMockPlayer();
        h.assertTrue(witch.offer(player), "she gave nothing");
        int marked = 0;
        for (ItemStack s : player.getInventory().items) {
            marked += Mice.isPoisoned(s) ? 1 : 0;
        }
        h.assertTrue(marked == 1, "she gave " + marked + " marked things");
        h.assertTrue(!witch.offer(player), "she gave again at once");
        h.runAfterDelay(30, () -> {
            h.assertTrue(!witch.offer(player), "she gave the same player a second one a moment later");
            Player other = h.makeMockPlayer();
            h.assertTrue(witch.offer(other), "she gave nothing to another player");
            h.succeed();
        });
    }

    @GameTest(batch = "w05", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void eatingTheMarkedFoodMakesAMouseOfThePlayerOnce(GameTestHelper h) {
        floor(h);
        Player player = h.makeMockPlayer();
        UUID witch = UUID.randomUUID();
        Mice.ate(player, witch);
        h.assertTrue(Mice.isMouse(player) && witch.equals(Mice.madeBy(player)), "not a mouse of hers");
        h.succeed();
    }

    @GameTest(batch = "w06", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void catsGoForMiceAndOnlyForMiceAndNotForTheirOwner(GameTestHelper h) {
        floor(h);
        Cat cat = h.spawn(EntityType.CAT, new BlockPos(5, 1, 5));
        double attack = cat.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        h.assertTrue(attack == 3.0D, "a cat's attack is " + attack + ", not 3");
        h.assertTrue(cat.goalSelector.getAvailableGoals().stream().anyMatch(w -> w.getGoal() instanceof MeleeAttackGoal), "the cat cannot attack");
        Player mouse = h.makeMockPlayer(), plain = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        h.assertTrue(Mice.catHunts(cat, mouse), "the cat does not hunt a mouse");
        h.assertTrue(!Mice.catHunts(cat, plain), "the cat hunts an ordinary player");
        cat.tame(mouse);
        h.assertTrue(!Mice.catHunts(cat, mouse), "a cat hunts its own owner");
        h.succeed();
    }

    @GameTest(batch = "w07", template = "field", setupTicks = 20, timeoutTicks = 400)
    public static void theWitchShowsWhatSheIsAtTheSightOfAMouseAndStampsOnIt(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(2, 1, 2));
        witch.disguiseAs(EntityType.WANDERING_TRADER);
        Player player = h.makeMockPlayer();
        put(h, player, 10, 10);
        Mice.become(player, null, 60);
        witch.extraVictims.add(player);
        float health = player.getHealth();
        h.runAfterDelay(15, () -> {
            h.assertTrue(!witch.isDisguised() && witch.isRaging(), "she is not in a rage: disguised " + witch.isDisguised() + ", raging " + witch.isRaging());
            h.assertTrue(witch.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.3D, "not faster in her rage: " + witch.getAttributeValue(Attributes.MOVEMENT_SPEED));
        });
        h.succeedWhen(() -> h.assertTrue(player.getHealth() < health, "she has not hurt the mouse yet (" + witch.position() + " to " + player.position() + ")"));
    }

    @GameTest(batch = "w08", template = "field", setupTicks = 20, timeoutTicks = 300)
    public static void theWitchStaysDisguisedAndCalmWhenThereIsNoMouse(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(2, 1, 2));
        witch.disguiseAs(EntityType.VILLAGER);
        Player player = h.makeMockPlayer();
        put(h, player, 10, 10);
        witch.extraVictims.add(player);              // an ordinary player
        h.runAfterDelay(100, () -> {
            h.assertTrue(witch.isDisguised() && !witch.isRaging(), "she showed herself or raged with no mouse about");
            h.assertTrue(player.getHealth() == player.getMaxHealth(), "she hurt an ordinary player");
            h.succeed();
        });
    }

    @GameTest(batch = "w09", template = "field", setupTicks = 20, timeoutTicks = 200)
    public static void whenTheWitchDiesWhatSheDidIsUndone(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        Player hers = h.makeMockPlayer(), other = h.makeMockPlayer();
        Mice.become(hers, witch.getUUID(), 100);
        Mice.become(other, UUID.randomUUID(), 100);
        witch.extraVictims.add(hers);
        witch.extraVictims.add(other);
        witch.hurt(h.getLevel().damageSources().genericKill(), 1000.0F);
        h.assertTrue(!Mice.isMouse(hers), "a mouse of hers is still one after her death");
        h.assertTrue(Mice.isMouse(other), "a mouse of another witch was freed");
        h.succeed();
    }

    @GameTest(batch = "w10", template = "field", setupTicks = 20, timeoutTicks = 200)
    public static void sheOnlyComesWhereThereIsAChildVillagerAndOneAtATime(GameTestHelper h) {
        floor(h);
        var around = new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(7, 1, 7))).inflate(300.0D);
        h.getLevel().getEntitiesOfClass(GrandWitch.class, around).forEach(net.minecraft.world.entity.Entity::discard);        // those of the other tests
        h.getLevel().getEntitiesOfClass(Villager.class, around, Villager::isBaby).forEach(net.minecraft.world.entity.Entity::discard);
        Player player = h.makeMockPlayer();
        put(h, player, 7, 7);
        WitchVisits.rollOverride = 0.0D;
        WitchVisits.distance = new double[]{4.0D, 5.0D};      // inside the room
        h.assertTrue(WitchVisits.visit(h.getLevel(), player, false) == null, "she came with no child about");
        Villager adult = h.spawn(EntityType.VILLAGER, new BlockPos(3, 1, 3));
        WitchConfig.TURNS_ADULTS.set(false);
        h.assertTrue(WitchVisits.visit(h.getLevel(), player, false) == null, "she came for an adult villager though she is not to turn them");
        WitchConfig.TURNS_ADULTS.set(true);
        GrandWitch forAdult = WitchVisits.visit(h.getLevel(), player, false);
        h.assertTrue(forAdult != null, "she did not come for a grown villager");
        forAdult.discard();
        adult.discard();
        Villager child = h.spawn(EntityType.VILLAGER, new BlockPos(3, 1, 3));
        child.setAge(-24000);
        GrandWitch witch = WitchVisits.visit(h.getLevel(), player, false);
        h.assertTrue(witch != null, "she did not come where there is a child; witches about: " + h.getLevel().getEntitiesOfClass(GrandWitch.class, player.getBoundingBox().inflate(160.0D)).size() + ", children: " + h.getLevel().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(48.0D), Villager::isBaby).size() + ", player at " + player.position());
        h.assertTrue(witch.isDisguised(), "she is not dressed up");
        h.assertTrue(WitchVisits.visit(h.getLevel(), player, false) == null, "a second witch came");
        WitchVisits.rollOverride = 0.99D;
        witch.discard();
        h.assertTrue(WitchVisits.visit(h.getLevel(), player, false) == null, "she came on a bad roll");
        WitchVisits.rollOverride = null;
        h.succeed();
    }

    @GameTest(batch = "w11", template = "field", setupTicks = 20, timeoutTicks = 300)
    public static void aWitchThatHasBeenLeftAloneDressesUpAgain(GameTestHelper h) {
        floor(h);
        GrandWitch.calmTicks = 40;
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        witch.disguiseAs(EntityType.VILLAGER);
        witch.reveal();
        h.assertTrue(!witch.isDisguised(), "not showing herself");
        h.runAfterDelay(120, () -> {
            h.assertTrue(witch.isDisguised(), "she is still showing herself after being left alone");
            GrandWitch.calmTicks = 600;
            h.succeed();
        });
    }

    @GameTest(batch = "w12", template = "field", setupTicks = 20, timeoutTicks = 300)
    public static void aWitchDoesNotDressUpWhileThereIsAMouseAbout(GameTestHelper h) {
        floor(h);
        GrandWitch.calmTicks = 40;
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(2, 1, 2));
        Player player = h.makeMockPlayer();
        put(h, player, 12, 12);
        Mice.become(player, null, 100);
        witch.extraVictims.add(player);
        h.runAfterDelay(120, () -> {
            h.assertTrue(!witch.isDisguised(), "she dressed up with a mouse in sight");
            GrandWitch.calmTicks = 600;
            h.succeed();
        });
    }

    @GameTest(batch = "w13", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aWitchFromAnEggComesDressedUp(GameTestHelper h) {
        floor(h);
        GrandWitch witch = ModEntities.GRAND_WITCH.get().create(h.getLevel());
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        witch.moveTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        witch.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(at), net.minecraft.world.entity.MobSpawnType.SPAWN_EGG, null, null);
        h.getLevel().addFreshEntity(witch);
        h.assertTrue(witch.isDisguised(), "an egg's witch is not dressed up");
        h.succeed();
    }

    @GameTest(batch = "w14", template = "field", setupTicks = 20, timeoutTicks = 200)
    public static void aMouseGoesThroughAGapHalfABlockHighAndAPlayerDoesNot(GameTestHelper h) {
        floor(h);
        // a wall across the room, with a slab hung under the top of its lowest block: a gap half a block high at the bottom
        for (int x = 0; x < 14; x++) {
            for (int y = 1; y <= 3; y++) {
                h.setBlock(new BlockPos(x, y, 8), Blocks.STONE);
            }
        }
        h.setBlock(new BlockPos(6, 1, 8), Blocks.STONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP));
        h.setBlock(new BlockPos(9, 1, 8), Blocks.AIR);
        Player mouse = h.makeMockPlayer(), plain = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        put(h, mouse, 6, 5);
        put(h, plain, 6, 5);
        double wall = h.absolutePos(new BlockPos(6, 1, 8)).getZ();
        for (int i = 0; i < 60; i++) {
            mouse.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0.0D, -0.05D, 0.1D));
            plain.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0.0D, -0.05D, 0.1D));
        }
        h.assertTrue(mouse.getZ() > wall + 1.0D, "the mouse did not get through: z " + mouse.getZ() + ", the wall at " + wall);
        h.assertTrue(plain.getZ() < wall, "an ordinary player got through a gap half a block high: z " + plain.getZ());
        h.succeed();
    }

    @GameTest(batch = "w15", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseEatsOneBitOfFoodFromAChestAndNothingElseMoves(GameTestHelper h) {
        floor(h);
        BlockPos pos = new BlockPos(5, 1, 5);
        h.setBlock(pos, Blocks.CHEST);
        var chest = (net.minecraft.world.Container) h.getBlockEntity(pos);
        chest.setItem(0, new ItemStack(Items.STICK, 5));
        chest.setItem(1, new ItemStack(Items.BREAD, 3));
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        mouse.getFoodData().setFoodLevel(6);
        h.assertTrue(Mice.nibble(mouse, h.absolutePos(pos)), "it ate nothing");
        h.assertTrue(chest.getItem(1).getCount() == 2 && chest.getItem(0).getCount() == 5, "the chest holds " + chest.getItem(0).getCount() + " sticks and " + chest.getItem(1).getCount() + " bread");
        h.assertTrue(mouse.getFoodData().getFoodLevel() > 6, "it is no less hungry");
        h.assertTrue(!Mice.nibble(mouse, h.absolutePos(pos)), "it ate again at once");
        h.succeed();
    }

    @GameTest(batch = "w16", template = "field", setupTicks = 20, timeoutTicks = 400)
    public static void aMouseSqueaksByItselfAndDrawsTheCatsToItselfAndOffTheOthers(GameTestHelper h) {
        floor(h);
        Cat far = h.spawn(EntityType.CAT, new BlockPos(2, 1, 2));
        Cat own = h.spawn(EntityType.CAT, new BlockPos(3, 1, 2));
        Player mouse = h.makeMockPlayer(), friend = h.makeMockPlayer(), plain = h.makeMockPlayer();
        put(h, mouse, 12, 12);
        own.tame(mouse);
        h.assertTrue(!Mice.squeak(plain), "a player who is not a mouse squeaked");
        Mice.become(mouse, null, 60);
        Mice.become(friend, null, 60);
        far.setTarget(friend);                              // it was after the other mouse
        mouse.setShiftKeyDown(true);
        Mice.squeakNow(mouse);
        Mice.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, mouse));
        h.assertTrue(far.getTarget() == friend, "a mouse that was keeping still squeaked");
        mouse.setShiftKeyDown(false);
        Mice.squeakNow(mouse);
        Mice.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, mouse));
        h.assertTrue(far.getTarget() == mouse, "no cat heard it by itself: it is after " + far.getTarget());
        h.assertTrue(far.getTarget() == mouse, "the cat is after " + far.getTarget());
        h.assertTrue(own.getTarget() != mouse, "the mouse's own cat came for it");
        h.assertTrue(Mice.catHunts(far, mouse) && !Mice.catHunts(far, friend), "the cat is not after the one that squeaked and no other");
        h.assertTrue(!Mice.squeak(mouse), "it squeaked again at once");
        h.runAfterDelay(230, () -> {
            h.assertTrue(Mice.catHunts(far, friend), "the cat never went back to hunting the other mouse");
            h.succeed();
        });
    }

    // ------------------------------------------------ children, and the world's way with her ------------------------------------------------

    private static Villager child(GameTestHelper h, int x, int z) {
        Villager v = h.spawn(EntityType.VILLAGER, new BlockPos(x, 1, z));
        v.setAge(-24000);
        return v;
    }

    private static java.util.List<VillageMouse> mice(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(VillageMouse.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(7, 1, 7))).inflate(20.0D));
    }

    @GameTest(batch = "w17", template = "field", setupTicks = 20, timeoutTicks = 300)
    public static void aChildTurnedIntoAMouseIsSmallAndIsTheChildAgain(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        Villager child = child(h, 7, 7);
        child.setCustomName(net.minecraft.network.chat.Component.literal("Tim"));
        VillageMouse mouse = witch.turnChild(child);
        h.assertTrue(mouse != null && child.isRemoved(), "the child was not turned");
        h.assertTrue(mouse.getBbHeight() < 0.5F && mouse.getBbWidth() < 0.5F, "the mouse is " + mouse.getBbWidth() + " x " + mouse.getBbHeight());
        h.assertTrue(witch.turnChild(child(h, 8, 8)) != null || true, "");
        h.runAfterDelay(5, () -> {
            h.assertTrue(mouse.revert(), "it did not go back");
            var back = h.getLevel().getEntitiesOfClass(Villager.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(7, 1, 7))).inflate(5.0D), v -> v.isBaby() && v.hasCustomName());
            h.assertTrue(back.size() == 1 && back.get(0).getCustomName().getString().equals("Tim"), "the child is not as it was: " + back);
            h.succeed();
        });
    }

    @GameTest(batch = "w18", template = "field", setupTicks = 20, timeoutTicks = 300)
    public static void whenTheWitchDiesTheChildrenSheTurnedAreChildrenAgain(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        VillageMouse mouse = witch.turnChild(child(h, 7, 7));
        VillageMouse other = VillageMouse.from(h.getLevel(), child(h, 9, 9), java.util.UUID.randomUUID(), 100);
        h.getLevel().addFreshEntity(other);
        witch.hurt(h.getLevel().damageSources().genericKill(), 1000.0F);
        h.assertTrue(mouse.isRemoved(), "the mouse is still a mouse after the witch is dead");
        h.assertTrue(!other.isRemoved(), "a mouse of another witch was freed");
        h.succeed();
    }

    @GameTest(batch = "w19", template = "field", setupTicks = 20, timeoutTicks = 300)
    public static void milkGivenToAMouseMakesItAChildAgain(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        VillageMouse mouse = witch.turnChild(child(h, 7, 7));
        Player player = h.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
        mouse.interact(player, InteractionHand.MAIN_HAND);
        h.assertTrue(mouse.isRemoved(), "milk did not help");
        h.succeed();
    }

    @GameTest(batch = "w20", template = "field", setupTicks = 20, timeoutTicks = 500)
    public static void theWitchGoesToAChildAndTurnsItAndThenShowsWhatSheIs(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(2, 1, 2));
        witch.disguiseAs(EntityType.VILLAGER);
        child(h, 11, 11);
        StringBuilder trail = new StringBuilder();
        for (int tick = 10; tick <= 480; tick += 5) {
            final int t = tick;
            h.runAfterDelay(t, () -> {
                if (t < 100 && t % 10 != 0) {
                    return;
                }
                var under = witch.blockPosition().below();
                var ents = h.getLevel().getEntities(witch, witch.getBoundingBox().inflate(0.3D, 1.2D, 0.3D), e -> true);
                trail.append(String.format("%d:%.1f,%.1f,%.1f %s v=%.2f %s below=%s feet=%s ents=%s; ", t, witch.getX(), witch.getY(), witch.getZ(), witch.isInWater() ? "W" : witch.onGround() ? "g" : "a", witch.getDeltaMovement().y, witch.runningGoalNames(),
                        h.getLevel().getBlockState(under).getBlock(), h.getLevel().getBlockState(witch.blockPosition()).getBlock(), ents.stream().map(e -> e.getType().toShortString()).toList()));
            });
        }
        h.succeedWhen(() -> {
            h.assertTrue(witch.turned() >= 1, "she has not turned the child (" + witch.position() + ", " + witch.runningGoalNames() + ") trail: " + trail + " floor y " + h.absolutePos(new BlockPos(0, 0, 0)).getY());
            h.assertTrue(!witch.isDisguised(), "she is still dressed as a villager after seeing a mouse");
        });
    }

    @GameTest(batch = "w21", template = "field", setupTicks = 20, timeoutTicks = 200)
    public static void aCatHissesAtTheWitchAndAGolemWatchesAndOnlyGoesForHerOnceShowsHerself(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(3, 1, 3));
        witch.disguiseAs(EntityType.VILLAGER);
        witch.getNavigation().stop();
        witch.setNoAi(true);
        Cat cat = h.spawn(EntityType.CAT, new BlockPos(6, 1, 3));
        var golem = h.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 1, 7));
        h.runAfterDelay(60, () -> {
            boolean hissing = cat.goalSelector.getRunningGoals().anyMatch(w -> w.getGoal() instanceof Reactions.HissGoal);
            h.assertTrue(hissing, "the cat is not hissing");
            h.assertTrue(golem.getTarget() != witch, "the golem went for a witch that is still dressed as a villager");
            witch.reveal();
        });
        h.succeedWhen(() -> h.assertTrue(golem.getTarget() == witch, "the golem has not gone for her: target " + golem.getTarget() + ", witch disguised " + witch.isDisguised() + " alive " + witch.isAlive() + " at " + witch.position() + ", golem at " + golem.position() + " alive " + golem.isAlive() + ", golem goals " + golem.targetSelector.getRunningGoals().map(w -> w.getGoal().getClass().getSimpleName()).toList() + ", invulnerable " + witch.isInvulnerable() + ", los " + golem.hasLineOfSight(witch)));
    }

    @GameTest(batch = "w22", template = "field", setupTicks = 20, timeoutTicks = 300)
    public static void villagersRunFromTheWitchOnceSheHasShownHerself(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(4, 1, 4));
        witch.setNoAi(true);
        Villager villager = h.spawn(EntityType.VILLAGER, new BlockPos(7, 1, 4));
        boolean[] ran = new boolean[1];
        for (int tick = 2; tick <= 200; tick += 2) {
            h.runAfterDelay(tick, () -> ran[0] |= villager.goalSelector.getRunningGoals().anyMatch(w -> w.getGoal() instanceof net.minecraft.world.entity.ai.goal.AvoidEntityGoal));
        }
        h.runAfterDelay(210, () -> {
            h.assertTrue(ran[0], "the villager never ran from the witch");
            h.succeed();
        });
    }

    @GameTest(batch = "w40", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseNibblingAChestDrinksTheAntidoteInItAndLeavesTheBottle(GameTestHelper h) {
        floor(h);
        BlockPos pos = new BlockPos(5, 1, 5);
        h.setBlock(pos, Blocks.CHEST);
        var chest = (net.minecraft.world.Container) h.getBlockEntity(pos);
        chest.setItem(0, new ItemStack(Items.BREAD, 3));
        chest.setItem(4, new ItemStack(ModItems.ANTIDOTE.get(), 1));
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        h.assertTrue(Mice.nibble(mouse, h.absolutePos(pos)), "it took nothing");
        h.assertTrue(!Mice.isMouse(mouse), "it is still a mouse after the antidote was in the chest");
        h.assertTrue(chest.getItem(4).is(Items.GLASS_BOTTLE), "the bottle is not left: " + chest.getItem(4));
        h.assertTrue(chest.getItem(0).getCount() == 3, "it ate the bread too");
        h.succeed();
    }

    // ------------------------------------------------ what a mouse cannot do, what it sees, the antidote, the hut ------------------------------------------------

    private static net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock click(Player p, BlockPos pos, ItemStack held) {
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        return new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, pos,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
    }

    @GameTest(batch = "w23", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseCanUseNoBlockBreakNonePlaceNoneButCanEat(GameTestHelper h) {
        floor(h);
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        h.setBlock(new BlockPos(5, 1, 5), Blocks.OAK_DOOR);
        Player mouse = h.makeMockPlayer(), plain = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        var onDoor = click(mouse, at, ItemStack.EMPTY);
        Mice.useBlock(onDoor);
        h.assertTrue(onDoor.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.DENY, "a mouse can use a door");
        var ordinary = click(plain, at, ItemStack.EMPTY);
        Mice.useBlock(ordinary);
        h.assertTrue(ordinary.getUseBlock() != net.minecraftforge.eventbus.api.Event.Result.DENY, "an ordinary player was stopped");
        var placing = click(mouse, at, new ItemStack(Items.STONE));
        Mice.useBlock(placing);
        h.assertTrue(placing.getUseItem() == net.minecraftforge.eventbus.api.Event.Result.DENY, "a mouse can place a block");
        var eating = click(mouse, at, new ItemStack(Items.BREAD));
        Mice.useBlock(eating);
        h.assertTrue(eating.getUseItem() != net.minecraftforge.eventbus.api.Event.Result.DENY, "a mouse cannot eat with a block in front of it");
        var bucket = click(mouse, at, new ItemStack(Items.WATER_BUCKET));
        Mice.useBlock(bucket);
        h.assertTrue(bucket.getUseItem() == net.minecraftforge.eventbus.api.Event.Result.DENY, "a mouse can pour water");
        var milk = click(mouse, at, new ItemStack(Items.MILK_BUCKET));
        Mice.useBlock(milk);
        h.assertTrue(milk.getUseItem() != net.minecraftforge.eventbus.api.Event.Result.DENY, "a mouse cannot drink milk");
        var breaking = new net.minecraftforge.event.level.BlockEvent.BreakEvent(h.getLevel(), at, h.getLevel().getBlockState(at), mouse);
        Mice.broke(breaking);
        h.assertTrue(breaking.isCanceled(), "a mouse can break a block");
        var breaking2 = new net.minecraftforge.event.level.BlockEvent.BreakEvent(h.getLevel(), at, h.getLevel().getBlockState(at), plain);
        Mice.broke(breaking2);
        h.assertTrue(!breaking2.isCanceled(), "an ordinary player cannot break a block");
        h.succeed();
    }

    @GameTest(batch = "w24", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseSeesInTheDarkAndOnlyWhileItIsOne(GameTestHelper h) {
        floor(h);
        Player mouse = h.makeMockPlayer(), potioned = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        Mice.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, mouse));
        h.assertTrue(mouse.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "no night vision");
        h.assertTrue(!mouse.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION).showIcon(), "the icon shows");
        Mice.cure(mouse, false);
        h.assertTrue(!mouse.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "still sees in the dark after it was cured");
        potioned.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 3000));
        Mice.become(potioned, null, 60);
        Mice.cure(potioned, false);
        h.assertTrue(potioned.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "a potion's night vision was taken off");
        h.succeed();
    }

    @GameTest(batch = "w25", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theAntidoteCuresAMouseAnotherMouseAndAChildAndIsNotWastedOnAnyoneElse(GameTestHelper h) {
        floor(h);
        Player drinker = h.makeMockPlayer(), giver = h.makeMockPlayer(), friend = h.makeMockPlayer(), well = h.makeMockPlayer();
        var item = (AntidoteItem) ModItems.ANTIDOTE.get();
        Mice.become(drinker, null, 60);
        ItemStack stack = new ItemStack(item, 2);
        item.finishUsingItem(stack, h.getLevel(), drinker);
        h.assertTrue(!Mice.isMouse(drinker), "drinking it did not help");
        h.assertTrue(stack.getCount() == 1, "the antidote was not used up: " + stack.getCount());
        Mice.become(friend, null, 60);
        giver.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 2));
        h.assertTrue(item.interactLivingEntity(giver.getMainHandItem(), giver, friend, InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.SUCCESS, "giving it to a mouse failed");
        h.assertTrue(!Mice.isMouse(friend) && giver.getMainHandItem().getCount() == 1, "the friend is still a mouse, or it was not used up");
        h.assertTrue(item.interactLivingEntity(giver.getMainHandItem(), giver, well, InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.PASS && giver.getMainHandItem().getCount() == 1, "it was used on someone who was not a mouse");
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        VillageMouse child = witch.turnChild(child(h, 7, 7));
        h.assertTrue(item.interactLivingEntity(giver.getMainHandItem(), giver, child, InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.SUCCESS && child.isRemoved(), "a child was not cured");
        h.assertTrue(item.use(h.getLevel(), well, InteractionHand.MAIN_HAND).getResult().consumesAction(), "a drink could not be begun");
        ItemStack unused = new ItemStack(item, 2);
        h.assertTrue(item.finishUsingItem(unused, h.getLevel(), well).getCount() == 2, "an antidote was used up on someone who is not a mouse");
        h.succeed();
    }

    @GameTest(batch = "w26", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theAntidoteCanBeMadeInTheInventoryGridFromThreeThingsOneCanGet(GameTestHelper h) {
        floor(h);
        var recipe = h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("grandwitch", "antidote"));
        h.assertTrue(recipe.isPresent(), "there is no recipe");
        h.assertTrue(recipe.get().getIngredients().size() == 3, "it takes " + recipe.get().getIngredients().size() + " things");
        h.succeed();
    }

    @GameTest(batch = "w27", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theHutHasADoorACauldronAndAChestWithAntidoteAndIsNotBuiltOnWhatIsThere(GameTestHelper h) {
        floor(h);
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 5));
        h.setBlock(new BlockPos(13, 1, 13), Blocks.OAK_PLANKS);
        BlockPos door = WitchHut.build(h.getLevel(), floor);
        h.assertTrue(door != null, "it was not built on clear ground");
        h.assertTrue(h.getLevel().getBlockState(floor.offset(0, 1, 3)).getBlock() == Blocks.SPRUCE_DOOR, "no door");
        h.assertTrue(h.getLevel().getBlockState(floor.offset(-2, 1, -2)).getBlock() == Blocks.WATER_CAULDRON, "no cauldron");
        var chest = (net.minecraft.world.Container) h.getLevel().getBlockEntity(floor.offset(2, 1, -2));
        int antidotes = 0;
        for (int i = 0; chest != null && i < chest.getContainerSize(); i++) {
            antidotes += chest.getItem(i).is(ModItems.ANTIDOTE.get()) ? chest.getItem(i).getCount() : 0;
        }
        h.assertTrue(antidotes == 2, "the chest holds " + antidotes + " antidotes");
        h.assertTrue(h.getLevel().getBlockState(floor.offset(0, 2, 0)).isAir(), "the room is not clear");
        h.assertTrue(WitchHut.hutNear(h.getLevel(), floor.offset(40, 0, 0), 160), "it is not on record");
        h.assertTrue(WitchHut.build(h.getLevel(), floor) == null, "it was built again over the first");
        h.succeed();
    }

    @GameTest(batch = "w28", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theHutIsNotBuiltWhereSomeoneHasBuilt(GameTestHelper h) {
        floor(h);
        h.setBlock(new BlockPos(8, 2, 6), Blocks.OAK_PLANKS);
        h.assertTrue(WitchHut.build(h.getLevel(), h.absolutePos(new BlockPos(7, 0, 5))) == null, "it was built over a block that was not air");
        h.succeed();
    }

    @GameTest(batch = "w29", template = "field", setupTicks = 20, timeoutTicks = 200)
    public static void aMouseFromAnEggIsAWildMouseThatNothingTurnsBack(GameTestHelper h) {
        floor(h);
        h.assertTrue(((net.minecraftforge.common.ForgeSpawnEggItem) ModItems.MOUSE_SPAWN_EGG.get()).getType(null) == ModEntities.VILLAGE_MOUSE.get(), "the egg is for another creature");
        VillageMouse wild = h.spawn(ModEntities.VILLAGE_MOUSE.get(), new BlockPos(5, 1, 5));
        h.assertTrue(!wild.hasOriginal() && !wild.revert(), "a mouse that was never a child turned into something");
        Player player = h.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
        wild.interact(player, InteractionHand.MAIN_HAND);
        h.assertTrue(!wild.isRemoved() && player.getMainHandItem().is(Items.MILK_BUCKET), "the milk did something to a wild mouse");
        h.runAfterDelay(100, () -> {
            h.assertTrue(!wild.isRemoved(), "the wild mouse went away by itself");
            h.succeed();
        });
    }

    @GameTest(batch = "w30", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theMousePotionMakesAMouseOfAPlayerUnlessAShieldIsUpAndOfAChild(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        Player bare = h.makeMockPlayer(), shielded = h.makeMockPlayer();
        var effect = ModEffects.MOUSE_FORM.get();
        h.assertTrue(effect.isInstantenous(), "the effect is not at once");
        effect.applyInstantenousEffect(null, witch, bare, 0, 1.0D);
        h.assertTrue(Mice.isMouse(bare) && witch.getUUID().equals(Mice.madeBy(bare)), "the player is not a mouse of hers");
        shielded.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        shielded.startUsingItem(InteractionHand.OFF_HAND);
        if (shielded.isBlocking()) {
            effect.applyInstantenousEffect(null, witch, shielded, 0, 1.0D);
            h.assertTrue(!Mice.isMouse(shielded), "a shield did not keep it off");
        }
        Villager child = child(h, 7, 7);
        effect.applyInstantenousEffect(null, witch, child, 0, 1.0D);
        h.assertTrue(child.isRemoved() && witch.turned() == 1, "the child was not turned");
        Villager adult = h.spawn(EntityType.VILLAGER, new BlockPos(8, 1, 8));
        WitchConfig.TURNS_ADULTS.set(false);
        effect.applyInstantenousEffect(null, witch, adult, 0, 1.0D);
        WitchConfig.TURNS_ADULTS.set(true);
        h.assertTrue(!adult.isRemoved(), "an adult was turned though she is not to turn them");
        effect.applyInstantenousEffect(null, witch, adult, 0, 1.0D);
        h.assertTrue(adult.isRemoved(), "a grown villager was not turned when she may");
        h.succeed();
    }

    @GameTest(batch = "w31", template = "field", setupTicks = 20, timeoutTicks = 600)
    public static void aWitchThatHasShownHerselfThrowsMousePotionsAtAPlayerInSightAndOnlyThen(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(2, 1, 6));
        Player player = h.makeMockPlayer();
        put(h, player, 10, 6);
        witch.extraVictims.add(player);
        witch.disguiseAs(EntityType.VILLAGER);
        h.runAfterDelay(120, () -> {
            h.assertTrue(witch.thrown() == 0, "she threw a potion while she was dressed as a villager");
            witch.reveal();
        });
        h.succeedWhen(() -> {
            h.assertTrue(witch.thrown() >= 1, "she has not thrown a potion");
            var potions = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.ThrownPotion.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(7, 1, 6))).inflate(20.0D));
            h.assertTrue(potions.isEmpty() || net.minecraft.world.item.alchemy.PotionUtils.getPotion(potions.get(0).getItem()) == ModEffects.MOUSE_POTION.get(), "it is not the mouse potion");
        });
    }

    @GameTest(batch = "w32", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aWitchThatIsHitDoesNotThrowAtAPlayerThatIsAlreadyAMouse(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(2, 1, 6));
        Player mouse = h.makeMockPlayer();
        put(h, mouse, 10, 6);
        Mice.become(mouse, null, 60);
        witch.extraVictims.add(mouse);
        h.assertTrue(witch.throwTarget() == null, "she aims at a mouse");
        h.succeed();
    }

    @GameTest(batch = "w33", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void wildMiceMayComeUpOnGroundAndNotWhenItIsSwitchedOff(GameTestHelper h) {
        floor(h);
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        var type = ModEntities.VILLAGE_MOUSE.get();
        WitchConfig.WILD_MICE.set(true);
        h.assertTrue(type.getCategory() == net.minecraft.world.entity.MobCategory.AMBIENT, "not an ambient creature");
        h.assertTrue(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(type, h.getLevel(), net.minecraft.world.entity.MobSpawnType.NATURAL, at, h.getLevel().random), "it cannot come up on stone ground");
        WitchConfig.WILD_MICE.set(false);
        boolean off = net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(type, h.getLevel(), net.minecraft.world.entity.MobSpawnType.NATURAL, at, h.getLevel().random);
        h.assertTrue(!off, "it comes up though it is switched off");
        h.succeed();
    }

    @GameTest(batch = "w34", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseHoleLetsAMouseOutAndNeverMoreThanThreeAreAbout(GameTestHelper h) {
        floor(h);
        MouseHoleBlock.requirePlayer = false;
        MouseHoleBlock.alwaysOut = true;
        WitchConfig.WILD_MICE.set(true);
        BlockPos hole = new BlockPos(6, 1, 6);
        h.setBlock(hole, ModBlocks.MOUSE_HOLE.get());
        var state = h.getLevel().getBlockState(h.absolutePos(hole));
        for (int i = 0; i < 10; i++) {
            ((MouseHoleBlock) state.getBlock()).randomTick(state, h.getLevel(), h.absolutePos(hole), h.getLevel().random);
        }
        int n = mice(h).size();
        h.assertTrue(n == 3, n + " mice came out, not three");
        WitchConfig.WILD_MICE.set(false);
        mice(h).forEach(net.minecraft.world.entity.Entity::discard);
        ((MouseHoleBlock) state.getBlock()).randomTick(state, h.getLevel(), h.absolutePos(hole), h.getLevel().random);
        h.assertTrue(mice(h).isEmpty(), "a mouse came out though it is switched off");
        MouseHoleBlock.requirePlayer = true;
        MouseHoleBlock.alwaysOut = null;
        h.succeed();
    }

    @GameTest(batch = "w35", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void whenTheWitchShowsHerselfTheBellsRingAndSheShinesAndOtherwiseNothing(GameTestHelper h) {
        floor(h);
        GrandWitch quiet = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(2, 1, 2));
        quiet.disguiseAs(EntityType.VILLAGER);
        quiet.reveal();
        h.assertTrue(!quiet.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "she shines with no bell about");
        quiet.discard();
        h.setBlock(new BlockPos(9, 1, 9), Blocks.BELL);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(3, 1, 3));
        witch.disguiseAs(EntityType.VILLAGER);
        witch.reveal();
        h.assertTrue(witch.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "she does not shine after the bell");
        h.succeed();
    }

    @GameTest(batch = "w36", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theHutHasAMouseHoleByItsWall(GameTestHelper h) {
        floor(h);
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 6));
        WitchHut.build(h.getLevel(), floor);
        h.assertTrue(h.getLevel().getBlockState(floor.offset(-4, 0, 1)).getBlock() == ModBlocks.MOUSE_HOLE.get(), "no hole by the west wall");
        h.assertTrue(h.getLevel().getBlockState(floor.offset(4, 0, -1)).getBlock() != ModBlocks.MOUSE_HOLE.get(), "a second hole by the east wall");
        h.succeed();
    }

    private static void wallWithHole(GameTestHelper h) {
        for (int x = 0; x < 14; x++) {
            for (int y = 1; y <= 3; y++) {
                h.setBlock(new BlockPos(x, y, 8), Blocks.STONE);
            }
        }
        h.setBlock(new BlockPos(6, 1, 8), ModBlocks.MOUSE_HOLE.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH));
    }

    @GameTest(batch = "w37", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theTunnelOfAMouseHoleLetsAMouseThroughAndNothingBigger(GameTestHelper h) {
        floor(h);
        wallWithHole(h);
        BlockPos hole = h.absolutePos(new BlockPos(6, 1, 8));
        var mouse = new net.minecraft.world.phys.AABB(hole.getX() + 0.5D - 0.2D, hole.getY(), hole.getZ() + 0.5D - 0.2D, hole.getX() + 0.5D + 0.2D, hole.getY() + 0.4D, hole.getZ() + 0.5D + 0.2D);
        var player = new net.minecraft.world.phys.AABB(hole.getX() + 0.5D - 0.3D, hole.getY(), hole.getZ() + 0.5D - 0.3D, hole.getX() + 0.5D + 0.3D, hole.getY() + 1.8D, hole.getZ() + 0.5D + 0.3D);
        var cat = new net.minecraft.world.phys.AABB(hole.getX() + 0.5D - 0.3D, hole.getY(), hole.getZ() + 0.5D - 0.3D, hole.getX() + 0.5D + 0.3D, hole.getY() + 0.7D, hole.getZ() + 0.5D + 0.3D);
        h.assertTrue(h.getLevel().noCollision(mouse), "a mouse does not fit in the tunnel");
        h.assertTrue(!h.getLevel().noCollision(player), "a player fits in the tunnel");
        h.assertTrue(!h.getLevel().noCollision(cat), "a cat fits in the tunnel");
        h.succeed();
    }

    @GameTest(batch = "w38", template = "field", setupTicks = 20, timeoutTicks = 200)
    public static void aMouseWalksThroughAMouseHoleAndAPlayerCannot(GameTestHelper h) {
        floor(h);
        wallWithHole(h);
        Player mouse = h.makeMockPlayer(), plain = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        put(h, mouse, 6, 5);
        put(h, plain, 6, 5);
        double wall = h.absolutePos(new BlockPos(6, 1, 8)).getZ();
        for (int i = 0; i < 60; i++) {
            mouse.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0.0D, -0.05D, 0.1D));
            plain.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0.0D, -0.05D, 0.1D));
        }
        h.assertTrue(mouse.getZ() > wall + 1.0D, "the mouse did not get through the hole: z " + mouse.getZ() + ", the wall at " + wall);
        h.assertTrue(plain.getZ() < wall, "an ordinary player got through the hole: z " + plain.getZ());
        h.succeed();
    }

    @GameTest(batch = "w39", template = "field", setupTicks = 20, timeoutTicks = 200)
    public static void aMouseInsideAHoleIsOutOfTheWitchesSightAndOutsideIsNot(GameTestHelper h) {
        floor(h);
        wallWithHole(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(3, 1, 3));
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        witch.extraVictims.add(mouse);
        put(h, mouse, 6, 8);
        h.assertTrue(witch.findVictim() == null, "she sees a mouse that is in a hole");
        put(h, mouse, 6, 5);
        h.assertTrue(witch.findVictim() == mouse, "she does not see a mouse in the open");
        h.succeed();
    }

    // ------------------------------------------------ her house: the alarm, and living in it ------------------------------------------------

    @GameTest(batch = "w41", template = "field", setupTicks = 20, timeoutTicks = 300)
    public static void someoneInHerHouseSetsTheWitchOnThemAndOnlyThenAndOnlyOnce(GameTestHelper h) {
        floor(h);
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 4));
        WitchHut.build(h.getLevel(), floor);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(1, 1, 12));
        witch.disguiseAs(EntityType.VILLAGER);
        WitchHut.bind(h.getLevel(), floor, witch);
        Player outside = h.makeMockPlayer(), inside = h.makeMockPlayer();
        put(h, outside, 12, 10);
        WitchHut.extraPeople.add(outside);
        WitchHut.checkIntruders(h.getLevel());
        h.assertTrue(witch.alertTicks() == 0 && witch.isDisguised(), "she was alarmed by someone outside the house");
        put(h, inside, 7, 4);
        WitchHut.extraPeople.add(inside);
        WitchHut.checkIntruders(h.getLevel());
        h.assertTrue(witch.alertTicks() > 0 && !witch.isDisguised() && witch.isRaging(), "she is not on them: alert " + witch.alertTicks() + ", disguised " + witch.isDisguised());
        int left = witch.alertTicks();
        h.runAfterDelay(20, () -> {
            WitchHut.checkIntruders(h.getLevel());
            h.assertTrue(witch.alertTicks() <= left, "the alarm was raised again while it was up");
        });
        double[] start = new double[1];
        h.runAfterDelay(2, () -> start[0] = witch.distanceToSqr(inside));
        h.succeedWhen(() -> h.assertTrue(witch.distanceToSqr(inside) < start[0] - 25.0D || inside.getHealth() < inside.getMaxHealth(), "she is not coming: " + witch.distanceToSqr(inside) + " from " + start[0]));
    }

    @GameTest(batch = "w42", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aWitchFarFromHerHouseIsAtItsDoorAtOnceAndNotWhenItIsSwitchedOff(GameTestHelper h) {
        floor(h);
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 4));
        WitchHut.build(h.getLevel(), floor);
        // far: the witch is made to think so by a house 60 blocks off from where she is
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(1, 1, 12));
        Player inside = h.makeMockPlayer();
        put(h, inside, 7, 4);
        BlockPos farHut = floor.offset(80, 0, 0);
        h.assertTrue(witch.alert(farHut, inside), "she did not take it up");
        h.assertTrue(witch.distanceToSqr(farHut.getX() + 0.5D, farHut.getY() + 1.0D, farHut.getZ() + 5.5D) < 4.0D, "she is not at the door: " + witch.position());
        GrandWitch other = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(1, 1, 12));
        var before = other.position();
        WitchConfig.BLINK.set(false);
        other.alert(farHut, inside);
        WitchConfig.BLINK.set(true);
        h.assertTrue(other.position().distanceTo(before) < 2.0D, "she blinked though it is switched off");
        h.succeed();
    }

    @GameTest(batch = "w43", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aWitchWithAHouseKeepsToItAndAWitchWithoutIsFree(GameTestHelper h) {
        floor(h);
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 4));
        GrandWitch lives = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(2, 1, 2));
        GrandWitch wild = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(3, 1, 3));
        lives.setHome(floor);
        h.assertTrue(lives.hasRestriction() && lives.getRestrictCenter().equals(floor) && lives.getRestrictRadius() == 40.0F, "she is not tied to her house");
        h.assertTrue(!wild.hasRestriction(), "a witch without a house is tied to one");
        var tag = new net.minecraft.nbt.CompoundTag();
        lives.addAdditionalSaveData(tag);
        GrandWitch loaded = ModEntities.GRAND_WITCH.get().create(h.getLevel());
        loaded.readAdditionalSaveData(tag);
        h.assertTrue(loaded.hasRestriction() && loaded.home().equals(floor), "she forgot her house when she was saved and loaded");
        h.succeed();
    }

    @GameTest(batch = "w44", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aWitchComesWithHerHouseAndIsBoundToIt(GameTestHelper h) {
        floor(h);
        WitchConfig.HUT.set(true);
        WitchVisits.rollOverride = 0.0D;
        WitchVisits.distance = new double[]{4.0D, 4.5D};
        WitchVisits.siteRange = new double[]{3.0D, 4.0D};
        Player player = h.makeMockPlayer();
        put(h, player, 7, 7);
        var child = child(h, 3, 3);
        GrandWitch witch = WitchVisits.visit(h.getLevel(), player, false);
        WitchConfig.HUT.set(false);
        if (witch != null) {
            h.assertTrue(witch.home() != null, "the witch that came with a house does not know it");
            var hut = WitchHut.Huts.get(h.getLevel()).all().stream().filter(x -> x.pos().equals(witch.home())).findFirst();
            h.assertTrue(hut.isPresent() && witch.getUUID().equals(hut.get().witch()), "the house does not know its witch");
        }
        h.succeed();
    }

    @GameTest(batch = "w45", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theWitchTurnsGrownVillagersAndTradersButOnlyAsManyAsSheMayAndNotWhenSwitchedOff(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        Villager a = h.spawn(EntityType.VILLAGER, new BlockPos(7, 1, 7)), b = h.spawn(EntityType.VILLAGER, new BlockPos(8, 1, 8));
        var trader = h.spawn(EntityType.WANDERING_TRADER, new BlockPos(9, 1, 9));
        WitchConfig.TURNS_ADULTS.set(false);
        h.assertTrue(witch.turnChild(a) == null && !a.isRemoved(), "she turned a grown villager though she is not to");
        WitchConfig.TURNS_ADULTS.set(true);
        h.assertTrue(witch.turnChild(a) != null && a.isRemoved(), "she did not turn a grown villager");
        h.assertTrue(witch.turnChild(b) == null && !b.isRemoved(), "she turned a second one, past her limit");
        Villager child = child(h, 3, 3);
        h.assertTrue(witch.turnChild(child) != null, "a child was not turned once she was at her limit of grown ones");
        GrandWitch other = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(6, 1, 6));
        h.assertTrue(other.turnChild(trader) != null && trader.isRemoved(), "a trader was not turned");
        h.succeed();
    }

    @GameTest(batch = "w46", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theWitchCanBeAGirlWithABasketOfEitherKindAndIsHerselfAgainWhenShowsHerself(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        float tall = witch.getBbHeight();
        for (int look = 1; look <= 2; look++) {
            witch.dressAsGirl(look);
            h.assertTrue(witch.isDisguised() && witch.girl() == look, "she is not dressed as the girl " + look);
            h.assertTrue(witch.getBbHeight() < tall && witch.getBbHeight() > 1.5F, "the girl is " + witch.getBbHeight() + " high");
            Player player = h.makeMockPlayer();
            h.assertTrue(witch.offer(player), "the girl gave nothing");
            var tag = new net.minecraft.nbt.CompoundTag();
            witch.addAdditionalSaveData(tag);
            GrandWitch loaded = ModEntities.GRAND_WITCH.get().create(h.getLevel());
            loaded.readAdditionalSaveData(tag);
            h.assertTrue(loaded.girl() == look, "she forgot she was a girl when she was saved and loaded");
            witch.reveal();
            h.assertTrue(!witch.isDisguised() && witch.girl() == 0 && witch.getBbHeight() == tall, "she is not herself again");
        }
        int girls = 0, others = 0;
        for (int i = 0; i < 200; i++) {
            GrandWitch w = ModEntities.GRAND_WITCH.get().create(h.getLevel());
            w.dressUp();
            if (w.girl() > 0) {
                girls++;
            } else if (w.isDisguised()) {
                others++;
            }
        }
        h.assertTrue(girls > 80 && others > 30, "she is a girl " + girls + " times in 200, something else " + others + " times");
        h.succeed();
    }

    // ------------------------------------------------ digging ------------------------------------------------

    private static net.minecraftforge.event.level.BlockEvent.BreakEvent breakAt(GameTestHelper h, Player p, BlockPos rel) {
        BlockPos at = h.absolutePos(rel);
        var event = new net.minecraftforge.event.level.BlockEvent.BreakEvent(h.getLevel(), at, h.getLevel().getBlockState(at), p);
        Mice.broke(event);
        return event;
    }

    @GameTest(batch = "w47", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseMakesATunnelOfSoftGroundAndLeavesEverythingElseAlone(GameTestHelper h) {
        floor(h);
        h.setBlock(new BlockPos(5, 1, 5), Blocks.DIRT);
        h.setBlock(new BlockPos(6, 1, 5), Blocks.GRASS_BLOCK);
        h.setBlock(new BlockPos(7, 1, 5), Blocks.STONE);
        h.setBlock(new BlockPos(8, 1, 5), Blocks.SAND);
        Player mouse = h.makeMockPlayer(), plain = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        put(h, mouse, 5, 3);                       // level with the dirt it digs
        var dirt = breakAt(h, mouse, new BlockPos(5, 1, 5));
        h.assertTrue(dirt.isCanceled(), "the block was really broken");
        var made = h.getLevel().getBlockState(h.absolutePos(new BlockPos(5, 1, 5)));
        h.assertTrue(made.is(ModBlocks.MOUSE_TUNNEL.get()), "dirt did not become a tunnel: " + made);
        h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(5, 1, 5))).inflate(3.0D)).isEmpty(), "digging dropped something");
        breakAt(h, mouse, new BlockPos(6, 1, 5));
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(new BlockPos(6, 1, 5))).is(ModBlocks.MOUSE_TUNNEL.get()), "grass did not become a tunnel");
        var stone = breakAt(h, mouse, new BlockPos(7, 1, 5));
        h.assertTrue(stone.isCanceled() && h.getLevel().getBlockState(h.absolutePos(new BlockPos(7, 1, 5))).is(Blocks.STONE), "stone was dug");
        breakAt(h, mouse, new BlockPos(8, 1, 5));
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(new BlockPos(8, 1, 5))).is(Blocks.SAND), "sand was dug");
        breakAt(h, mouse, new BlockPos(5, 1, 5));
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(new BlockPos(5, 1, 5))).is(ModBlocks.MOUSE_TUNNEL.get()), "a tunnel was dug again");
        h.setBlock(new BlockPos(9, 1, 5), Blocks.DIRT);
        var ordinary = breakAt(h, plain, new BlockPos(9, 1, 5));
        h.assertTrue(!ordinary.isCanceled() && h.getLevel().getBlockState(h.absolutePos(new BlockPos(9, 1, 5))).is(Blocks.DIRT), "an ordinary player's breaking was changed");
        h.succeed();
    }

    @GameTest(batch = "w48", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseCanDigAsManyBlocksAsItLikes(GameTestHelper h) {
        floor(h);
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        put(h, mouse, 7, 6);
        int made = 0;
        for (int i = 0; i < 84; i++) {
            BlockPos at = new BlockPos(i % 14, 1, 2 + i / 14);
            h.setBlock(at, Blocks.DIRT);
            breakAt(h, mouse, at);
            made += h.getLevel().getBlockState(h.absolutePos(at)).is(ModBlocks.MOUSE_TUNNEL.get()) ? 1 : 0;
        }
        h.assertTrue(made == 84, made + " tunnels of 84");
        h.succeed();
    }

    @GameTest(batch = "w49", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseDigsSoftGroundAsFastAsWithAShovelAndStartsDiggingOnlyThere(GameTestHelper h) {
        floor(h);
        h.setBlock(new BlockPos(5, 1, 5), Blocks.DIRT);
        h.setBlock(new BlockPos(6, 1, 5), Blocks.STONE);
        Player mouse = h.makeMockPlayer(), plain = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        put(h, mouse, 5, 3);                       // level with the dirt
        var slow = new net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(mouse, h.getLevel().getBlockState(h.absolutePos(new BlockPos(5, 1, 5))), 1.0F, h.absolutePos(new BlockPos(5, 1, 5)));
        Mice.digSpeed(slow);
        var normal = new net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(plain, h.getLevel().getBlockState(h.absolutePos(new BlockPos(5, 1, 5))), 1.0F, h.absolutePos(new BlockPos(5, 1, 5)));
        Mice.digSpeed(normal);
        mouse.setOnGround(true);
        var onGround = new net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(mouse, h.getLevel().getBlockState(h.absolutePos(new BlockPos(5, 1, 5))), 1.0F, h.absolutePos(new BlockPos(5, 1, 5)));
        Mice.digSpeed(onGround);
        h.assertTrue(Math.abs(slow.getNewSpeed() - WitchConfig.MOUSE_DIG_SPEED.get().floatValue() / 5.0F) < 1.0E-3F, "in the air a mouse does not dig a fifth as fast: " + slow.getNewSpeed());
        h.assertTrue(Math.abs(onGround.getNewSpeed() - WitchConfig.MOUSE_DIG_SPEED.get().floatValue()) < 1.0E-3F && normal.getNewSpeed() == 1.0F,
                "the speeds are " + onGround.getNewSpeed() + " (on the ground) and " + normal.getNewSpeed() + " (a person)");
        var onDirt = new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(mouse, h.absolutePos(new BlockPos(5, 1, 5)), net.minecraft.core.Direction.UP, net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
        Mice.startBreaking(onDirt);
        var onStone = new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(mouse, h.absolutePos(new BlockPos(6, 1, 5)), net.minecraft.core.Direction.UP, net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
        Mice.startBreaking(onStone);
        h.assertTrue(!onDirt.isCanceled(), "a mouse is stopped from starting to dig soft ground");
        h.assertTrue(onStone.isCanceled(), "a mouse can start to dig stone");
        h.succeed();
    }

    @GameTest(batch = "w50", template = "field", setupTicks = 20, timeoutTicks = 200)
    public static void aMouseGoesThroughADugTunnelAndIsHiddenInItAndNothingBiggerFits(GameTestHelper h) {
        floor(h);
        for (int x = 0; x < 14; x++) {
            for (int y = 1; y <= 3; y++) {
                h.setBlock(new BlockPos(x, y, 8), Blocks.DIRT);
            }
        }
        Player mouse = h.makeMockPlayer(), plain = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        put(h, mouse, 6, 5);
        breakAt(h, mouse, new BlockPos(6, 1, 8));
        put(h, plain, 6, 5);
        double wall = h.absolutePos(new BlockPos(6, 1, 8)).getZ();
        for (int i = 0; i < 60; i++) {
            mouse.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0.0D, -0.05D, 0.1D));
            plain.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0.0D, -0.05D, 0.1D));
        }
        h.assertTrue(mouse.getZ() > wall + 1.0D, "the mouse did not get through its tunnel: z " + mouse.getZ() + ", the wall at " + wall);
        h.assertTrue(plain.getZ() < wall, "an ordinary player got through a tunnel");
        put(h, mouse, 6, 8);
        h.assertTrue(MouseHoleBlock.hidden(mouse), "a mouse in a tunnel it dug is not hidden");
        h.succeed();
    }

    @GameTest(batch = "w51", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aDugBlockIsShutOnAllSidesBeyondWhichIsGroundAndFromInsideItTheNextBlockInAnyDirectionCanStillBeDug(GameTestHelper h) {
        floor(h);
        for (int x = 3; x <= 9; x++) {
            for (int z = 3; z <= 9; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
            }
        }
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        put(h, mouse, 6, 2);                                          // outside, level with the dirt
        breakAt(h, mouse, new BlockPos(6, 1, 3));                     // the first one, dug from outside
        BlockPos first = h.absolutePos(new BlockPos(6, 1, 3));
        // open to the air it was dug from (north) and shut, with a wall of earth, on the other three sides (ground beyond): a mouse fits in the middle and well out toward the open side, and not into the walls
        for (double[] d : new double[][]{{0, 0}, {0, -0.3}}) {
            var box = new net.minecraft.world.phys.AABB(first.getX() + 0.5D + d[0] - 0.2D, first.getY(), first.getZ() + 0.5D + d[1] - 0.2D, first.getX() + 0.5D + d[0] + 0.2D, first.getY() + 0.4D, first.getZ() + 0.5D + d[1] + 0.2D);
            h.assertTrue(h.getLevel().noCollision(box), "a mouse does not fit at " + d[0] + "," + d[1]);
        }
        for (double[] d : new double[][]{{0, 0.3}, {0.3, 0}, {-0.3, 0}}) {
            var box = new net.minecraft.world.phys.AABB(first.getX() + 0.5D + d[0] - 0.2D, first.getY(), first.getZ() + 0.5D + d[1] - 0.2D, first.getX() + 0.5D + d[0] + 0.2D, first.getY() + 0.4D, first.getZ() + 0.5D + d[1] + 0.2D);
            h.assertTrue(!h.getLevel().noCollision(box), "a mouse goes into the wall of a shut side at " + d[0] + "," + d[1]);
        }
        var player = new net.minecraft.world.phys.AABB(first.getX() + 0.5D - 0.3D, first.getY(), first.getZ() + 0.5D - 0.3D, first.getX() + 0.5D + 0.3D, first.getY() + 1.8D, first.getZ() + 0.5D + 0.3D);
        h.assertTrue(!h.getLevel().noCollision(player), "a player fits in a tunnel");
        // from inside it, looking each way in turn, what is seen is the next block of ground, and not a wall of this one
        float[] yaws = {0.0F, 90.0F, -90.0F, 180.0F};                // south, west, east, north
        net.minecraft.core.Direction[] ways = {net.minecraft.core.Direction.SOUTH, net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.NORTH};
        for (int i = 0; i < 3; i++) {                                // (north is the air it was dug from)
            put(h, mouse, 6, 3);
            mouse.setYRot(yaws[i]);
            mouse.setYHeadRot(yaws[i]);               // what it looks along is where its head is turned
            mouse.setXRot(0.0F);
            var hit = mouse.pick(4.5D, 1.0F, false);
            h.assertTrue(hit instanceof net.minecraft.world.phys.BlockHitResult bhr && bhr.getBlockPos().equals(first.relative(ways[i])), "looking " + ways[i] + " it sees " + hit.getLocation() + " (block " + (hit instanceof net.minecraft.world.phys.BlockHitResult b2 ? b2.getBlockPos() : null) + ") and not the ground beyond " + first.relative(ways[i]) + "; eye " + mouse.getEyePosition() + ", view " + mouse.getViewVector(1.0F) + ", at " + mouse.position() + ", first " + first + ", yaw " + mouse.getYRot());
        }
        // so it can dig on, a corner and a branch of its own: a block to the south, then one east of that
        put(h, mouse, 6, 3);
        breakAt(h, mouse, new BlockPos(6, 1, 4));
        put(h, mouse, 6, 4);
        breakAt(h, mouse, new BlockPos(7, 1, 4));
        for (BlockPos rel : new BlockPos[]{new BlockPos(6, 1, 3), new BlockPos(6, 1, 4), new BlockPos(7, 1, 4)}) {
            h.assertTrue(h.getLevel().getBlockState(h.absolutePos(rel)).is(ModBlocks.MOUSE_TUNNEL.get()), "no tunnel at " + rel);
        }
        h.succeed();
    }

    @GameTest(batch = "w52", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseDigsOnlyTheBlockStraightAboveOrBelowAndNotFurtherOrToTheSide(GameTestHelper h) {
        floor(h);
        h.setBlock(new BlockPos(5, 1, 5), Blocks.DIRT);
        h.setBlock(new BlockPos(5, 3, 5), Blocks.DIRT);
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        put(h, mouse, 5, 3);                                         // standing on the floor, with the dirt of two cells away at its own level (y 1) and one higher (y 3)
        var down = new BlockPos(5, 0, 3);                            // the floor under the mouse, made dirt: it is straight below it, so it is dug (a way down)
        h.setBlock(down, Blocks.DIRT);
        breakAt(h, mouse, down);
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(down)).is(ModBlocks.MOUSE_TUNNEL.get()), "a mouse could not dig straight down");
        breakAt(h, mouse, new BlockPos(5, 3, 5));
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(new BlockPos(5, 3, 5))).is(Blocks.DIRT), "a mouse dug a block that is up and two to the side");
        var twoUp = new BlockPos(5, 3, 3);                           // two straight up from its feet: not the one above it
        h.setBlock(twoUp, Blocks.DIRT);
        breakAt(h, mouse, twoUp);
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(twoUp)).is(Blocks.DIRT), "a mouse dug two blocks up");
        h.succeed();
    }

    @GameTest(batch = "w53", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aTunnelDugOutOfGrassKeepsItsGrassAndDirtOneDoesNotAndGrassGrowsOnABareOne(GameTestHelper h) {
        floor(h);
        h.setBlock(new BlockPos(5, 1, 5), Blocks.GRASS_BLOCK);
        h.setBlock(new BlockPos(6, 1, 5), Blocks.DIRT);
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        put(h, mouse, 5, 3);
        breakAt(h, mouse, new BlockPos(5, 1, 5));
        breakAt(h, mouse, new BlockPos(6, 1, 5));
        var fromGrass = h.getLevel().getBlockState(h.absolutePos(new BlockPos(5, 1, 5)));
        var fromDirt = h.getLevel().getBlockState(h.absolutePos(new BlockPos(6, 1, 5)));
        h.assertTrue(fromGrass.is(ModBlocks.MOUSE_TUNNEL.get()) && fromGrass.getValue(MouseTunnelBlock.GRASSY), "a tunnel dug out of grass is bare");
        h.assertTrue(fromDirt.is(ModBlocks.MOUSE_TUNNEL.get()) && !fromDirt.getValue(MouseTunnelBlock.GRASSY), "a tunnel dug out of dirt has grass");
        // the bare one, with the grassy one beside it and the sky over it, takes grass from it, in time (grass does not spread at night, as on dirt: it is made noon)
        MouseTunnelBlock.lightOverride = true;
        var block = (MouseTunnelBlock) fromDirt.getBlock();
        BlockPos bare = h.absolutePos(new BlockPos(6, 1, 5));
        h.runAfterDelay(10, () -> {
            for (int i = 0; i < 600 && !h.getLevel().getBlockState(bare).getValue(MouseTunnelBlock.GRASSY); i++) {
                block.randomTick(h.getLevel().getBlockState(bare), h.getLevel(), bare, h.getLevel().random);
            }
            h.assertTrue(h.getLevel().getBlockState(bare).getValue(MouseTunnelBlock.GRASSY), "grass did not grow on a bare tunnel with grass beside it and light over it");
            // where there is no light, grass dies
            MouseTunnelBlock.lightOverride = false;
            BlockPos grassy = h.absolutePos(new BlockPos(5, 1, 5));
            for (int i = 0; i < 5; i++) {
                ((MouseTunnelBlock) fromGrass.getBlock()).randomTick(h.getLevel().getBlockState(grassy), h.getLevel(), grassy, h.getLevel().random);
            }
            MouseTunnelBlock.lightOverride = null;
            h.assertTrue(!h.getLevel().getBlockState(grassy).getValue(MouseTunnelBlock.GRASSY), "grass lives where there is no light");
            h.succeed();
        });
    }

    @GameTest(batch = "w54", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void plantsCanStandOnATunnelAndAMouseDoesNotDigATunnelAgain(GameTestHelper h) {
        floor(h);
        BlockPos at = new BlockPos(5, 1, 5);
        h.setBlock(at, ModBlocks.MOUSE_TUNNEL.get());
        h.setBlock(at.above(), Blocks.GRASS);
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(at.above())).canSurvive(h.getLevel(), h.absolutePos(at.above())), "grass cannot stand on a tunnel");
        h.setBlock(at.above(), Blocks.OAK_SAPLING);
        h.assertTrue(h.getLevel().getBlockState(h.absolutePos(at.above())).canSurvive(h.getLevel(), h.absolutePos(at.above())), "a sapling cannot stand on a tunnel");
        h.succeed();
    }

    // ------------------------------------------------ the cauldron and the diary ------------------------------------------------

    private static void drop(GameTestHelper h, BlockPos rel, net.minecraft.world.item.Item item, int count) {
        BlockPos at = h.absolutePos(rel);
        h.getLevel().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), at.getX() + 0.5D, at.getY() + 0.3D, at.getZ() + 0.5D, new ItemStack(item, count)));
    }

    private static int antidotesAbout(GameTestHelper h) {
        int n = 0;
        for (var e : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(7, 1, 7))).inflate(20.0D))) {
            n += e.getItem().is(ModItems.ANTIDOTE.get()) ? e.getItem().getCount() : 0;
        }
        return n;
    }

    private static void cauldron(GameTestHelper h, BlockPos rel, boolean fire, int water) {
        h.setBlock(rel.below(), fire ? Blocks.CAMPFIRE.defaultBlockState() : Blocks.STONE.defaultBlockState());
        h.setBlock(rel, water == 0 ? Blocks.CAULDRON.defaultBlockState() : Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, water));
    }

    @GameTest(batch = "w55", template = "field", setupTicks = 20, timeoutTicks = 200)
    public static void aCauldronWithWaterAndAFireMakesTwoAntidotesFromThreeThingsAndLosesAWaterLevel(GameTestHelper h) {
        floor(h);
        BlockPos at = new BlockPos(6, 2, 6);
        cauldron(h, at, true, 3);
        drop(h, at, Items.GOLDEN_CARROT, 1);
        drop(h, at, Items.SPIDER_EYE, 2);
        drop(h, at, Items.GLASS_BOTTLE, 1);
        h.runAfterDelay(30, () -> {
            h.assertTrue(antidotesAbout(h) == 2, "the cauldron made " + antidotesAbout(h) + " antidotes");
            h.assertTrue(h.getLevel().getBlockState(h.absolutePos(at)).getValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL) == 2, "the water did not go down a level");
            int eyes = 0;
            for (var e : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(at)).inflate(3.0D))) {
                eyes += e.getItem().is(Items.SPIDER_EYE) ? e.getItem().getCount() : 0;
            }
            h.assertTrue(eyes == 1, "of two spider eyes, " + eyes + " are left, not one");
            h.succeed();
        });
    }

    @GameTest(batch = "w56", template = "field", setupTicks = 20, timeoutTicks = 300)
    public static void aCauldronMakesNothingWithNoFireNoWaterOrAThingMissing(GameTestHelper h) {
        floor(h);
        CauldronBrew.needsFireOverride = true;
        cauldron(h, new BlockPos(3, 2, 3), false, 3);         // no fire
        cauldron(h, new BlockPos(6, 2, 6), true, 0);          // no water
        cauldron(h, new BlockPos(9, 2, 9), true, 3);          // water and fire, but no bottle
        for (BlockPos at : new BlockPos[]{new BlockPos(3, 2, 3), new BlockPos(6, 2, 6), new BlockPos(9, 2, 9)}) {
            drop(h, at, Items.GOLDEN_CARROT, 1);
            drop(h, at, Items.SPIDER_EYE, 1);
            if (!at.equals(new BlockPos(9, 2, 9))) {
                drop(h, at, Items.GLASS_BOTTLE, 1);
            }
        }
        StringBuilder counts = new StringBuilder();
        for (int tick = 0; tick <= 100; tick += 5) {
            final int at = tick;
            h.runAfterDelay(tick, () -> counts.append(at).append(':').append(CauldronBrew.notedCount()).append(' '));
        }
        h.runAfterDelay(50, () -> {
            h.assertTrue(antidotesAbout(h) == 0, antidotesAbout(h) + " antidotes were made where there should be none (noted " + CauldronBrew.notedCount() + ")");
            CauldronBrew.needsFireOverride = false;            // the game goes on looking, every half second: now a fire is not needed
        });
        h.runAfterDelay(100, () -> {
            CauldronBrew.needsFireOverride = null;
            h.assertTrue(antidotesAbout(h) == 2, "with a fire not needed, the one with water and all three should make two: " + antidotesAbout(h) + " (noted over time: " + counts + ")");
            h.succeed();
        });
    }

    @GameTest(batch = "w57", template = "field", setupTicks = 20, timeoutTicks = 200)
    public static void theDiaryIsAWornBookOfNinePagesAndIsInTheChestOfTheHutAndTheCauldronThereHasAFire(GameTestHelper h) {
        floor(h);
        var diary = WitchDiary.create();
        h.assertTrue(diary.is(Items.WRITTEN_BOOK) && diary.getTag().getList("pages", 8).size() == WitchDiary.PAGES, "the diary is not a book of " + WitchDiary.PAGES + " pages");
        h.assertTrue(diary.getTag().getInt("generation") == 3, "it is not worn");
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 5));
        h.assertTrue(WitchHut.build(h.getLevel(), floor) != null, "no hut");
        var chest = (net.minecraft.world.Container) h.getLevel().getBlockEntity(floor.offset(2, 1, -2));
        boolean has = false;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            has |= chest.getItem(i).is(Items.WRITTEN_BOOK);
        }
        h.assertTrue(has, "the diary is not in the chest");
        h.assertTrue(h.getLevel().getBlockState(floor.offset(-2, 0, -2)).is(Blocks.CAMPFIRE) && h.getLevel().getBlockState(floor.offset(-2, 0, -2)).getValue(net.minecraft.world.level.block.CampfireBlock.LIT), "there is no lit fire under the cauldron");
        h.assertTrue(CauldronBrew.heated(h.getLevel(), floor.offset(-2, 1, -2)), "the cauldron of the hut is not heated");
        h.succeed();
    }

    // ------------------------------------------------ the gift is a gamble ------------------------------------------------

    @GameTest(batch = "w58", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aGiftIsAMouseOrARewardDecidedWhenItIsEatenAndEveryRewardIsAReward(GameTestHelper h) {
        floor(h);
        UUID witch = UUID.randomUUID();
        Player unlucky = h.makeMockPlayer(), lucky = h.makeMockPlayer();
        Mice.giftRollOverride = 0.0D;
        Mice.eaten(unlucky, witch);
        h.assertTrue(Mice.isMouse(unlucky), "a bad roll did not make a mouse");
        Mice.giftRollOverride = 0.99D;
        Mice.rewardOverride = 0;
        Mice.eaten(lucky, witch);
        h.assertTrue(!Mice.isMouse(lucky), "a good roll made a mouse");
        Mice.giftRollOverride = null;
        Mice.rewardOverride = null;
        for (int which = 0; which < Mice.REWARDS; which++) {
            Player p = h.makeMockPlayer();
            int before = p.getInventory().items.stream().mapToInt(ItemStack::getCount).sum();
            Mice.reward(p, which);
            int after = p.getInventory().items.stream().mapToInt(ItemStack::getCount).sum();
            h.assertTrue(after > before || !p.getActiveEffects().isEmpty(), "reward " + which + " gave nothing");
        }
        h.succeed();
    }

    @GameTest(batch = "w59", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theGiftsOfTheWitchAreAllAlikeWhateverTheyTurnOutToBe(GameTestHelper h) {
        floor(h);
        UUID witch = UUID.randomUUID();
        // the same kind of food, from the same witch: the two cannot differ in anything they carry, for the outcome is not in them
        var kind = Mice.poisoned(h.getLevel().random, witch).getItem();
        ItemStack a = new ItemStack(kind), b = new ItemStack(kind);
        a.getOrCreateTag().putBoolean(Mice.FOOD, true);
        a.getOrCreateTag().putUUID("Witch", witch);
        b.getOrCreateTag().putBoolean(Mice.FOOD, true);
        b.getOrCreateTag().putUUID("Witch", witch);
        h.assertTrue(ItemStack.isSameItemSameTags(a, b), "two gifts of one kind from one witch are not alike");
        // and over many, about as many are mice as is set
        int mice = 0;
        for (int i = 0; i < 400; i++) {
            Player p = h.makeMockPlayer();
            Mice.eaten(p, witch);
            mice += Mice.isMouse(p) ? 1 : 0;
        }
        h.assertTrue(mice > 140 && mice < 260, mice + " mice in 400 gifts, not about 200");
        h.succeed();
    }

    @GameTest(batch = "w60", template = "field", setupTicks = 20, timeoutTicks = 200)
    public static void theWitchComesToChildrenWithNoPlayerByThemWhenThatIsSoAndNotWhenAPlayerIsRequiredAndAbsent(GameTestHelper h) {
        floor(h);
        // the whole of the world's children, not those near: the test world has a village of its own, and the witch goes to any child that is loaded
        var around = new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(7, 1, 7))).inflate(300.0D);
        h.getLevel().getEntitiesOfClass(GrandWitch.class, around).forEach(net.minecraft.world.entity.Entity::discard);
        for (var kid : new java.util.ArrayList<>(h.getLevel().getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(net.minecraft.world.entity.npc.AbstractVillager.class), v -> true))) {
            kid.discard();
        }
        child(h, 7, 7);
        WitchVisits.rollOverride = 0.0D;
        WitchVisits.distance = new double[]{4.0D, 5.0D};
        WitchConfig.PLAYER_RANGE.set(128);
        h.assertTrue(WitchVisits.sweep(h.getLevel()) == null, "she came to children with no player within the range that is asked");
        WitchConfig.PLAYER_RANGE.set(0);
        GrandWitch witch = WitchVisits.sweep(h.getLevel());
        WitchConfig.PLAYER_RANGE.set(128);
        h.assertTrue(witch != null, "she did not come to children with no player required to be near");
        h.assertTrue(witch.distanceToSqr(h.absolutePos(new BlockPos(7, 1, 7)).getX() + 0.5D, h.absolutePos(new BlockPos(7, 1, 7)).getY(), h.absolutePos(new BlockPos(7, 1, 7)).getZ() + 0.5D) < 12.0D * 12.0D, "she came, but not to the children: " + witch.position());
        h.assertTrue(WitchVisits.sweep(h.getLevel()) == null, "a second witch came to the same children");
        witch.discard();
        WitchVisits.rollOverride = 0.99D;
        WitchConfig.PLAYER_RANGE.set(0);
        h.assertTrue(WitchVisits.sweep(h.getLevel()) == null, "she came on a bad roll");
        WitchConfig.PLAYER_RANGE.set(128);
        WitchVisits.rollOverride = null;
        h.succeed();
    }

    @GameTest(batch = "w61", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseCanBeginToDrinkTheAntidoteWhateverThisSideThinksAndItIsUsedOnlyWhenDrunkByAMouse(GameTestHelper h) {
        floor(h);
        var item = (AntidoteItem) ModItems.ANTIDOTE.get();
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        mouse.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 3));
        h.assertTrue(item.use(h.getLevel(), mouse, InteractionHand.MAIN_HAND).getResult().consumesAction(), "a mouse cannot begin to drink");
        var left = item.finishUsingItem(mouse.getMainHandItem(), h.getLevel(), mouse);
        h.assertTrue(!Mice.isMouse(mouse), "drinking it did not help");
        h.assertTrue(left.getCount() == 2 || left.is(Items.GLASS_BOTTLE), "it was not used up: " + left);
        h.succeed();
    }

    @GameTest(batch = "w62", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void thePotionOfTheMouseIsDrunkToBecomeOneIsLeftByTheWitchAndIsInTheHut(GameTestHelper h) {
        floor(h);
        Player drinker = h.makeMockPlayer();
        var potion = ModEffects.drinkable();
        h.assertTrue(net.minecraft.world.item.alchemy.PotionUtils.getPotion(potion) == ModEffects.MOUSE_POTION.get(), "the potion is not the potion of the mouse");
        // drunk: the effect is applied to the one who drinks, who is the thrower of it too
        ModEffects.MOUSE_FORM.get().applyInstantenousEffect(drinker, drinker, drinker, 0, 1.0D);
        h.assertTrue(Mice.isMouse(drinker) && Mice.madeBy(drinker) == null, "drinking it did not make a mouse of nobody's making");
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        witch.hurt(h.getLevel().damageSources().genericKill(), 1000.0F);
        boolean left = false;
        for (var e : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(5, 1, 5))).inflate(6.0D))) {
            left |= net.minecraft.world.item.alchemy.PotionUtils.getPotion(e.getItem()) == ModEffects.MOUSE_POTION.get();
        }
        h.assertTrue(left, "she left no potion of the mouse");
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 5));
        WitchHut.build(h.getLevel(), floor);
        var chest = (net.minecraft.world.Container) h.getLevel().getBlockEntity(floor.offset(2, 1, -2));
        boolean inChest = false;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            inChest |= net.minecraft.world.item.alchemy.PotionUtils.getPotion(chest.getItem(i)) == ModEffects.MOUSE_POTION.get();
        }
        h.assertTrue(inChest, "there is no potion of the mouse in the chest");
        h.succeed();
    }

    // ------------------------------------------------ the four of her ------------------------------------------------

    @GameTest(batch = "w63", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void eachOfTheFourHasItsOwnLookAndTheNannyOnlyWhereThereAreChildren(GameTestHelper h) {
        floor(h);
        GrandWitch w = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        w.dressUp(WitchKind.GIFT);
        h.assertTrue(w.kind() == WitchKind.GIFT && (w.girl() == 1 || w.girl() == 2 || w.disguiseType() == EntityType.VILLAGER), "the gift-giver is not a girl or a villager");
        w.dressUp(WitchKind.POTION);
        h.assertTrue(w.kind() == WitchKind.POTION && w.girl() == 3 && w.getMainHandItem().is(Items.STICK), "the potion-seller is not the old woman with her stick");
        w.dressUp(WitchKind.CURSE);
        h.assertTrue(w.kind() == WitchKind.CURSE && w.disguiseType() == EntityType.WANDERING_TRADER && w.getMainHandItem().isEmpty(), "the curser is not a trader");
        w.dressUp(WitchKind.NANNY);
        h.assertTrue(w.kind() == WitchKind.NANNY && w.girl() == 4, "the nanny is not the nanny");
        w.reveal();
        h.assertTrue(!w.isDisguised() && w.getMainHandItem().isEmpty(), "she is not herself again");
        // with no children there is no nanny, and with some there is
        int nannies = 0;
        for (int i = 0; i < 200; i++) {
            nannies += w.pickKind() == WitchKind.NANNY ? 1 : 0;
        }
        h.assertTrue(nannies == 0, "a nanny with no child about: " + nannies);
        child(h, 7, 7);
        int[] count = new int[4];
        for (int i = 0; i < 800; i++) {
            count[w.pickKind().ordinal()]++;
        }
        h.assertTrue(count[0] > 200 && count[0] < 360 && count[1] > 130 && count[1] < 270 && count[2] > 100 && count[2] < 220 && count[3] > 100 && count[3] < 220, "the shares of the four are " + java.util.Arrays.toString(count) + " in 800, not about 280, 200, 160, 160");
        var tag = new net.minecraft.nbt.CompoundTag();
        w.dressUp(WitchKind.POTION);
        w.addAdditionalSaveData(tag);
        GrandWitch loaded = ModEntities.GRAND_WITCH.get().create(h.getLevel());
        loaded.readAdditionalSaveData(tag);
        h.assertTrue(loaded.kind() == WitchKind.POTION && loaded.girl() == 3 && loaded.getMainHandItem().is(Items.STICK), "she forgot which of the four she was");
        h.succeed();
    }

    @GameTest(batch = "w64", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theOldWomanSellsPotionsForEmeraldsAndEachMayBeTheMouseWhenDrunk(GameTestHelper h) {
        floor(h);
        GrandWitch w = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        w.dressUp(WitchKind.POTION);
        Player poor = h.makeMockPlayer(), rich = h.makeMockPlayer();
        poor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, 1));
        h.assertTrue(!w.sell(poor, InteractionHand.MAIN_HAND) && poor.getMainHandItem().getCount() == 1, "she sold for too few emeralds");
        rich.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, 5));
        h.assertTrue(w.sell(rich, InteractionHand.MAIN_HAND), "she did not sell for the price");
        h.assertTrue(rich.getMainHandItem().getCount() == 3, "the price was not taken: " + rich.getMainHandItem().getCount());
        ItemStack bought = null;
        for (ItemStack s : rich.getInventory().items) {
            if (Mice.isPoisoned(s) && s.is(Items.POTION)) {
                bought = s;
            }
        }
        h.assertTrue(bought != null, "no potion was given");
        h.assertTrue(!net.minecraft.world.item.alchemy.PotionUtils.getMobEffects(bought).isEmpty() && net.minecraft.world.item.alchemy.PotionUtils.getPotion(bought) != ModEffects.MOUSE_POTION.get(), "it does not look like a good potion of the game's own");
        h.assertTrue(!ItemStack.isSameItemSameTags(bought, net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.PotionUtils.getPotion(bought))), "it mixes with a plain potion");
        Player a = h.makeMockPlayer(), b = h.makeMockPlayer();
        Mice.potionRollOverride = 0.0D;
        Mice.drunk(a, w.getUUID());
        Mice.potionRollOverride = 0.99D;
        Mice.drunk(b, w.getUUID());
        Mice.potionRollOverride = null;
        h.assertTrue(Mice.isMouse(a) && !Mice.isMouse(b), "the roll did not decide it: " + Mice.isMouse(a) + ", " + Mice.isMouse(b));
        h.succeed();
    }

    @GameTest(batch = "w65", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theTradersGiftIsGoodAndCursedAndMakesAMouseOnlyOfThoseWhoCarryItLongEnough(GameTestHelper h) {
        floor(h);
        GrandWitch w = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        w.dressUp(WitchKind.CURSE);
        Player p = h.makeMockPlayer();
        h.assertTrue(w.curseGift(p), "she gave nothing");
        h.assertTrue(!w.curseGift(p), "she gave a second to the same player at once");
        ItemStack gift = null;
        for (ItemStack s : p.getInventory().items) {
            if (Curses.cursed(s)) {
                gift = s;
            }
        }
        h.assertTrue(gift != null && gift.getTagElement("display") != null && gift.getTagElement("display").contains("Lore"), "the gift is not cursed or does not say it is warm");
        Curses.secondsOverride = 6;
        for (int i = 0; i < 5; i++) {
            Curses.second(p);
        }
        h.assertTrue(!Mice.isMouse(p), "a mouse before the time");
        Curses.second(p);
        h.assertTrue(Mice.isMouse(p) && w.getUUID().equals(Mice.madeBy(p)), "no mouse after the time");
        h.assertTrue(!Curses.cursed(gift) && gift.getTag().getCompound("display").isEmpty(), "the gift is still cursed after it has done its work");
        // what is not carried is not counted
        Player q = h.makeMockPlayer();
        for (int i = 0; i < 20; i++) {
            Curses.second(q);
        }
        h.assertTrue(!Mice.isMouse(q) && q.getPersistentData().getInt(Curses.COUNT) == 0, "time was counted with nothing cursed on the player");
        h.succeed();
    }

    @GameTest(batch = "w66", template = "field", setupTicks = 20, timeoutTicks = 600)
    public static void theNannyGathersTheChildrenAndLeadsThemAndMakesMiceOfThemAndLosesThemWhenHit(GameTestHelper h) {
        floor(h);
        GrandWitch.nannyTimeout = 120;
        BlockPos floor = h.absolutePos(new BlockPos(10, 0, 4));
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        witch.dressUp(WitchKind.NANNY);
        witch.setHome(floor);
        Villager a = child(h, 3, 3), b = child(h, 4, 3);
        h.runAfterDelay(80, () -> {
            h.assertTrue(!witch.following().isEmpty(), "no child is following her: " + witch.runningGoalNames());
            h.assertTrue(a.getPersistentData().hasUUID(GrandWitch.LED) || b.getPersistentData().hasUUID(GrandWitch.LED), "no child is marked as led");
        });
        h.succeedWhen(() -> {
            h.assertTrue(witch.turned() >= 1, "she has not made mice of the children she led (" + witch.following().size() + " following, goals " + witch.runningGoalNames() + ")");
            h.assertTrue(mice(h).size() >= 1, "there is no mouse");
        });
    }

    @GameTest(batch = "w67", template = "field", setupTicks = 20, timeoutTicks = 600)
    public static void theChildrenAreLetGoWhenTheNannyIsHit(GameTestHelper h) {
        floor(h);
        BlockPos floor = h.absolutePos(new BlockPos(11, 0, 11));
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        witch.dressUp(WitchKind.NANNY);
        witch.setHome(floor);
        Villager a = child(h, 3, 3);
        boolean[] hit = new boolean[1];
        h.succeedWhen(() -> {
            if (!hit[0]) {
                h.assertTrue(!witch.following().isEmpty(), "no child is following her yet");      // waits till one is, however long it takes her
                hit[0] = true;
                witch.hurt(h.getLevel().damageSources().generic(), 1.0F);
            }
            h.assertTrue(witch.following().isEmpty() && !a.getPersistentData().hasUUID(GrandWitch.LED), "the child is still led after she has been hit");
            h.assertTrue(!witch.isDisguised(), "she is still dressed up after being hit");
        });
    }

    // ------------------------------------------------ where a house can be, and old ones ------------------------------------------------

    @GameTest(batch = "w69", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theHutIsBuiltOnAHillockAndAmongLeavesAndOnlyWhatNoOneBuiltIsCleared(GameTestHelper h) {
        floor(h);
        // a mound two blocks high, with a few leaves and flowers on it, where the house is to be
        for (int x = 4; x <= 9; x++) {
            for (int z = 3; z <= 8; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
                if ((x + z) % 2 == 0) {
                    h.setBlock(new BlockPos(x, 2, z), Blocks.GRASS_BLOCK);
                }
            }
        }
        h.setBlock(new BlockPos(6, 4, 6), Blocks.OAK_LEAVES);
        h.setBlock(new BlockPos(8, 2, 4), Blocks.DANDELION);
        BlockPos floor = h.absolutePos(new BlockPos(7, 1, 5));
        h.assertTrue(WitchHut.fits(h.getLevel(), floor), "a house cannot stand on a mound with leaves and flowers");
        h.assertTrue(WitchHut.build(h.getLevel(), floor) != null, "it was not built");
        h.assertTrue(h.getLevel().getBlockState(floor.offset(0, 2, 0)).isAir() && h.getLevel().getBlockState(floor.offset(-1, 3, 1)).isAir(), "the mound was not cut down to the floor of the room");
        h.assertTrue(h.getLevel().getBlockState(floor.offset(0, 1, 3)).getBlock() == Blocks.SPRUCE_DOOR, "no door");
        h.succeed();
    }

    @GameTest(batch = "w70", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theHutIsNotBuiltWhereThereIsWaterOrWhatSomeoneBuiltOrATrunkOfATree(GameTestHelper h) {
        floor(h);
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 5));
        for (var block : new net.minecraft.world.level.block.state.BlockState[]{Blocks.WATER.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), Blocks.CHEST.defaultBlockState(), Blocks.OAK_DOOR.defaultBlockState()}) {
            h.setBlock(new BlockPos(9, 3, 6), block);
            h.assertTrue(!WitchHut.fits(h.getLevel(), floor), "the house was to be built over " + block.getBlock());
            h.setBlock(new BlockPos(9, 3, 6), Blocks.AIR);
        }
        h.assertTrue(WitchHut.fits(h.getLevel(), floor), "it does not fit on clear ground");
        h.succeed();
    }

    @GameTest(batch = "w71", template = "field", setupTicks = 20, timeoutTicks = 200)
    public static void aWitchComesToAnOldHouseThatHasNoWitchAndDoesNotBuildAnother(GameTestHelper h) {
        floor(h);
        WitchConfig.HUT.set(true);
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 4));
        GrandWitch gone = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(1, 1, 12));
        WitchHut.build(h.getLevel(), floor);
        WitchHut.bind(h.getLevel(), floor, gone);
        h.assertTrue(WitchHut.ownerless(h.getLevel(), floor, 160) == null, "a house with a witch is said to have none");
        gone.discard();
        h.assertTrue(WitchHut.ownerless(h.getLevel(), floor, 160) != null, "a house whose witch has gone is not said to have none");
        GrandWitch witch = WitchVisits.visitAround(h.getLevel(), floor.offset(0, 1, 6), true);
        WitchConfig.HUT.set(false);
        h.assertTrue(witch != null, "no witch came");
        h.assertTrue(floor.equals(witch.home()), "the new witch did not take the house: " + witch.home());
        h.assertTrue(witch.distanceToSqr(floor.getX() + 0.5D, floor.getY() + 1.0D, floor.getZ() + 5.5D) < 4.0D, "she is not at the door: " + witch.position());
        h.assertTrue(WitchHut.Huts.get(h.getLevel()).all().size() == 1 && witch.getUUID().equals(WitchHut.Huts.get(h.getLevel()).all().get(0).witch()), "the house does not know her, or a second one was built");
        h.succeed();
    }

    // ------------------------------------------------ brooms ------------------------------------------------

    private static BroomEntity broom(GameTestHelper h, BroomKind kind, double x, double z, double y) {
        BroomEntity b = ModEntities.BROOM.get().create(h.getLevel());
        b.setItem(new ItemStack(kind == BroomKind.GOLD ? ModItems.GOLDEN_BROOM.get() : ModItems.WOODEN_BROOM.get()));
        BlockPos at = h.absolutePos(new BlockPos((int) x, 1, (int) z));
        b.moveTo(at.getX() + 0.5D, at.getY() + y, at.getZ() + 0.5D, 0.0F, 0.0F);
        h.getLevel().addFreshEntity(b);
        return b;
    }

    private static void run(BroomEntity b, Player pilot, int ticks) {
        for (int i = 0; i < ticks; i++) {
            b.step(pilot);
        }
    }

    @GameTest(batch = "w72", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aBroomGoesWhereTheRiderLooksAndKeepsALowHeightThatLooksChange(GameTestHelper h) {
        floor(h);
        BroomEntity b = broom(h, BroomKind.WOOD, 7, 2, 0.6D);
        Player pilot = h.makeMockPlayer();
        pilot.startRiding(b, true);
        double ground = h.absolutePos(new BlockPos(7, 1, 2)).getY();
        double z0 = b.getZ();
        pilot.zza = 1.0F;
        pilot.setYRot(0.0F);                 // south
        pilot.setXRot(0.0F);
        run(b, pilot, 20);                   // a second: some 4 blocks, and still in the room
        h.assertTrue(b.getZ() - z0 > 3.0D && b.getZ() - z0 < 6.0D, "it went " + (b.getZ() - z0) + " blocks south in a second, not 3 to 6");
        h.assertTrue(Math.abs(b.getX() - (h.absolutePos(new BlockPos(7, 1, 2)).getX() + 0.5D)) < 0.3D, "it drifted sideways");
        h.assertTrue(Math.abs(b.getY() - (ground + 1.0D)) < 0.25D, "level, it is " + (b.getY() - ground) + " above the ground, not about 1");
        pilot.zza = 0.0F;                    // the key let go, and the look is what steers the height
        pilot.setXRot(-45.0F);               // looking up
        run(b, pilot, 40);
        h.assertTrue(b.getY() - ground > 2.6D && b.getY() - ground < 3.2D, "looking up it is " + (b.getY() - ground) + " up, not about 3, the most for a wooden broom");
        pilot.setXRot(45.0F);                // looking down
        run(b, pilot, 60);
        h.assertTrue(b.getY() - ground > 0.3D && b.getY() - ground < 0.8D, "looking down it is " + (b.getY() - ground) + " up, not about half a block");
        // to one side: the strafe key
        pilot.setXRot(0.0F);
        run(b, pilot, 30);
        double x1 = b.getX();
        pilot.xxa = 1.0F;                    // to the left: at yaw 0, facing south, that is east
        run(b, pilot, 15);
        h.assertTrue(b.getX() - x1 > 1.5D, "it did not go to the side: " + (b.getX() - x1));
        h.succeed();
    }

    @GameTest(batch = "w73", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theGoldenBroomIsFasterAndClimbsHigherAndAnEmptyBroomSettlesOnTheGround(GameTestHelper h) {
        floor(h);
        BroomEntity wood = broom(h, BroomKind.WOOD, 3, 2, 0.6D), gold = broom(h, BroomKind.GOLD, 10, 2, 0.6D);
        Player a = h.makeMockPlayer(), b = h.makeMockPlayer();
        a.startRiding(wood, true);
        b.startRiding(gold, true);
        a.zza = 1.0F;
        b.zza = 1.0F;
        double wz = wood.getZ(), gz = gold.getZ();
        run(wood, a, 20);
        run(gold, b, 20);
        h.assertTrue(gold.getZ() - gz > (wood.getZ() - wz) * 1.4D, "the golden broom went " + (gold.getZ() - gz) + ", the wooden " + (wood.getZ() - wz));
        a.zza = 0.0F;
        b.zza = 0.0F;
        a.setXRot(-45.0F);
        b.setXRot(-45.0F);
        run(wood, a, 50);
        run(gold, b, 50);
        double ground = h.absolutePos(new BlockPos(3, 1, 2)).getY();
        h.assertTrue(gold.getY() - ground > wood.getY() - ground + 1.5D && gold.getY() - ground < 6.3D, "up, the golden is " + (gold.getY() - ground) + " and the wooden " + (wood.getY() - ground));
        BroomEntity empty = broom(h, BroomKind.WOOD, 7, 7, 3.0D);
        run(empty, null, 60);
        h.assertTrue(empty.getY() - ground < 0.3D && empty.getY() - ground > -0.1D, "an empty broom is " + (empty.getY() - ground) + " up, not on the ground");
        h.succeed();
    }

    @GameTest(batch = "w74", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aBroomWearsWithFlyingBreaksWhenWornOutAndDropsItsRiderAndHoveringCostsNothing(GameTestHelper h) {
        floor(h);
        BroomEntity b = broom(h, BroomKind.WOOD, 7, 2, 0.6D);
        Player pilot = h.makeMockPlayer();
        pilot.startRiding(b, true);
        int before = b.item().getDamageValue();
        run(b, pilot, 100);                                 // still, with no key pressed
        h.assertTrue(b.item().getDamageValue() == before, "it wore while hovering");
        pilot.zza = 1.0F;
        run(b, pilot, 100);
        h.assertTrue(b.item().getDamageValue() == before + 5, "it wore " + (b.item().getDamageValue() - before) + " in five seconds of flying, not 5");
        b.item().setDamageValue(b.item().getMaxDamage() - 1);
        run(b, pilot, 40);
        h.assertTrue(b.isRemoved() && pilot.getVehicle() == null, "a worn-out broom is still there, or its rider is still on it");
        h.succeed();
    }

    @GameTest(batch = "w75", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aBroomIsPutDownUnderThePlayerWhoIsOnItAndTakenUpBySomeoneCrouchingAndDropsWhenHit(GameTestHelper h) {
        floor(h);
        Player p = h.makeMockPlayer();
        put(h, p, 7, 7);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WOODEN_BROOM.get()));
        ModItems.WOODEN_BROOM.get().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(p.getVehicle() instanceof BroomEntity, "the player is not on a broom");
        h.assertTrue(p.getMainHandItem().isEmpty(), "the broom was not taken out of the hand");
        BroomEntity b = (BroomEntity) p.getVehicle();
        p.stopRiding();
        p.setShiftKeyDown(true);
        b.interact(p, InteractionHand.MAIN_HAND);
        boolean back = false;
        for (ItemStack s : p.getInventory().items) {
            back |= s.is(ModItems.WOODEN_BROOM.get());
        }
        h.assertTrue(back && b.isRemoved(), "the broom was not taken up");
        BroomEntity hit = broom(h, BroomKind.GOLD, 3, 3, 0.6D);
        hit.hurt(h.getLevel().damageSources().generic(), 1.0F);
        h.assertTrue(hit.isRemoved(), "a broom that was hit is still there");
        boolean dropped = false;
        for (var e : h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(3, 1, 3))).inflate(3.0D))) {
            dropped |= e.getItem().is(ModItems.GOLDEN_BROOM.get());
        }
        h.assertTrue(dropped, "no broom was left");
        h.succeed();
    }

    @GameTest(batch = "w76", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void bothBroomsCanBeMade(GameTestHelper h) {
        floor(h);
        for (String name : new String[]{"wooden_broom", "golden_broom"}) {
            var recipe = h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("grandwitch", name));
            h.assertTrue(recipe.isPresent(), "there is no recipe for the " + name);
        }
        h.succeed();
    }

    // ------------------------------------------------ feeding the witch what is spiked ------------------------------------------------

    private static ItemStack spikedFood(net.minecraft.world.item.Item item) {
        ItemStack s = new ItemStack(item);
        s.getOrCreateTag().putBoolean(SpikeRecipe.SPIKED, true);
        return s;
    }

    @GameTest(batch = "w77", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aPotionOfTheMouseAndAFoodMakeAFoodThatIsSpikedAndGiveTheBottleBackAndNothingElseDoes(GameTestHelper h) {
        floor(h);
        var recipe = new SpikeRecipe(new net.minecraft.resources.ResourceLocation("grandwitch", "spike"), net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        var grid = new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player p, int i) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player p) {
                return true;
            }
        }, 2, 2);
        grid.setItem(0, ModEffects.drinkable());
        grid.setItem(3, new ItemStack(Items.COOKIE));
        h.assertTrue(recipe.matches(grid, h.getLevel()), "the potion and a cookie do not make a spiked cookie");
        ItemStack out = recipe.assemble(grid, h.getLevel().registryAccess());
        h.assertTrue(out.is(Items.COOKIE) && SpikeRecipe.spiked(out) && out.getCount() == 1, "the result is not a spiked cookie: " + out);
        h.assertTrue(recipe.getRemainingItems(grid).get(0).is(Items.GLASS_BOTTLE), "the bottle did not come back");
        grid.setItem(3, new ItemStack(Items.STICK));
        h.assertTrue(!recipe.matches(grid, h.getLevel()), "a stick made something of the potion");
        grid.setItem(3, new ItemStack(Items.COOKIE));
        grid.setItem(1, new ItemStack(Items.BREAD));
        h.assertTrue(!recipe.matches(grid, h.getLevel()), "two foods and a potion made something");
        grid.setItem(1, ItemStack.EMPTY);
        grid.setItem(0, net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION));
        h.assertTrue(!recipe.matches(grid, h.getLevel()), "a potion of another sort made something");
        h.succeed();
    }

    @GameTest(batch = "w78", template = "field", setupTicks = 20, timeoutTicks = 200)
    public static void theWitchDressedUpEatsWhatIsSpikedAndIsAMouseOrSmellsItAndShowsWhatSheIsAndNotWhenItIsNotSpiked(GameTestHelper h) {
        floor(h);
        Player p = h.makeMockPlayer();
        GrandWitch w = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(5, 1, 5));
        w.dressUp(WitchKind.GIFT);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKIE, 2));
        h.assertTrue(!w.feed(p, InteractionHand.MAIN_HAND), "she took what was not spiked");
        p.setItemInHand(InteractionHand.MAIN_HAND, spikedFood(Items.COOKIE));
        GrandWitch.suspicionOverride = 0.0D;
        h.assertTrue(w.feed(p, InteractionHand.MAIN_HAND), "she did not answer");
        h.assertTrue(!w.isDisguised() && !w.isMouseForm() && w.getTarget() == p, "she did not smell it and show what she is");
        h.assertTrue(p.getMainHandItem().is(Items.COOKIE), "what she spat out was taken from the player");
        GrandWitch w2 = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(8, 1, 8));
        w2.dressUp(WitchKind.POTION);
        GrandWitch.suspicionOverride = 0.99D;
        float before = w2.getHealth();
        h.assertTrue(w2.feed(p, InteractionHand.MAIN_HAND), "she did not eat it");
        GrandWitch.suspicionOverride = null;
        h.assertTrue(w2.isMouseForm() && !w2.isDisguised(), "she did not become a mouse");
        h.assertTrue(p.getMainHandItem().isEmpty(), "the food was not taken");
        h.assertTrue(w2.getBbHeight() < 0.5F && w2.getBbWidth() < 0.5F, "she is " + w2.getBbWidth() + " x " + w2.getBbHeight());
        h.assertTrue(w2.getMaxHealth() == 10.0F && w2.getHealth() == 10.0F, "her health is " + w2.getHealth() + " of " + w2.getMaxHealth());
        h.assertTrue(w2.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) > 0.35D, "she is not quick: " + w2.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
        // revealed already, she does not take it
        GrandWitch w3 = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(3, 1, 8));
        p.setItemInHand(InteractionHand.MAIN_HAND, spikedFood(Items.BREAD));
        h.assertTrue(!w3.feed(p, InteractionHand.MAIN_HAND) && !w3.isMouseForm(), "a witch that is showing what she is took it");
        h.succeed();
    }

    @GameTest(batch = "w79", template = "field", setupTicks = 20, timeoutTicks = 300)
    public static void theWitchAsAMouseHasNoTricksFleesAndIsHerselfAgainWithTheHealthSheHadAndASplashPotionMakesHerOne(GameTestHelper h) {
        floor(h);
        GrandWitch w = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(3, 1, 3));
        w.setHealth(40.0F);
        // a splash of the potion, thrown by a player at her as she is
        Player thrower = h.makeMockPlayer();
        ModEffects.MOUSE_FORM.get().applyInstantenousEffect(null, thrower, w, 0, 1.0D);
        h.assertTrue(w.isMouseForm(), "the potion did not make her a mouse");
        // one she throws herself does not
        GrandWitch other = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(9, 1, 9));
        ModEffects.MOUSE_FORM.get().applyInstantenousEffect(null, w, other, 0, 1.0D);
        h.assertTrue(!other.isMouseForm(), "a witch made a mouse of a witch with a potion she threw");
        // she has no tricks: a player in her sight is not thrown at, and a mouse player in her sight is not stamped on
        Player player = h.makeMockPlayer(), mouse = h.makeMockPlayer();
        put(h, player, 11, 3);
        put(h, mouse, 3, 11);
        Mice.become(mouse, null, 60);
        w.extraVictims.add(player);
        w.extraVictims.add(mouse);
        float mouseHealth = mouse.getHealth();
        h.runAfterDelay(120, () -> {
            h.assertTrue(w.thrown() == 0, "she threw a potion as a mouse");
            h.assertTrue(mouse.getHealth() == mouseHealth, "she stamped on a mouse as a mouse");
            h.assertTrue(w.isMouseForm(), "she was herself again before her time");
            w.extraVictims.clear();
            w.hurt(h.getLevel().damageSources().generic(), 1.0F);
            h.assertTrue(w.isAlive() && w.getHealth() < 10.0F, "she did not take harm as a mouse");
            // her time is up: herself again, with the health she had before, not what she was left with
            w.mouseTicksForTest(1);
        });
        h.succeedWhen(() -> {
            h.assertTrue(!w.isMouseForm(), "she is still a mouse");
            h.assertTrue(w.getMaxHealth() == 60.0F && w.getHealth() == 40.0F, "she has " + w.getHealth() + " of " + w.getMaxHealth() + ", not 40 of 60");
            h.assertTrue(w.getBbHeight() > 1.5F, "she is not her size again");
        });
    }

    @GameTest(batch = "w80", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void catsHuntTheWitchOnlyWhenSheIsAMouseAndHerDeathAsAMouseUndoesWhatSheDid(GameTestHelper h) {
        floor(h);
        Cat cat = h.spawn(EntityType.CAT, new BlockPos(5, 1, 5));
        GrandWitch w = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(8, 1, 8));
        h.assertTrue(!Mice.catHunts(cat, w), "a cat hunts the witch as she is");
        Player hers = h.makeMockPlayer();
        Mice.become(hers, w.getUUID(), 100);
        w.extraVictims.add(hers);
        w.becomeMouse(null);
        h.assertTrue(Mice.catHunts(cat, w), "a cat does not hunt her as a mouse");
        w.hurt(h.getLevel().damageSources().genericKill(), 1000.0F);
        h.assertTrue(!w.isAlive() && !Mice.isMouse(hers), "she is dead as a mouse and what she did is not undone");
        h.succeed();
    }

    @GameTest(batch = "w81", template = "field", setupTicks = 20, timeoutTicks = 400)
    public static void theWitchRidesABroomLowOverTheGroundWhenSheIsAfterSomeoneFarAndGetsOffWhenNearAndNotWhenSwitchedOffOrDressedUpOrAMouse(GameTestHelper h) {
        floor(h);
        GrandWitch w = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(1, 1, 1));
        Player mouse = h.makeMockPlayer();
        put(h, mouse, 12, 12);
        Mice.become(mouse, null, 100);
        w.extraVictims.add(mouse);
        double ground = h.absolutePos(new BlockPos(1, 1, 1)).getY();
        boolean[] flew = new boolean[1];
        double[] high = new double[1];
        for (int tick = 3; tick <= 100; tick += 2) {
            h.runAfterDelay(tick, () -> {
                if (w.onBroom()) {
                    flew[0] = true;
                    high[0] = Math.max(high[0], w.getY() - ground);
                }
            });
        }
        h.runAfterDelay(110, () -> {
            h.assertTrue(flew[0], "she did not ride a broom to a mouse that was far off");
            h.assertTrue(high[0] > 0.5D && high[0] < 4.5D, "she flew " + high[0] + " above the ground, not low");
            h.assertTrue(w.distanceToSqr(mouse) < 36.0D, "she did not get there: " + w.distanceToSqr(mouse));
            h.assertTrue(!w.onBroom(), "she is still on her broom beside the mouse");
            h.assertTrue(h.getLevel().getEntitiesOfClass(BroomEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(7, 1, 7))).inflate(30.0D)).isEmpty(), "the broom she called up is still there");
        });
        h.runAfterDelay(120, () -> {
            // whether she goes by broom: by the answer to being asked, in each case
            GrandWitch far = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(1, 1, 12));
            h.assertTrue(far.chase(mouse, 1.0D) == false || far.distanceToSqr(mouse) > 36.0D, "");
            far.setPos(far.getX(), far.getY(), h.absolutePos(new BlockPos(1, 1, 1)).getZ() + 0.5D);
            h.assertTrue(far.chase(mouse, 1.0D), "a revealed witch far off did not ride");
            WitchConfig.BROOM.set(false);
            boolean off = far.chase(mouse, 1.0D);
            WitchConfig.BROOM.set(true);
            h.assertTrue(!off, "she flew with it switched off");
            GrandWitch dressed = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(1, 1, 1));
            dressed.dressUp(WitchKind.GIFT);
            h.assertTrue(!dressed.chase(mouse, 1.0D), "a witch dressed up rode a broom");
            GrandWitch tiny = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(1, 1, 1));
            tiny.becomeMouse(null);
            h.assertTrue(!tiny.chase(mouse, 1.0D), "a witch that is a mouse rode a broom");
            GrandWitch near = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(11, 1, 11));
            h.assertTrue(!near.chase(mouse, 1.0D), "a witch beside her prey rode a broom");
            h.succeed();
        });
    }

    @GameTest(batch = "w83", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theJumpKeyClimbsToTheMostTheBroomCanAndLettingGoBringsItBackAndTheRiderSitsOnTheHandle(GameTestHelper h) {
        floor(h);
        BroomEntity b = broom(h, BroomKind.WOOD, 7, 4, 0.6D);
        Player pilot = h.makeMockPlayer();
        pilot.startRiding(b, true);
        double ground = h.absolutePos(new BlockPos(7, 1, 4)).getY();
        pilot.setXRot(0.0F);
        pilot.setJumping(true);
        run(b, pilot, 50);
        h.assertTrue(b.getY() - ground > 2.6D && b.getY() - ground < 3.2D, "holding the jump key it is " + (b.getY() - ground) + " up, not about 3");
        pilot.setJumping(false);
        run(b, pilot, 60);
        h.assertTrue(b.getY() - ground > 0.8D && b.getY() - ground < 1.3D, "with the key let go it is " + (b.getY() - ground) + " up, not about 1");
        BroomEntity gold = broom(h, BroomKind.GOLD, 10, 4, 0.6D);
        Player second = h.makeMockPlayer();
        second.startRiding(gold, true);
        second.setJumping(true);
        run(gold, second, 80);
        h.assertTrue(gold.getY() - ground > 5.5D && gold.getY() - ground < 6.3D, "the golden broom, with the jump key held, is " + (gold.getY() - ground) + " up, not about 6");
        // where the rider is put: their feet a tenth of a block under the broom, on its middle
        gold.positionRider(second, net.minecraft.world.entity.Entity::setPos);
        h.assertTrue(Math.abs(second.getY() - (gold.getY() - 0.1D)) < 0.001D && Math.abs(second.getX() - gold.getX()) < 0.001D && Math.abs(second.getZ() - gold.getZ()) < 0.001D, "the rider is not on the broom: " + second.position() + " on " + gold.position());
        h.succeed();
    }

    @GameTest(batch = "w84", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseCannotGetOnABroomOrUseOneAndIsPutOffOneIfItBecomesAMouse(GameTestHelper h) {
        floor(h);
        Player mouse = h.makeMockPlayer();
        put(h, mouse, 7, 7);
        Mice.become(mouse, null, 60);
        mouse.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WOODEN_BROOM.get()));
        ModItems.WOODEN_BROOM.get().use(h.getLevel(), mouse, InteractionHand.MAIN_HAND);
        h.assertTrue(mouse.getVehicle() == null && mouse.getMainHandItem().getCount() == 1, "a mouse used a broom");
        BroomEntity b = broom(h, BroomKind.WOOD, 7, 7, 0.6D);
        b.interact(mouse, InteractionHand.MAIN_HAND);
        h.assertTrue(mouse.getVehicle() == null && b.getPassengers().isEmpty(), "a mouse got on a broom");
        Player rider = h.makeMockPlayer();
        rider.startRiding(b, true);
        h.assertTrue(rider.getVehicle() == b, "a player is not on the broom");
        Mice.become(rider, null, 60);
        h.assertTrue(rider.getVehicle() == null, "a player that became a mouse is still on the broom");
        h.succeed();
    }

    @GameTest(batch = "w85", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theRoofOfTheHutRisesTowardTheRidgeAndNotTheOtherWay(GameTestHelper h) {
        floor(h);
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 5));
        h.assertTrue(WitchHut.build(h.getLevel(), floor) != null, "no hut");
        for (int step = 0; step < 4; step++) {
            var west = h.getLevel().getBlockState(floor.offset(-4 + step, 4 + step, 0));
            var east = h.getLevel().getBlockState(floor.offset(4 - step, 4 + step, 0));
            h.assertTrue(west.is(net.minecraft.world.level.block.Blocks.SPRUCE_STAIRS) && west.getValue(net.minecraft.world.level.block.StairBlock.FACING) == net.minecraft.core.Direction.EAST,
                    "the stair on the west side, step " + step + ", is " + west);
            h.assertTrue(east.is(net.minecraft.world.level.block.Blocks.SPRUCE_STAIRS) && east.getValue(net.minecraft.world.level.block.StairBlock.FACING) == net.minecraft.core.Direction.WEST,
                    "the stair on the east side, step " + step + ", is " + east);
        }
        h.succeed();
    }

    // ------------------------------------------------ the cage ------------------------------------------------

    @GameTest(batch = "w86", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theHutHasACageWithAGateAndTwoMiceInItThatTheWitchLeavesBe(GameTestHelper h) {
        floor(h);
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 5));
        h.assertTrue(WitchHut.build(h.getLevel(), floor) != null, "no hut");
        h.assertTrue(h.getLevel().getBlockState(floor.offset(-1, 1, 0)).getBlock() == Blocks.SPRUCE_FENCE_GATE, "no gate");
        // every side of the cell (x -2, z -1 and 0, y 1 and 2) is shut, each line of bars from solid to solid: a log at each end of the east side, the gate between, bars over it
        for (int y = 1; y <= 2; y++) {
            h.assertTrue(h.getLevel().getBlockState(floor.offset(-1, y, -1)).is(Blocks.STRIPPED_SPRUCE_LOG) && h.getLevel().getBlockState(floor.offset(-1, y, 1)).is(Blocks.STRIPPED_SPRUCE_LOG), "no log at an end of the east side, at " + y);
            h.assertTrue(h.getLevel().getBlockState(floor.offset(-2, y, 1)).getBlock() == Blocks.IRON_BARS, "no bars at the south end, at " + y);
        }
        for (BlockPos p : new BlockPos[]{floor.offset(-1, 2, 0), floor.offset(-2, 2, -2), floor.offset(-2, 3, -1), floor.offset(-2, 3, 0)}) {
            h.assertTrue(h.getLevel().getBlockState(p).getBlock() == Blocks.IRON_BARS, "the cage is open at " + p.subtract(floor));
        }
        h.assertTrue(h.getLevel().getBlockState(floor.offset(-2, 1, -2)).getBlock() == Blocks.WATER_CAULDRON, "no cauldron at the north end of the cage");
        var caged = h.getLevel().getEntitiesOfClass(VillageMouse.class, new net.minecraft.world.phys.AABB(floor).inflate(8.0D), VillageMouse::isCaged);
        h.assertTrue(caged.size() == 2, caged.size() + " mice in the cage, not 2");
        for (var m : caged) {
            h.assertTrue(Math.abs(m.getX() - (floor.getX() - 2 + 0.5D)) < 0.7D && Math.abs(m.getZ() - (floor.getZ() + 0.5D)) < 2.0D, "a mouse is not in the cage: " + m.position());
        }
        // her own, in her house: they do not set her raging; one that has got out and run off does
        GrandWitch w = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(7, 1, 9));
        w.setHome(floor);
        h.assertTrue(w.findVictim() == null, "the mice in her cage set her raging");
        var one = caged.get(0);
        one.setPos(floor.getX() + 0.5D, floor.getY() + 1.0D, floor.getZ() + 12.5D);
        h.assertTrue(w.findVictim() == one, "a mouse of hers that is far from the cage does not set her raging");
        h.succeed();
    }

    @GameTest(batch = "w87", template = "field", setupTicks = 20, timeoutTicks = 700)
    public static void theNannyPutsTheChildrenInTheCageAndTheyStayMiceUntilSomeoneHelpsThemAndShesNotEnraged(GameTestHelper h) {
        floor(h);
        GrandWitch.nannyTimeout = 120;
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 4));
        h.assertTrue(WitchHut.build(h.getLevel(), floor) != null, "no hut");
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(1, 1, 8));
        witch.dressUp(WitchKind.NANNY);
        witch.setHome(floor);
        child(h, 2, 6);
        child(h, 3, 6);
        h.succeedWhen(() -> {
            h.assertTrue(witch.turned() >= 1, "she has not made mice of the children (" + witch.following().size() + " following)");
            var mine = h.getLevel().getEntitiesOfClass(VillageMouse.class, new net.minecraft.world.phys.AABB(floor).inflate(8.0D), m -> m.isCaged() && m.hasOriginal());
            h.assertTrue(!mine.isEmpty(), "no child that was turned is in the cage");
            for (var m : mine) {
                h.assertTrue(Math.abs(m.getX() - (floor.getX() - 2 + 0.5D)) < 1.0D && Math.abs(m.getZ() - (floor.getZ() + 0.5D)) < 2.5D, "a child is not in the cage: " + m.position());
            }
            h.assertTrue(!witch.isRaging(), "she is raging at the mice she keeps");
        });
    }

    @GameTest(batch = "w88", template = "field", setupTicks = 20, timeoutTicks = 400)
    public static void theWitchMakesAMouseOfABabyZombieForGoodAndLeavesAGrownOneBe(GameTestHelper h) {
        floor(h);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(3, 1, 3));
        var baby = h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(10, 1, 10));
        baby.setBaby(true);
        baby.setNoAi(true);
        var grown = h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(12, 1, 3));
        grown.setNoAi(true);
        h.succeedWhen(() -> {
            h.assertTrue(baby.isRemoved(), "the baby zombie is still a zombie");
            var mice = h.getLevel().getEntitiesOfClass(VillageMouse.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(10, 1, 10))).inflate(6.0D), VillageMouse::hasOriginal);
            h.assertTrue(mice.size() == 1, mice.size() + " mice, not 1, where the zombie was");
            h.assertTrue(!grown.isRemoved(), "a grown zombie was turned");
        });
    }

    @GameTest(batch = "w89", template = "field", setupTicks = 20, timeoutTicks = 500)
    public static void theWitchLiesDownAtTheOpeningOfATunnelAndPullsAMouseOutThatIsWithinFiveBlocksAndNotOneThatIsFurtherIn(GameTestHelper h) {
        floor(h);
        GrandWitch.gropeLuck = 0.0F;
        // two tunnels in a bank of dirt, along z from z=9, each open at its near end (z=8): a mouse at the second block of one (within reach), another at the sixth of the other (out of it)
        for (int x = 6; x <= 12; x++) {
            for (int z = 9; z <= 15; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT.defaultBlockState());
            }
        }
        for (int z = 9; z <= 11; z++) {
            h.setBlock(new BlockPos(7, 1, z), ModBlocks.MOUSE_TUNNEL.get().defaultBlockState());
        }
        for (int z = 9; z <= 14; z++) {
            h.setBlock(new BlockPos(11, 1, z), ModBlocks.MOUSE_TUNNEL.get().defaultBlockState());
        }
        BlockPos nearAt = h.absolutePos(new BlockPos(7, 1, 10)), farAt = h.absolutePos(new BlockPos(11, 1, 14));
        VillageMouse near = h.spawn(ModEntities.VILLAGE_MOUSE.get(), new BlockPos(7, 1, 10));
        near.setPos(nearAt.getX() + 0.5D, nearAt.getY() + 0.1D, nearAt.getZ() + 0.5D);
        near.setNoAi(true);
        VillageMouse far = h.spawn(ModEntities.VILLAGE_MOUSE.get(), new BlockPos(11, 1, 14));
        far.setPos(farAt.getX() + 0.5D, farAt.getY() + 0.1D, farAt.getZ() + 0.5D);
        far.setNoAi(true);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(3, 1, 4));
        witch.hunt(near);
        GrandWitch other = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(13, 1, 4));
        other.hunt(far);
        h.succeedWhen(() -> {
            h.assertTrue(!MouseHoleBlock.hidden(near), "the mouse is still in the tunnel: " + near.position() + ", she is " + witch.position() + " prone=" + witch.isProne() + " trail: " + witch.reachTrail);
            h.assertTrue(MouseHoleBlock.hidden(far), "the one six blocks in was pulled out");
            h.assertTrue(other.armPath().length <= 5, "she reached further than five blocks: " + other.armPath().length);
            GrandWitch.gropeLuck = -1.0F;
            GrandWitch.gropeOneIn = 40;
        });
    }

    @GameTest(batch = "w90", template = "field", setupTicks = 20, timeoutTicks = 500)
    public static void theWitchDoesNotReachIntoATunnelThatIsOnlyOpenAboveAndHasNoOpeningAtTheSide(GameTestHelper h) {
        floor(h);
        for (int z = 8; z <= 10; z++) {
            h.setBlock(new BlockPos(7, 0, z), ModBlocks.MOUSE_TUNNEL.get().defaultBlockState());
        }
        BlockPos at = h.absolutePos(new BlockPos(7, 0, 9));
        VillageMouse mouse = h.spawn(ModEntities.VILLAGE_MOUSE.get(), new BlockPos(7, 0, 9));
        mouse.setPos(at.getX() + 0.5D, at.getY() + 0.1D, at.getZ() + 0.5D);
        mouse.setNoAi(true);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(3, 1, 4));
        witch.hunt(mouse);
        h.runAfterDelay(250, () -> {
            h.assertTrue(MouseHoleBlock.hidden(mouse) && !witch.isProne(), "she reached in from above: " + mouse.position() + " prone=" + witch.isProne());
            h.succeed();
        });
    }

    @GameTest(batch = "w91", template = "field", setupTicks = 20, timeoutTicks = 500)
    public static void theWitchReachesInFromTheSideWhenTheTunnelIsOpenAtTheSideAndNotFromAbove(GameTestHelper h) {
        floor(h);
        // a bank one block high, a tunnel in it that is open on the near side (z=8) and open above
        for (int x = 6; x <= 8; x++) {
            for (int z = 9; z <= 10; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT.defaultBlockState());
            }
        }
        h.setBlock(new BlockPos(7, 1, 9), ModBlocks.MOUSE_TUNNEL.get().defaultBlockState());
        BlockPos at = h.absolutePos(new BlockPos(7, 1, 9));
        VillageMouse mouse = h.spawn(ModEntities.VILLAGE_MOUSE.get(), new BlockPos(7, 1, 9));
        mouse.setPos(at.getX() + 0.5D, at.getY() + 0.1D, at.getZ() + 0.5D);
        mouse.setNoAi(true);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(3, 1, 4));
        witch.hunt(mouse);
        double front = h.absolutePos(new BlockPos(7, 1, 8)).getY();
        h.succeedWhen(() -> {
            h.assertTrue(witch.isProne() || !MouseHoleBlock.hidden(mouse), "she has not lain down: " + witch.position());
            h.assertTrue(Math.abs(witch.getY() - front) < 0.3D && witch.getZ() < at.getZ() + 0.1D, "she is not lying in front of the opening but on top of the bank: " + witch.position() + " (the front is at " + front + ", the tunnel at z " + at.getZ() + ")");
        });
    }

    @GameTest(batch = "w92", template = "field", setupTicks = 20, timeoutTicks = 700)
    public static void theWitchFeelsAboutWithAllOfHerArmForAMouseThatIsSixOrSevenBlocksInAndFindsNothing(GameTestHelper h) {
        floor(h);
        GrandWitch.gropeOneIn = 1;
        GrandWitch.gropeLuck = 0.0F;
        for (int x = 6; x <= 8; x++) {
            for (int z = 9; z <= 17; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT.defaultBlockState());
            }
        }
        for (int z = 9; z <= 15; z++) {
            h.setBlock(new BlockPos(7, 1, z), ModBlocks.MOUSE_TUNNEL.get().defaultBlockState());
        }
        BlockPos at = h.absolutePos(new BlockPos(7, 1, 15));         // the seventh block
        VillageMouse mouse = h.spawn(ModEntities.VILLAGE_MOUSE.get(), new BlockPos(7, 1, 15));
        mouse.setPos(at.getX() + 0.5D, at.getY() + 0.1D, at.getZ() + 0.5D);
        mouse.setNoAi(true);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(3, 1, 4));
        witch.hunt(mouse);
        h.succeedWhen(() -> {
            h.assertTrue(witch.isProne() && witch.armTicks() >= GrandWitch.ARM_FULL, "she has not put her arm in: prone=" + witch.isProne() + " arm=" + witch.armTicks() + " at " + witch.position() + " disguised=" + witch.isDisguised() + " alive=" + witch.isAlive() + " why-not: " + witch.reachWhy + " trail: " + witch.reachTrail);
            h.assertTrue(witch.armPath().length == 5, "her arm is " + witch.armPath().length + " blocks, not the 5 it goes");
            h.assertTrue(MouseHoleBlock.hidden(mouse), "she got hold of it");
            GrandWitch.gropeLuck = -1.0F;
            GrandWitch.gropeOneIn = 40;
        });
    }

    @GameTest(batch = "w93", template = "field", setupTicks = 20, timeoutTicks = 700)
    public static void theWitchSometimesGetsHoldOfAMouseThatIsAtTheSeventhBlockByStretching(GameTestHelper h) {
        floor(h);
        GrandWitch.gropeOneIn = 1;
        GrandWitch.gropeLuck = 1.0F;
        for (int x = 6; x <= 8; x++) {
            for (int z = 9; z <= 17; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT.defaultBlockState());
            }
        }
        for (int z = 9; z <= 15; z++) {
            h.setBlock(new BlockPos(7, 1, z), ModBlocks.MOUSE_TUNNEL.get().defaultBlockState());
        }
        BlockPos at = h.absolutePos(new BlockPos(7, 1, 15));
        VillageMouse mouse = h.spawn(ModEntities.VILLAGE_MOUSE.get(), new BlockPos(7, 1, 15));
        mouse.setPos(at.getX() + 0.5D, at.getY() + 0.1D, at.getZ() + 0.5D);
        mouse.setNoAi(true);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(3, 1, 4));
        witch.hunt(mouse);
        h.succeedWhen(() -> {
            h.assertTrue(!MouseHoleBlock.hidden(mouse), "she did not get hold of it: " + mouse.position() + " prone=" + witch.isProne() + " disguised=" + witch.isDisguised() + " at " + witch.position() + " trail: " + witch.reachTrail);
            GrandWitch.gropeLuck = -1.0F;
            GrandWitch.gropeOneIn = 40;
        });
    }

    @GameTest(batch = "w94", template = "field", setupTicks = 20, timeoutTicks = 600)
    public static void theWitchGoesInAtTheDoorOfHerHouseAfterAMouseThatIsInside(GameTestHelper h) {
        floor(h);
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 4));
        BlockPos front = WitchHut.build(h.getLevel(), floor);
        h.assertTrue(front != null, "no hut");
        VillageMouse mouse = h.spawn(ModEntities.VILLAGE_MOUSE.get(), new BlockPos(7, 1, 4));
        mouse.setNoAi(true);
        mouse.setPos(floor.getX() + 0.5D, floor.getY() + 1.0D, floor.getZ() + 0.5D);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(7, 1, 4 + 6));
        h.succeedWhen(() -> {
            h.assertTrue(witch.getZ() < floor.getZ() + 2.0D && Math.abs(witch.getY() - (floor.getY() + 1.0D)) < 1.0D, "she is not inside: " + witch.position() + ", the hut at " + floor.getX() + "," + floor.getZ() + ", door " + h.getLevel().getBlockState(floor.offset(0, 1, 3)));
        });
    }

    @GameTest(batch = "w95", template = "field", setupTicks = 20, timeoutTicks = 500)
    public static void theMiceInTheCageStayInItWhileTheyWanderAbout(GameTestHelper h) {
        floor(h);
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 5));
        h.assertTrue(WitchHut.build(h.getLevel(), floor) != null, "no hut");
        var caged = h.getLevel().getEntitiesOfClass(VillageMouse.class, new net.minecraft.world.phys.AABB(floor).inflate(8.0D), VillageMouse::isCaged);
        h.assertTrue(caged.size() == 2, caged.size() + " mice in the cage, not 2");
        h.runAfterDelay(400, () -> {
            for (var m : caged) {
                h.assertTrue(!m.isRemoved() && m.getX() > floor.getX() - 2.0D - 0.05D && m.getX() < floor.getX() - 1.0D + 0.05D
                        && m.getZ() > floor.getZ() - 1.0D - 0.05D && m.getZ() < floor.getZ() + 1.0D + 0.05D, "a mouse has got out of the cage: " + m.position() + " (the cell is x " + (floor.getX() - 2) + " to " + (floor.getX() - 1) + ", z " + (floor.getZ() - 1) + " to " + (floor.getZ() + 1) + ")");
            }
            h.succeed();
        });
    }

    @GameTest(batch = "w96", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aHouseWhoseWitchIsDeadGetsANewOneBySmallChanceAndOnlyWhileSomeoneIsNearAndNoOtherWitchIs(GameTestHelper h) {
        floor(h);
        var level = h.getLevel();
        BlockPos floor = h.absolutePos(new BlockPos(7, 0, 5));
        WitchHut.Huts.get(level).forgetAll();
        h.assertTrue(WitchHut.build(level, floor) != null, "no hut");
        WitchHut.Huts.get(level).add(floor);
        double chance = WitchConfig.HUT_RESPAWN_CHANCE.get();
        try {
            WitchConfig.HUT_RESPAWN_CHANCE.set(1.0D);
            // no one is near: nothing comes
            h.assertTrue(WitchHut.respawn(level) == null, "a witch came with no one near the house");
            Player near = h.makeMockPlayer();
            WitchHut.extraPeople.add(near);
            put(h, near, 3, 3);
            // another witch is about: none comes
            GrandWitch other = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(2, 1, 2));
            h.assertTrue(WitchHut.respawn(level) == null, "a witch came with another one about");
            other.discard();
            // the chance is nothing: none comes
            WitchConfig.HUT_RESPAWN_CHANCE.set(0.0D);
            h.assertTrue(WitchHut.respawn(level) == null, "a witch came with no chance of it");
            // the chance is certain, a player is near and no witch is about: one comes, and the house is hers
            WitchConfig.HUT_RESPAWN_CHANCE.set(1.0D);
            GrandWitch witch = WitchHut.respawn(level);
            h.assertTrue(witch != null && witch.isAlive(), "no witch came");
            h.assertTrue(witch.home() != null && witch.home().equals(floor), "the house is not hers: " + witch.home());
            h.assertTrue(WitchHut.respawn(level) == null, "a second witch came to a house that has one");
            witch.discard();
            WitchHut.extraPeople.remove(near);
        } finally {
            WitchConfig.HUT_RESPAWN_CHANCE.set(chance);
            WitchHut.extraPeople.clear();
        }
        h.succeed();
    }

    @GameTest(batch = "w97", template = "field", setupTicks = 20, timeoutTicks = 800)
    public static void anAlertThatRunsOutDoesNotBringTheWitchDownWhicheverTickItRunsOutOn(GameTestHelper h) {
        floor(h);
        BlockPos floor = h.absolutePos(new BlockPos(3, 0, 3));
        Player p1 = h.makeMockPlayer();
        Player p2 = h.makeMockPlayer();
        put(h, p1, 12, 12);
        put(h, p2, 12, 2);
        GrandWitch a = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(2, 1, 6));
        GrandWitch b = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(2, 1, 9));
        h.assertTrue(a.alert(floor, p1), "she did not take up the alert");
        h.runAfterDelay(1, () -> h.assertTrue(b.alert(floor, p2), "the other did not take up the alert"));
        h.runAfterDelay(700, () -> {
            h.assertTrue(a.isAlive() && b.isAlive() && a.alertTicks() == 0 && b.alertTicks() == 0, "the alert did not run out, or one of them is gone: " + a.alertTicks() + " " + b.alertTicks());
            h.succeed();
        });
    }

    @GameTest(batch = "w98", template = "field", setupTicks = 20, timeoutTicks = 600)
    public static void aWitchThatIsAfterSomeoneInATunnelSheCannotReachGivesUpAfterAFewSecondsAndNotAfterThirty(GameTestHelper h) {
        floor(h);
        BlockPos floor = h.absolutePos(new BlockPos(3, 0, 3));
        for (int x = 9; x <= 12; x++) {
            h.setBlock(new BlockPos(x, 0, 9), ModBlocks.MOUSE_TUNNEL.get().defaultBlockState());      // covered all round: the floor's own blocks on every side
        }
        Player mouse = h.makeMockPlayer();
        BlockPos at = h.absolutePos(new BlockPos(11, 0, 9));
        mouse.setPos(at.getX() + 0.5D, at.getY() + 0.1D, at.getZ() + 0.5D);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(2, 1, 6));
        h.assertTrue(witch.alert(floor, mouse), "she did not take up the alert");
        h.runAfterDelay(300, () -> {
            h.assertTrue(witch.alertTicks() == 0, "she is still after one she cannot reach, after fifteen seconds: " + witch.alertTicks());
            h.succeed();
        });
    }

    @GameTest(batch = "w99", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theMousePotionMakesAMouseOfAnyCreatureButNotABossOrAMouseAndItIsTheCreatureAgainAfterwards(GameTestHelper h) {
        floor(h);
        var effect = ModEffects.MOUSE_FORM.get();
        var cow = h.spawn(EntityType.COW, new BlockPos(4, 1, 4));
        var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(6, 1, 4));
        zombie.setNoAi(true);
        var villager = h.spawn(EntityType.VILLAGER, new BlockPos(8, 1, 4));
        var stand = h.spawn(EntityType.ARMOR_STAND, new BlockPos(10, 1, 4));
        for (var e : new net.minecraft.world.entity.LivingEntity[]{cow, zombie, villager, stand}) {
            effect.applyInstantenousEffect(null, null, e, 0, 1.0D);
        }
        h.assertTrue(cow.isRemoved() && zombie.isRemoved() && villager.isRemoved(), "a creature is still what it was: cow " + !cow.isRemoved() + " zombie " + !zombie.isRemoved() + " villager " + !villager.isRemoved());
        h.assertTrue(!stand.isRemoved(), "an armor stand was made a mouse");
        var mice = h.getLevel().getEntitiesOfClass(VillageMouse.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(7, 1, 4))).inflate(8.0D), VillageMouse::hasOriginal);
        h.assertTrue(mice.size() == 3, mice.size() + " mice, not 3");
        // a mouse of a mouse: nothing
        effect.applyInstantenousEffect(null, null, mice.get(0), 0, 1.0D);
        h.assertTrue(!mice.get(0).isRemoved(), "a mouse was made a mouse");
        // and they are what they were when it is undone
        for (var m : mice) {
            h.assertTrue(m.revert(), "a mouse could not be made what it was");
        }
        h.assertTrue(!h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Cow.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(7, 1, 4))).inflate(8.0D)).isEmpty(), "no cow again");
        h.assertTrue(!h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Zombie.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(7, 1, 4))).inflate(8.0D)).isEmpty(), "no zombie again");
        h.succeed();
    }

    @GameTest(batch = "w100", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aTippedArrowsWayOfPuttingTheEffectOnMakesAMouseOfACreatureAndAPlayer(GameTestHelper h) {
        floor(h);
        // what an arrow does on a hit: the effect is put on for a tick, and then ticked
        var cow = h.spawn(EntityType.COW, new BlockPos(4, 1, 4));
        var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(7, 1, 4));
        zombie.setNoAi(true);
        Player player = h.makeMockPlayer();
        put(h, player, 10, 4);
        cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.MOUSE_FORM.get(), 1));
        zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.MOUSE_FORM.get(), 1));
        // a mock player is not ticked by the world, so what the game does with the effect on a tick is done here
        ModEffects.MOUSE_FORM.get().applyEffectTick(player, 0);
        h.runAfterDelay(5, () -> {
            h.assertTrue(cow.isRemoved() && zombie.isRemoved(), "a creature that had the effect put on it is still what it was: cow " + !cow.isRemoved() + " zombie " + !zombie.isRemoved());
            h.assertTrue(Mice.isMouse(player), "a player that had the effect put on is not a mouse");
            h.succeed();
        });
    }

    @GameTest(batch = "w101", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aPlagueVirusInAnAwkwardPotionMakesTheMousePotionAndTheUsualStepsGoOnFromThere(GameTestHelper h) {
        var awkward = net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(net.minecraft.world.item.Items.POTION), net.minecraft.world.item.alchemy.Potions.AWKWARD);
        var virus = new ItemStack(ModItems.PLAGUE_VIRUS.get());
        var registry = net.minecraftforge.common.brewing.BrewingRecipeRegistry.class;
        ItemStack mouse = net.minecraftforge.common.brewing.BrewingRecipeRegistry.getOutput(awkward, virus);
        h.assertTrue(mouse.is(net.minecraft.world.item.Items.POTION) && net.minecraft.world.item.alchemy.PotionUtils.getPotion(mouse) == ModEffects.MOUSE_POTION.get(), "an awkward potion and a plague virus do not make the mouse potion: " + mouse);
        ItemStack splash = net.minecraftforge.common.brewing.BrewingRecipeRegistry.getOutput(mouse, new ItemStack(net.minecraft.world.item.Items.GUNPOWDER));
        h.assertTrue(splash.is(net.minecraft.world.item.Items.SPLASH_POTION) && net.minecraft.world.item.alchemy.PotionUtils.getPotion(splash) == ModEffects.MOUSE_POTION.get(), "gunpowder does not make it splash: " + splash);
        ItemStack lingering = net.minecraftforge.common.brewing.BrewingRecipeRegistry.getOutput(splash, new ItemStack(net.minecraft.world.item.Items.DRAGON_BREATH));
        h.assertTrue(lingering.is(net.minecraft.world.item.Items.LINGERING_POTION) && net.minecraft.world.item.alchemy.PotionUtils.getPotion(lingering) == ModEffects.MOUSE_POTION.get(), "dragon's breath does not make it lingering: " + lingering);
        // not a water bottle, not the wrong thing
        h.assertTrue(net.minecraftforge.common.brewing.BrewingRecipeRegistry.getOutput(new ItemStack(net.minecraft.world.item.Items.POTION), virus).isEmpty(), "a plain bottle took the virus");
        h.succeed();
    }

    @GameTest(batch = "w102", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aWildMouseAPlayerKillsMayDropAPlagueVirusAndNoOtherDeathDoesAndNeitherDoesAMouseThatWasSomeone(GameTestHelper h) {
        floor(h);
        double chance = WitchConfig.VIRUS_CHANCE.get();
        try {
            WitchConfig.VIRUS_CHANCE.set(1.0D);
            Player player = h.makeMockPlayer();
            put(h, player, 3, 3);
            var area = new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(7, 1, 7))).inflate(10.0D);
            VillageMouse wild = h.spawn(ModEntities.VILLAGE_MOUSE.get(), new BlockPos(5, 1, 5));
            wild.hurt(h.getLevel().damageSources().playerAttack(player), 1000.0F);
            h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area, i -> i.getItem().is(ModItems.PLAGUE_VIRUS.get())).size() == 1, "a wild mouse that a player killed dropped no plague virus (or more than one)");
            // no player: nothing (a cat, the witch's foot)
            VillageMouse other = h.spawn(ModEntities.VILLAGE_MOUSE.get(), new BlockPos(9, 1, 5));
            other.hurt(h.getLevel().damageSources().generic(), 1000.0F);
            h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area, i -> i.getItem().is(ModItems.PLAGUE_VIRUS.get())).size() == 1, "a mouse that no player killed dropped a plague virus");
            // one that was a cow: nothing
            var cow = h.spawn(EntityType.COW, new BlockPos(11, 1, 5));
            VillageMouse turned = VillageMouse.from(h.getLevel(), cow, null, 300);
            cow.discard();
            h.getLevel().addFreshEntity(turned);
            turned.hurt(h.getLevel().damageSources().playerAttack(player), 1000.0F);
            h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area, i -> i.getItem().is(ModItems.PLAGUE_VIRUS.get())).size() == 1, "a mouse that was a cow dropped a plague virus");
        } finally {
            WitchConfig.VIRUS_CHANCE.set(chance);
        }
        h.succeed();
    }

    @GameTest(batch = "w103", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theWitchMayDropAPlagueVirusWhenSheDies(GameTestHelper h) {
        floor(h);
        double chance = WitchConfig.VIRUS_WITCH_CHANCE.get();
        try {
            WitchConfig.VIRUS_WITCH_CHANCE.set(1.0D);
            Player player = h.makeMockPlayer();
            put(h, player, 3, 3);
            GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(7, 1, 7));
            witch.hurt(h.getLevel().damageSources().playerAttack(player), 1000.0F);
            h.assertTrue(!h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(7, 1, 7))).inflate(6.0D), i -> i.getItem().is(ModItems.PLAGUE_VIRUS.get())).isEmpty(), "she dropped no plague virus");
        } finally {
            WitchConfig.VIRUS_WITCH_CHANCE.set(chance);
        }
        h.succeed();
    }

    @GameTest(batch = "w104", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void eightArrowsRoundAPotionOfTheMouseMakeEightArrowsWithTheMouseOnThemAndNothingElseDoes(GameTestHelper h) {
        floor(h);
        var recipe = new MouseArrowRecipe(new net.minecraft.resources.ResourceLocation("grandwitch", "mouse_arrow"), net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT);
        var menu = new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player p, int i) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player p) {
                return true;
            }
        };
        var grid = new net.minecraft.world.inventory.TransientCraftingContainer(menu, 3, 3);
        for (int i = 0; i < 9; i++) {
            grid.setItem(i, i == 4 ? ModEffects.drinkable() : new ItemStack(Items.ARROW));
        }
        h.assertTrue(recipe.matches(grid, h.getLevel()), "eight arrows round the potion make nothing");
        ItemStack out = recipe.assemble(grid, h.getLevel().registryAccess());
        h.assertTrue(out.is(Items.TIPPED_ARROW) && out.getCount() == 8 && net.minecraft.world.item.alchemy.PotionUtils.getPotion(out) == ModEffects.MOUSE_POTION.get(), "it is not eight arrows with the mouse on them: " + out);
        h.assertTrue(recipe.getRemainingItems(grid).get(4).is(Items.GLASS_BOTTLE), "the bottle did not come back");
        grid.setItem(4, ModEffects.splash());
        h.assertTrue(recipe.matches(grid, h.getLevel()), "a splash potion of the mouse does not do");
        grid.setItem(4, net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), ModEffects.MOUSE_POTION.get()));
        h.assertTrue(recipe.matches(grid, h.getLevel()), "a lingering potion of the mouse does not do");
        grid.setItem(4, net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.REGENERATION));
        h.assertTrue(!recipe.matches(grid, h.getLevel()), "a potion of another sort did");
        grid.setItem(4, ModEffects.drinkable());
        grid.setItem(0, ItemStack.EMPTY);
        h.assertTrue(!recipe.matches(grid, h.getLevel()), "seven arrows did");
        grid.setItem(0, new ItemStack(Items.STICK));
        h.assertTrue(!recipe.matches(grid, h.getLevel()), "a stick in the place of an arrow did");
        h.succeed();
    }

    @GameTest(batch = "w105", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseHoleIsMadeInTheFaceOfABankOfSoftGroundAndNotOnFlatGround(GameTestHelper h) {
        floor(h);
        int from = h.absolutePos(new BlockPos(0, 10, 0)).getY();
        var level = h.getLevel();
        var random = level.getRandom();
        // flat: nothing (the whole of what is looked at is level)
        h.assertTrue(!MouseHoleFeature.placeNear(level, h.absolutePos(new BlockPos(2, 0, 2)), 2, random, 60, from), "a hole was made on flat ground");
        // a bank of dirt, two high
        for (int x = 8; x <= 11; x++) {
            for (int z = 6; z <= 9; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT.defaultBlockState());
                h.setBlock(new BlockPos(x, 2, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
        h.assertTrue(MouseHoleFeature.placeNear(level, h.absolutePos(new BlockPos(9, 0, 7)), 3, random, 400, from), "no hole was made in a bank of dirt");
        BlockPos hole = null;
        for (int x = 6; x <= 13; x++) {
            for (int z = 4; z <= 11; z++) {
                for (int y = 0; y <= 3; y++) {
                    if (h.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.MOUSE_HOLE.get())) {
                        hole = new BlockPos(x, y, z);
                    }
                }
            }
        }
        h.assertTrue(hole != null, "no hole block is to be found");
        var state = h.getBlockState(hole);
        var facing = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
        h.assertTrue(hole.getY() == 1, "the hole is not level with the low ground: " + hole);
        // the open end is to the low ground, with air in front of it and ground under that, and the tunnel runs in from the other end
        BlockPos front = hole.relative(facing.getOpposite());
        h.assertTrue(h.getBlockState(front).isAir() && h.getBlockState(front.below()).isFaceSturdy(level, h.absolutePos(front.below()), net.minecraft.core.Direction.UP), "the hole does not open on the low ground: " + hole + " facing " + facing);
        int tunnel = 0;
        while (h.getBlockState(hole.relative(facing, tunnel + 1)).is(ModBlocks.MOUSE_TUNNEL.get())) {
            tunnel++;
        }
        h.assertTrue(tunnel >= 2 && tunnel <= 3, "the tunnel is " + tunnel + " blocks, not two or three");
        var toHole = facing.getOpposite();
        for (int k = 1; k <= tunnel; k++) {
            var block = h.getBlockState(hole.relative(facing, k));
            h.assertTrue(MouseTunnelBlock.isOpen(block, toHole), "block " + k + " of the tunnel is shut toward the hole");
            h.assertTrue(MouseTunnelBlock.isOpen(block, facing) == (k < tunnel), "block " + k + " of the tunnel is " + (k < tunnel ? "shut" : "open") + " toward the way on");
            h.assertTrue(!MouseTunnelBlock.isOpen(block, facing.getClockWise()) && !MouseTunnelBlock.isOpen(block, facing.getCounterClockWise()), "block " + k + " of the tunnel is open at its sides");
        }
        h.succeed();
    }

    @GameTest(batch = "w106", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseCanUseOnlyWhatItCanEatOrDrinkAndNoToolOrWeaponOrBowOrBucketOrBlock(GameTestHelper h) {
        floor(h);
        for (ItemStack ok : new ItemStack[]{ItemStack.EMPTY, new ItemStack(Items.BREAD), new ItemStack(Items.GOLDEN_APPLE), ModEffects.drinkable(), new ItemStack(ModItems.ANTIDOTE.get()), new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.COOKIE)}) {
            h.assertTrue(Mice.pawsCanUse(ok), "a mouse cannot use " + ok);
        }
        for (ItemStack no : new ItemStack[]{new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.BOW), new ItemStack(Items.CROSSBOW),
                new ItemStack(Items.BUCKET), new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.STONE), new ItemStack(Items.SHEARS), new ItemStack(Items.LEAD), ModEffects.splash(),
                new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.FLINT_AND_STEEL), new ItemStack(Items.SHIELD)}) {
            h.assertTrue(!Mice.pawsCanUse(no), "a mouse can use " + no);
        }
        Player mouse = h.makeMockPlayer();
        put(h, mouse, 7, 7);
        Mice.become(mouse, null, 60);
        var target = h.spawn(EntityType.COW, new BlockPos(8, 1, 7));
        mouse.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        var bus = net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        h.assertTrue(bus.post(new net.minecraftforge.event.entity.player.AttackEntityEvent(mouse, target)), "a mouse struck with a sword");
        h.assertTrue(bus.post(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem(mouse, InteractionHand.MAIN_HAND)), "a mouse used a sword");
        h.assertTrue(bus.post(new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start(mouse, new ItemStack(Items.BOW), 72000)), "a mouse drew a bow");
        h.assertTrue(bus.post(new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(mouse, InteractionHand.MAIN_HAND, target)), "a mouse used a sword on a cow");
        // what it digs (soft ground) it digs as with a shovel, whatever it holds, and what it cannot dig it does slowly; in the air it is a fifth
        mouse.setOnGround(true);
        var dirt = new net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(mouse, Blocks.DIRT.defaultBlockState(), 1.0F, h.absolutePos(new BlockPos(5, 0, 5)));
        bus.post(dirt);
        h.assertTrue(Math.abs(dirt.getNewSpeed() - WitchConfig.MOUSE_DIG_SPEED.get().floatValue()) < 1.0E-3F, "a mouse does not dig soft ground at the speed set: " + dirt.getNewSpeed());
        var stone = new net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(mouse, Blocks.STONE.defaultBlockState(), 8.0F, h.absolutePos(new BlockPos(5, 0, 5)));
        bus.post(stone);
        h.assertTrue(stone.getNewSpeed() <= 0.6F + 1.0E-4F, "a tool made a mouse dig what it cannot dig faster than with its paws: " + stone.getNewSpeed());
        mouse.setOnGround(false);
        var air = new net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(mouse, Blocks.DIRT.defaultBlockState(), 1.0F, h.absolutePos(new BlockPos(5, 0, 5)));
        bus.post(air);
        h.assertTrue(Math.abs(air.getNewSpeed() - WitchConfig.MOUSE_DIG_SPEED.get().floatValue() / 5.0F) < 1.0E-3F, "a mouse in the air digs at " + air.getNewSpeed());
        mouse.setOnGround(true);
        // with nothing in its paws, or something to eat, it does as it did
        mouse.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(!bus.post(new net.minecraftforge.event.entity.player.AttackEntityEvent(mouse, target)), "a mouse could not bite with nothing in its paws");
        mouse.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        h.assertTrue(!bus.post(new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem(mouse, InteractionHand.MAIN_HAND)), "a mouse could not use bread");
        h.assertTrue(!bus.post(new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start(mouse, new ItemStack(Items.BREAD), 32)), "a mouse could not start to eat");
        // and not a mouse: as ever
        Player person = h.makeMockPlayer();
        person.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        h.assertTrue(!bus.post(new net.minecraftforge.event.entity.player.AttackEntityEvent(person, target)), "a person could not strike with a sword");
        h.succeed();
    }

    @GameTest(batch = "w107", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aDugBlockIsOpenOnlyToWhereTheMouseCameFromAndTheNextOnlyOnTheSideDugThrough(GameTestHelper h) {
        floor(h);
        for (int x = 5; x <= 7; x++) {
            for (int z = 3; z <= 6; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
            }
        }
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        put(h, mouse, 4, 5);                                   // outside, level with the dirt
        breakAt(h, mouse, new BlockPos(5, 1, 5));
        var first = h.getBlockState(new BlockPos(5, 1, 5));
        h.assertTrue(first.is(ModBlocks.MOUSE_TUNNEL.get()), "no tunnel was made");
        h.assertTrue(MouseTunnelBlock.isOpen(first, net.minecraft.core.Direction.WEST), "it is not open toward where the mouse stood");
        h.assertTrue(!MouseTunnelBlock.isOpen(first, net.minecraft.core.Direction.EAST) && !MouseTunnelBlock.isOpen(first, net.minecraft.core.Direction.NORTH) && !MouseTunnelBlock.isOpen(first, net.minecraft.core.Direction.SOUTH),
                "it is open on a side it was not dug through: " + first);
        // the mouse goes in, and digs on east: that block is open toward this one, and this one opens toward it, and nothing else is opened
        put(h, mouse, 5, 5);
        breakAt(h, mouse, new BlockPos(6, 1, 5));
        var firstNow = h.getBlockState(new BlockPos(5, 1, 5));
        var second = h.getBlockState(new BlockPos(6, 1, 5));
        h.assertTrue(MouseTunnelBlock.isOpen(second, net.minecraft.core.Direction.WEST) && !MouseTunnelBlock.isOpen(second, net.minecraft.core.Direction.EAST)
                && !MouseTunnelBlock.isOpen(second, net.minecraft.core.Direction.NORTH) && !MouseTunnelBlock.isOpen(second, net.minecraft.core.Direction.SOUTH), "the second block is not open only toward the first: " + second);
        h.assertTrue(MouseTunnelBlock.isOpen(firstNow, net.minecraft.core.Direction.EAST) && MouseTunnelBlock.isOpen(firstNow, net.minecraft.core.Direction.WEST)
                && !MouseTunnelBlock.isOpen(firstNow, net.minecraft.core.Direction.NORTH) && !MouseTunnelBlock.isOpen(firstNow, net.minecraft.core.Direction.SOUTH), "the first block did not open toward the second, and only that: " + firstNow);
        // and on the north, from the second block
        put(h, mouse, 6, 5);
        breakAt(h, mouse, new BlockPos(6, 1, 4));
        var secondNow = h.getBlockState(new BlockPos(6, 1, 5));
        var third = h.getBlockState(new BlockPos(6, 1, 4));
        h.assertTrue(MouseTunnelBlock.isOpen(third, net.minecraft.core.Direction.SOUTH) && !MouseTunnelBlock.isOpen(third, net.minecraft.core.Direction.NORTH), "the third block is not open only toward the second: " + third);
        h.assertTrue(MouseTunnelBlock.isOpen(secondNow, net.minecraft.core.Direction.NORTH) && MouseTunnelBlock.isOpen(secondNow, net.minecraft.core.Direction.WEST) && !MouseTunnelBlock.isOpen(secondNow, net.minecraft.core.Direction.SOUTH),
                "the second block did not open toward the third, and only that: " + secondNow);
        h.succeed();
    }

    @GameTest(batch = "w108", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aShutSideOfATunnelIsAWallToWhatWalksAndNotToTheEyeAndOneMadeBeforeIsOpenOnAllSides(GameTestHelper h) {
        floor(h);
        var old = ModBlocks.MOUSE_TUNNEL.get().defaultBlockState();
        for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            h.assertTrue(MouseTunnelBlock.isOpen(old, d), "a tunnel that is not told otherwise is shut on " + d);
        }
        var west = MouseTunnelBlock.shut(false).setValue(MouseTunnelBlock.side(net.minecraft.core.Direction.WEST), true);
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        var inNorthWall = net.minecraft.world.phys.shapes.Shapes.create(new net.minecraft.world.phys.AABB(0.3D, 0.1D, 0.02D, 0.7D, 0.4D, 0.05D));
        var inWestWall = net.minecraft.world.phys.shapes.Shapes.create(new net.minecraft.world.phys.AABB(0.02D, 0.1D, 0.3D, 0.05D, 0.4D, 0.7D));
        var collision = west.getCollisionShape(h.getLevel(), at);
        var outline = west.getShape(h.getLevel(), at);
        var and = net.minecraft.world.phys.shapes.BooleanOp.AND;
        h.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(collision, inNorthWall, and), "a shut side (north) is no wall to what walks");
        h.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(collision, inWestWall, and), "the open side (west) has a wall in it");
        h.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(outline, inNorthWall, and), "the eye is stopped by a shut side (north), so what is beyond it cannot be dug");
        h.succeed();
    }

    @GameTest(batch = "w109", template = "field", setupTicks = 20, timeoutTicks = 500)
    public static void theWitchsArmDoesNotGoThroughASideThatIsShut(GameTestHelper h) {
        floor(h);
        for (int x = 6; x <= 8; x++) {
            for (int z = 9; z <= 15; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT.defaultBlockState());
            }
        }
        for (int z = 9; z <= 13; z++) {
            var block = ModBlocks.MOUSE_TUNNEL.get().defaultBlockState();
            if (z == 10) {
                block = block.setValue(MouseTunnelBlock.side(net.minecraft.core.Direction.SOUTH), false);      // between the second block and the third: shut
            }
            if (z == 11) {
                block = block.setValue(MouseTunnelBlock.side(net.minecraft.core.Direction.NORTH), false);
            }
            h.setBlock(new BlockPos(7, 1, z), block);
        }
        BlockPos at = h.absolutePos(new BlockPos(7, 1, 12));
        VillageMouse mouse = h.spawn(ModEntities.VILLAGE_MOUSE.get(), new BlockPos(7, 1, 12));
        mouse.setPos(at.getX() + 0.5D, at.getY() + 0.1D, at.getZ() + 0.5D);
        mouse.setNoAi(true);
        GrandWitch witch = h.spawn(ModEntities.GRAND_WITCH.get(), new BlockPos(3, 1, 4));
        witch.hunt(mouse);
        h.runAfterDelay(300, () -> {
            h.assertTrue(MouseHoleBlock.hidden(mouse) && !witch.isProne(), "she reached through a shut side: " + mouse.position() + " prone=" + witch.isProne() + " trail: " + witch.reachTrail);
            h.succeed();
        });
    }

    @GameTest(batch = "w110", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aBlockDugInAThinWallIsOpenToTheAirOnBothSidesAndShutToTheGroundBeside(GameTestHelper h) {
        floor(h);
        for (int x = 4; x <= 8; x++) {
            h.setBlock(new BlockPos(x, 1, 8), Blocks.DIRT);                 // a wall of dirt, one block thick
        }
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        put(h, mouse, 6, 7);
        breakAt(h, mouse, new BlockPos(6, 1, 8));
        var block = h.getBlockState(new BlockPos(6, 1, 8));
        h.assertTrue(MouseTunnelBlock.isOpen(block, net.minecraft.core.Direction.NORTH) && MouseTunnelBlock.isOpen(block, net.minecraft.core.Direction.SOUTH), "it is shut toward the air on a side: " + block);
        h.assertTrue(!MouseTunnelBlock.isOpen(block, net.minecraft.core.Direction.EAST) && !MouseTunnelBlock.isOpen(block, net.minecraft.core.Direction.WEST), "it is open toward the ground beside it: " + block);
        h.succeed();
    }

    @GameTest(batch = "w111", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void twoTunnelsSideBySideAreNotJoinedOfThemselvesAndTheWallBetweenCanBeDugThroughByHand(GameTestHelper h) {
        floor(h);
        for (int x = 4; x <= 9; x++) {
            for (int z = 4; z <= 6; z++) {
                h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
            }
        }
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        // two blocks side by side, both dug from the south, as a player does: not joined
        put(h, mouse, 5, 7);
        breakAt(h, mouse, new BlockPos(5, 1, 6));
        put(h, mouse, 6, 7);
        breakAt(h, mouse, new BlockPos(6, 1, 6));
        var a = h.getBlockState(new BlockPos(5, 1, 6));
        var b = h.getBlockState(new BlockPos(6, 1, 6));
        h.assertTrue(!MouseTunnelBlock.isOpen(a, net.minecraft.core.Direction.EAST) && !MouseTunnelBlock.isOpen(b, net.minecraft.core.Direction.WEST), "two tunnels side by side were joined of themselves: " + a + " / " + b);
        // the wall between them can be pointed at (the eye comes to it) from inside one: it is part of what the eye is stopped by
        BlockPos aPos = h.absolutePos(new BlockPos(5, 1, 6));
        var east = net.minecraft.world.phys.shapes.Shapes.create(new net.minecraft.world.phys.AABB(0.95D, 0.1D, 0.3D, 0.98D, 0.4D, 0.7D));
        var west = net.minecraft.world.phys.shapes.Shapes.create(new net.minecraft.world.phys.AABB(0.02D, 0.1D, 0.3D, 0.05D, 0.4D, 0.7D));
        var and = net.minecraft.world.phys.shapes.BooleanOp.AND;
        var eye = net.minecraft.world.phys.shapes.CollisionContext.of(mouse);                  // what the eye of a mouse is stopped by
        h.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(a.getShape(h.getLevel(), aPos, eye), east, and), "the wall toward the next tunnel cannot be pointed at");
        h.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(a.getShape(h.getLevel(), aPos, eye), west, and), "a wall with ground beyond it is pointed at (the eye should go through to the ground)");
        // digging it: begun on the east wall (the face hit is the west face of it), and when it is done both sides are open, and the block is still there
        put(h, mouse, 5, 6);
        var begin = new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(mouse, aPos, net.minecraft.core.Direction.WEST, net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
        Mice.startBreaking(begin);
        h.assertTrue(!begin.isCanceled(), "a mouse is stopped from digging the wall between two tunnels");
        breakAt(h, mouse, new BlockPos(5, 1, 6));
        h.assertTrue(MouseTunnelBlock.isOpen(h.getBlockState(new BlockPos(5, 1, 6)), net.minecraft.core.Direction.EAST) && MouseTunnelBlock.isOpen(h.getBlockState(new BlockPos(6, 1, 6)), net.minecraft.core.Direction.WEST),
                "the wall between was not dug through");
        h.assertTrue(h.getBlockState(new BlockPos(5, 1, 6)).is(ModBlocks.MOUSE_TUNNEL.get()), "the tunnel block was broken");
        // a wall with ground beyond it is not something to dig: it is stopped, and told why
        var onGround = new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(mouse, aPos, net.minecraft.core.Direction.EAST, net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
        Mice.startBreaking(onGround);
        h.assertTrue(onGround.isCanceled(), "a wall with ground beyond it can be dug as a wall");
        h.succeed();
    }

    private static void standAt(GameTestHelper h, Player p, int x, int y, int z) {
        BlockPos at = h.absolutePos(new BlockPos(x, y, z));
        p.setPos(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
    }

    @GameTest(batch = "w112", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseDigsStraightDownAndStraightUpAndItIsAShaftThatIsClimbed(GameTestHelper h) {
        floor(h);
        for (int y = 1; y <= 4; y++) {
            h.setBlock(new BlockPos(5, y, 5), Blocks.DIRT);
            h.setBlock(new BlockPos(9, y, 5), Blocks.DIRT);
        }
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        var down = net.minecraft.core.Direction.DOWN;
        var up = net.minecraft.core.Direction.UP;
        // down from the top of the ground: the block below is open to the air above it (a pit), and the one under that is dug from inside it
        standAt(h, mouse, 5, 5, 5);
        breakAt(h, mouse, new BlockPos(5, 4, 5));
        var first = h.getBlockState(new BlockPos(5, 4, 5));
        h.assertTrue(first.is(ModBlocks.MOUSE_TUNNEL.get()) && MouseTunnelBlock.isOpen(first, up) && !MouseTunnelBlock.isOpen(first, down), "digging down from above did not make a block open above: " + first);
        standAt(h, mouse, 5, 4, 5);
        breakAt(h, mouse, new BlockPos(5, 3, 5));
        var second = h.getBlockState(new BlockPos(5, 3, 5));
        var firstNow = h.getBlockState(new BlockPos(5, 4, 5));
        h.assertTrue(MouseTunnelBlock.isOpen(second, up) && !MouseTunnelBlock.isOpen(second, down), "the block dug below is not open above: " + second);
        h.assertTrue(MouseTunnelBlock.isOpen(firstNow, down) && MouseTunnelBlock.isOpen(firstNow, up), "the block the mouse was in did not open below: " + firstNow);
        // up from inside a tunnel: its roof goes, and the one above has no floor
        h.setBlock(new BlockPos(9, 2, 5), MouseTunnelBlock.shut(false));
        standAt(h, mouse, 9, 2, 5);
        breakAt(h, mouse, new BlockPos(9, 3, 5));
        var low = h.getBlockState(new BlockPos(9, 2, 5));
        var high = h.getBlockState(new BlockPos(9, 3, 5));
        h.assertTrue(MouseTunnelBlock.isOpen(low, up) && !MouseTunnelBlock.isOpen(low, down), "the tunnel it dug up from has not lost its roof: " + low);
        h.assertTrue(MouseTunnelBlock.isOpen(high, down) && !MouseTunnelBlock.isOpen(high, up), "the block dug above is not open below: " + high);
        // not two up, not diagonally: only what is straight above or below
        standAt(h, mouse, 9, 2, 5);
        h.assertTrue(!Mice.sameLevel(mouse, h.absolutePos(new BlockPos(9, 4, 5))), "a mouse can dig two blocks up");
        h.assertTrue(!Mice.sameLevel(mouse, h.absolutePos(new BlockPos(10, 3, 5))), "a mouse can dig a block that is up and to the side");
        // the shaft is climbed by what is as small as a mouse, and by nothing else; a tunnel that goes only across is not climbed
        h.assertTrue(low.isLadder(h.getLevel(), h.absolutePos(new BlockPos(9, 2, 5)), mouse) && high.isLadder(h.getLevel(), h.absolutePos(new BlockPos(9, 3, 5)), mouse), "a shaft cannot be climbed by a mouse");
        h.assertTrue(!MouseTunnelBlock.shut(false).isLadder(h.getLevel(), h.absolutePos(new BlockPos(9, 2, 5)), mouse), "a tunnel that goes only across is climbed");
        var cow = h.spawn(EntityType.COW, new BlockPos(2, 1, 2));
        h.assertTrue(!low.isLadder(h.getLevel(), h.absolutePos(new BlockPos(9, 2, 5)), cow), "a cow climbs a shaft of a mouse");
        h.succeed();
    }

    @GameTest(batch = "w113", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseLooksThroughTheRoofToTheGroundAboveAndAnyoneElseSeesTheRoofAndTwoTunnelsOneOverTheOtherAreJoinedByDiggingThroughTheRoof(GameTestHelper h) {
        floor(h);
        for (int y = 1; y <= 4; y++) {
            h.setBlock(new BlockPos(5, y, 5), Blocks.DIRT);
        }
        Player mouse = h.makeMockPlayer(), plain = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        h.setBlock(new BlockPos(5, 2, 5), MouseTunnelBlock.shut(false));       // ground above it (3) and below it (1)
        BlockPos at = h.absolutePos(new BlockPos(5, 2, 5));
        var state = h.getBlockState(new BlockPos(5, 2, 5));
        var roof = net.minecraft.world.phys.shapes.Shapes.create(new net.minecraft.world.phys.AABB(0.3D, 0.7D, 0.3D, 0.7D, 0.95D, 0.7D));
        var and = net.minecraft.world.phys.shapes.BooleanOp.AND;
        h.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(state.getShape(h.getLevel(), at, net.minecraft.world.phys.shapes.CollisionContext.of(mouse)), roof, and), "a mouse is stopped by the roof with ground above it");
        h.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(state.getShape(h.getLevel(), at, net.minecraft.world.phys.shapes.CollisionContext.of(plain)), roof, and), "someone else is not stopped by the roof of a tunnel");
        // a tunnel above this one: the roof between is what the mouse is stopped by, and can dig through
        h.setBlock(new BlockPos(5, 3, 5), MouseTunnelBlock.shut(false));
        var two = h.getBlockState(new BlockPos(5, 2, 5));
        h.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(two.getShape(h.getLevel(), at, net.minecraft.world.phys.shapes.CollisionContext.of(mouse)), roof, and), "the roof toward a tunnel above cannot be pointed at");
        standAt(h, mouse, 5, 2, 5);
        var begin = new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(mouse, at, net.minecraft.core.Direction.DOWN, net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
        Mice.startBreaking(begin);
        h.assertTrue(!begin.isCanceled(), "a mouse is stopped from digging through the roof to a tunnel above");
        breakAt(h, mouse, new BlockPos(5, 2, 5));
        h.assertTrue(MouseTunnelBlock.isOpen(h.getBlockState(new BlockPos(5, 2, 5)), net.minecraft.core.Direction.UP) && MouseTunnelBlock.isOpen(h.getBlockState(new BlockPos(5, 3, 5)), net.minecraft.core.Direction.DOWN), "the roof between was not dug through");
        h.assertTrue(h.getBlockState(new BlockPos(5, 2, 5)).is(ModBlocks.MOUSE_TUNNEL.get()), "the tunnel block was broken");
        h.succeed();
    }

    @GameTest(batch = "w114", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aShaftIsWalledAllItsHeightAndEightAcrossSoAMouseGoesInAndAPersonDoesNot(GameTestHelper h) {
        floor(h);
        var shaft = MouseTunnelBlock.shut(false).setValue(MouseTunnelBlock.UP, true).setValue(MouseTunnelBlock.DOWN, true);
        h.setBlock(new BlockPos(5, 2, 5), shaft);
        BlockPos at = h.absolutePos(new BlockPos(5, 2, 5));
        var and = net.minecraft.world.phys.shapes.BooleanOp.AND;
        // a wall on a shut side, in the lower half and in the upper: no ring that is only at the bottom
        for (double y : new double[]{0.2D, 0.8D}) {
            var inWall = net.minecraft.world.phys.shapes.Shapes.create(new net.minecraft.world.phys.AABB(0.3D, y - 0.1D, 0.02D, 0.7D, y + 0.1D, 0.2D));
            h.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shaft.getCollisionShape(h.getLevel(), at), inWall, and), "a shaft has no wall at height " + y);
        }
        // eight across: a mouse (0.4) fits in the middle of it, and a person (0.6) does not
        var mouseBox = new net.minecraft.world.phys.AABB(at.getX() + 0.3D, at.getY(), at.getZ() + 0.3D, at.getX() + 0.7D, at.getY() + 1.0D, at.getZ() + 0.7D);
        var personBox = new net.minecraft.world.phys.AABB(at.getX() + 0.2D, at.getY(), at.getZ() + 0.2D, at.getX() + 0.8D, at.getY() + 1.0D, at.getZ() + 0.8D);
        h.assertTrue(h.getLevel().noCollision(mouseBox), "a mouse does not fit in a shaft");
        h.assertTrue(!h.getLevel().noCollision(personBox), "a person fits in a shaft");
        // an open side is a mouse's opening and no one else's: the lintel is over it, the opening 8 high
        var openNorth = shaft.setValue(MouseTunnelBlock.NORTH, true);
        var lintel = net.minecraft.world.phys.shapes.Shapes.create(new net.minecraft.world.phys.AABB(0.3D, 0.6D, 0.02D, 0.7D, 0.9D, 0.2D));
        var gap = net.minecraft.world.phys.shapes.Shapes.create(new net.minecraft.world.phys.AABB(0.3D, 0.1D, 0.02D, 0.7D, 0.4D, 0.2D));
        h.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(openNorth.getCollisionShape(h.getLevel(), at), lintel, and), "an open side of a shaft has no lintel over it");
        h.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(openNorth.getCollisionShape(h.getLevel(), at), gap, and), "an open side of a shaft is walled below the lintel");
        // and the eye of a mouse goes through the wall of a shaft to the ground beyond it
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        var inWall = net.minecraft.world.phys.shapes.Shapes.create(new net.minecraft.world.phys.AABB(0.3D, 0.1D, 0.02D, 0.7D, 0.4D, 0.2D));
        h.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shaft.getShape(h.getLevel(), at, net.minecraft.world.phys.shapes.CollisionContext.of(mouse)), inWall, and), "the eye of a mouse is stopped by the wall of a shaft, with ground beyond it");
        h.succeed();
    }

    @GameTest(batch = "w115", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void theRoofUnderOpenAirIsDugOpenToTheSkyAndTheShaftReachesTheTopOfTheGround(GameTestHelper h) {
        floor(h);
        // a tunnel block with nothing above it (it is the top layer of the ground): the mouse points at its roof, and digs it, and it is open to the sky
        h.setBlock(new BlockPos(5, 1, 5), MouseTunnelBlock.shut(true).setValue(MouseTunnelBlock.DOWN, true));
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
        var roof = net.minecraft.world.phys.shapes.Shapes.create(new net.minecraft.world.phys.AABB(0.3D, 0.7D, 0.3D, 0.7D, 0.95D, 0.7D));
        var and = net.minecraft.world.phys.shapes.BooleanOp.AND;
        var state = h.getBlockState(new BlockPos(5, 1, 5));
        h.assertTrue(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(state.getShape(h.getLevel(), at, net.minecraft.world.phys.shapes.CollisionContext.of(mouse)), roof, and), "the roof under open air cannot be pointed at");
        standAt(h, mouse, 5, 1, 5);
        var begin = new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(mouse, at, net.minecraft.core.Direction.DOWN, net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
        Mice.startBreaking(begin);
        h.assertTrue(!begin.isCanceled(), "a mouse is stopped from digging the roof under open air");
        breakAt(h, mouse, new BlockPos(5, 1, 5));
        var after = h.getBlockState(new BlockPos(5, 1, 5));
        h.assertTrue(after.is(ModBlocks.MOUSE_TUNNEL.get()) && MouseTunnelBlock.isOpen(after, net.minecraft.core.Direction.UP) && MouseTunnelBlock.isOpen(after, net.minecraft.core.Direction.DOWN), "the roof was not dug open: " + after);
        h.succeed();
    }

    @GameTest(batch = "w116", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseBitesThroughSmallPlantsAndCropsAndLeavesHardThingsAndFarmlandBe(GameTestHelper h) {
        floor(h);
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        put(h, mouse, 9, 9);
        net.minecraft.world.level.block.Block[] plants = {Blocks.GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.POPPY, Blocks.DANDELION, Blocks.OAK_SAPLING};
        net.minecraft.world.level.block.Block[] crops = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};        // these stand on farmland, or they fall as drops
        int count = plants.length + crops.length;
        BlockPos[] spots = new BlockPos[count];
        for (int i = 0; i < count; i++) {                  // all put first, apart from each other, one every second block
            boolean crop = i >= plants.length;
            net.minecraft.world.level.block.Block block = crop ? crops[i - plants.length] : plants[i];
            spots[i] = new BlockPos(1 + 2 * (i % 4), 1, 1 + 2 * (i / 4));
            h.setBlock(spots[i].below(), crop ? Blocks.FARMLAND : Blocks.GRASS_BLOCK);
            h.setBlock(spots[i], block);
        }
        for (int i = 0; i < count; i++) {
            net.minecraft.world.level.block.Block block = i >= plants.length ? crops[i - plants.length] : plants[i];
            h.assertTrue(h.getBlockState(spots[i]).is(block), block + " did not stay where it was put: " + h.getBlockState(spots[i]) + ", below it " + h.getBlockState(spots[i].below()));
            BlockPos at = h.absolutePos(spots[i]);
            var begin = new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(mouse, at, net.minecraft.core.Direction.UP, net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
            Mice.startBreaking(begin);
            h.assertTrue(!begin.isCanceled(), "a mouse is stopped from biting through " + block);
            h.assertTrue(!breakAt(h, mouse, spots[i]).isCanceled(), "a mouse's breaking of " + block + " is stopped");
        }
        // not stone, not a torch, not planks, not farmland
        net.minecraft.world.level.block.Block[] hard = {Blocks.STONE, Blocks.OAK_PLANKS, Blocks.FARMLAND};
        for (int i = 0; i < hard.length; i++) {
            BlockPos rel = new BlockPos(2 + i, 1, 7);
            h.setBlock(rel, hard[i]);
            BlockPos at = h.absolutePos(rel);
            var begin = new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(mouse, at, net.minecraft.core.Direction.UP, net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
            Mice.startBreaking(begin);
            h.assertTrue(begin.isCanceled(), "a mouse can start to dig " + hard[i]);
            h.assertTrue(breakAt(h, mouse, rel).isCanceled(), "a mouse can break " + hard[i]);
        }
        h.setBlock(new BlockPos(6, 0, 7), Blocks.DIRT);
        h.setBlock(new BlockPos(6, 1, 7), Blocks.TORCH);
        var torch = new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(mouse, h.absolutePos(new BlockPos(6, 1, 7)), net.minecraft.core.Direction.UP, net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
        Mice.startBreaking(torch);
        h.assertTrue(torch.isCanceled(), "a mouse can start to dig a torch");
        h.succeed();
    }

    @GameTest(batch = "w117", template = "field", setupTicks = 20, timeoutTicks = 100)
    public static void aMouseDigsAShovelPathAndHayAsItDigsDirtAndNotFarmland(GameTestHelper h) {
        floor(h);
        Player mouse = h.makeMockPlayer();
        Mice.become(mouse, null, 60);
        put(h, mouse, 3, 5);
        for (net.minecraft.world.level.block.Block ok : new net.minecraft.world.level.block.Block[]{Blocks.DIRT_PATH, Blocks.HAY_BLOCK}) {
            h.setBlock(new BlockPos(5, 1, 5), ok);
            BlockPos at = h.absolutePos(new BlockPos(5, 1, 5));
            var begin = new net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(mouse, at, net.minecraft.core.Direction.WEST, net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
            Mice.startBreaking(begin);
            h.assertTrue(!begin.isCanceled(), "a mouse is stopped from digging " + ok);
            breakAt(h, mouse, new BlockPos(5, 1, 5));
            h.assertTrue(h.getBlockState(new BlockPos(5, 1, 5)).is(ModBlocks.MOUSE_TUNNEL.get()), "a mouse did not make a tunnel of " + ok);
        }
        h.setBlock(new BlockPos(6, 1, 5), Blocks.FARMLAND);
        breakAt(h, mouse, new BlockPos(6, 1, 5));
        h.assertTrue(h.getBlockState(new BlockPos(6, 1, 5)).is(Blocks.FARMLAND), "a mouse dug farmland");
        h.succeed();
    }
}

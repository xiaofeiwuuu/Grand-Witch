package com.xiaofeiwu.grandwitch;

import net.minecraftforge.common.ForgeConfigSpec;

public final class WitchConfig {

    static final ForgeConfigSpec SPEC;
    static final ForgeConfigSpec.BooleanValue ENABLED;
    static final ForgeConfigSpec.DoubleValue CHANCE;
    static final ForgeConfigSpec.IntValue CHECK_SECONDS;
    static final ForgeConfigSpec.IntValue STAY_SECONDS;
    static final ForgeConfigSpec.IntValue MOUSE_SECONDS;
    static final ForgeConfigSpec.DoubleValue MOUSE_SPEED;
    static final ForgeConfigSpec.BooleanValue MILK_CURES;
    static final ForgeConfigSpec.DoubleValue STOMP_DAMAGE;
    static final ForgeConfigSpec.BooleanValue CATS_HUNT;
    static final ForgeConfigSpec.IntValue SQUEAK_SECONDS;
    static final ForgeConfigSpec.BooleanValue TURNS_CHILDREN;
    static final ForgeConfigSpec.BooleanValue WITCH_REACH;
    static final ForgeConfigSpec.BooleanValue HUT_RESPAWN;
    static final ForgeConfigSpec.BooleanValue WORLD_HOLES;
    static final ForgeConfigSpec.DoubleValue WORLD_HOLE_CHANCE;
    static final ForgeConfigSpec.DoubleValue VIRUS_CHANCE;
    static final ForgeConfigSpec.DoubleValue VIRUS_WITCH_CHANCE;
    static final ForgeConfigSpec.DoubleValue HUT_RESPAWN_CHANCE;
    static final ForgeConfigSpec.BooleanValue DANGER_SENSE;

    /** Whether a mouse sees what is dangerous about it glowing (on the player's own side of it). */
    public static boolean dangerSense() {
        return DANGER_SENSE.get();
    }
    static final ForgeConfigSpec.BooleanValue TURNS_BABY_ZOMBIES;
    static final ForgeConfigSpec.BooleanValue NIGHT_VISION;
    static final ForgeConfigSpec.BooleanValue HUT;
    static final ForgeConfigSpec.BooleanValue WILD_MICE;
    static final ForgeConfigSpec.BooleanValue POTIONS;
    static final ForgeConfigSpec.IntValue POTION_SECONDS;
    static final ForgeConfigSpec.BooleanValue ALARM;
    static final ForgeConfigSpec.BooleanValue HUT_ALARM;
    static final ForgeConfigSpec.BooleanValue BROOM;
    static final ForgeConfigSpec.DoubleValue BROOM_SPEED;
    static final ForgeConfigSpec.DoubleValue SUSPICION;
    static final ForgeConfigSpec.IntValue WITCH_MOUSE_SECONDS;
    static final ForgeConfigSpec.IntValue W_GIFT;
    static final ForgeConfigSpec.IntValue W_POTION;
    static final ForgeConfigSpec.IntValue W_CURSE;
    static final ForgeConfigSpec.IntValue W_NANNY;
    static final ForgeConfigSpec.IntValue POTION_PRICE;
    static final ForgeConfigSpec.DoubleValue POTION_POISON;
    static final ForgeConfigSpec.IntValue CURSE_SECONDS;
    static final ForgeConfigSpec.IntValue NANNY_MAX;
    static final ForgeConfigSpec.IntValue PLAYER_RANGE;
    static final ForgeConfigSpec.DoubleValue POISON_CHANCE;
    static final ForgeConfigSpec.BooleanValue BREW;
    static final ForgeConfigSpec.BooleanValue BREW_NEEDS_FIRE;
    static final ForgeConfigSpec.DoubleValue ADULT_CHANCE;
    static final ForgeConfigSpec.BooleanValue TURNS_ADULTS;
    static final ForgeConfigSpec.IntValue ADULTS_PER_VISIT;
    static final ForgeConfigSpec.BooleanValue BLINK;
    static final ForgeConfigSpec.IntValue CHILD_MOUSE_SECONDS;
    static final ForgeConfigSpec.BooleanValue CHILD_MOUSE_CAN_DIE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        ENABLED = b.comment("Whether the Grand Witch ever comes by herself. (The spawn egg and /grandwitch witch still work.)").define("enabled", true);
        CHECK_SECONDS = b.comment("How often the game looks at each player to see whether she comes.").defineInRange("checkSeconds", 60, 10, 3600);
        CHANCE = b.comment("The chance, at each look, that she comes to a player who has a child villager within 48 blocks and no Grand Witch within 160.")
                .defineInRange("chance", 0.3, 0.0, 1.0);
        STAY_SECONDS = b.comment("How long she stays about if nothing comes of it, before she is gone in a puff of smoke.").defineInRange("staySeconds", 480, 60, 7200);
        MOUSE_SECONDS = b.comment("How long a player stays a mouse after eating what she gave. Killing the witch that did it, or drinking milk, ends it sooner.")
                .defineInRange("mouseSeconds", 240, 10, 7200);
        MOUSE_SPEED = b.comment("How much faster a mouse moves: 0.6 is sixty percent faster.").defineInRange("mouseSpeed", 0.6, 0.0, 3.0);
        MILK_CURES = b.comment("Whether a bucket of milk turns a mouse back into a player. (Off: the antidote is the way, or the death of the witch that did it, or waiting. Milk always works on a child that is a mouse.)").define("milkCures", false);
        STOMP_DAMAGE = b.comment("Health a stamp of the witch's foot takes off a mouse (2 is one heart), about every 0.7 seconds while she is on it.").defineInRange("stompDamage", 6.0, 0.0, 40.0);
        CATS_HUNT = b.comment("Whether cats (tame ones too, except a mouse's own) go for a player who is a mouse.").define("catsHunt", true);
        SQUEAK_SECONDS = b.comment("About how often a mouse squeaks by itself, in seconds (a little more or less each time). Every cat within 32 blocks turns to it. A mouse that is crouching keeps quiet. It also squeaks when it is hurt.")
                .defineInRange("squeakSeconds", 30, 5, 600);
        TURNS_CHILDREN = b.comment("Whether the witch turns child villagers into mice (about every minute, one at a time, when she is by one).").define("turnsChildren", true);
        DANGER_SENSE = b.comment("Whether a player that is a mouse sees what is dangerous to it (the witch, cats, monsters) glowing, through walls, within 20 blocks. Only the mouse sees it.").define("dangerSense", true);
        VIRUS_CHANCE = b.comment("The chance a wild mouse (not one that was a villager or the like) that a player kills drops a plague virus; each level of looting adds 0.1.").defineInRange("plagueVirusChance", 0.25, 0.0, 1.0);
        VIRUS_WITCH_CHANCE = b.comment("The chance the witch drops a plague virus when she dies.").defineInRange("plagueVirusWitchChance", 0.5, 0.0, 1.0);
        WORLD_HOLES = b.comment("Whether mouse holes come up on their own in the world, in the side of banks of soft ground, in the plains, the forests and the hills (only in the chunks made after it is on).").define("worldMouseHoles", true);
        WORLD_HOLE_CHANCE = b.comment("The chance, in each chunk, that a hole is tried for (one is not always found a place: a bank of soft ground is wanted).").defineInRange("worldMouseHoleChance", 0.03, 0.0, 1.0);
        HUT_RESPAWN = b.comment("Whether a house whose witch is dead (or gone) gets a new one, of itself, now and then, while someone is near it.").define("hutRespawn", true);
        HUT_RESPAWN_CHANCE = b.comment("The chance, each minute, that a witch comes to a house of hers that stands empty, with a player within 96 blocks of it and no other witch within 160 (0.02 is about one in fifty minutes).").defineInRange("hutRespawnChance", 0.02, 0.0, 1.0);
        WITCH_REACH = b.comment("Whether the witch lies down at the mouth of a mouse tunnel and reaches into it (up to 5 blocks of tunnel along) to pull out a mouse she was after.").define("witchReach", true);
        TURNS_BABY_ZOMBIES = b.comment("Whether the witch, whoever she is, turns baby zombies near her into mice (for good, unless the witch dies or they are given milk).").define("turnsBabyZombies", true);
        CHILD_MOUSE_SECONDS = b.comment("How long a child stays a mouse. Milk, given to it, or the death of the witch that did it, ends it sooner.").defineInRange("childMouseSeconds", 300, 10, 7200);
        CHILD_MOUSE_CAN_DIE = b.comment("Whether a child that is a mouse can be killed (by cats, by the witch's foot, by anything). If not, it takes no harm, and the child is only lost by waiting.").define("childMouseCanDie", true);
        NIGHT_VISION = b.comment("Whether a mouse sees in the dark.").define("mouseNightVision", true);
        HUT = b.comment("Whether the witch's little house, with the antidote in a chest in it, is built where she comes (on flat open ground only, and not where there is one already within 160 blocks).").define("witchHut", true);
        WILD_MICE = b.comment("Whether wild mice come up on their own (on the ground, in any biome of the overworld, a few now and then), and out of mouse holes.").define("wildMice", true);
        POTIONS = b.comment("Whether the witch, when she has shown what she is, throws potions that make a mouse of whoever they land on (a shield held up keeps one off).").define("witchPotions", true);
        POTION_SECONDS = b.comment("About how long between two potions, in seconds, while a player who is not a mouse is in her sight within 16 blocks.").defineInRange("witchPotionSeconds", 9, 3, 120);
        ALARM = b.comment("Whether the bells within 48 blocks of the witch ring when she shows what she is, and she shines for ten seconds.").define("villageAlarm", true);
        HUT_ALARM = b.comment("Whether someone who goes into the witch's house sets her on them: she shows what she is, runs back, and goes for them for thirty seconds. Her house is also where she lives: when she has nothing to do she keeps within 40 blocks of it.").define("hutAlarm", true);
        BLINK = b.comment("Whether a witch who is more than 40 blocks from her house when someone goes into it is at its door at once, in a puff of smoke, instead of running all the way.").define("witchBlink", true);
        ADULT_CHANCE = b.comment("The same, for a player who has grown villagers (or a wandering trader) within 48 blocks and no child. Only if turnsAdults is on.").defineInRange("adultChance", 0.05, 0.0, 1.0);
        TURNS_ADULTS = b.comment("Whether the witch turns grown villagers and wandering traders into mice too, and not only children. (A grown villager she turns is lost, with its trades, if a cat or her foot gets it and childMouseCanDie is on.)").define("turnsAdults", true);
        ADULTS_PER_VISIT = b.comment("How many grown villagers one witch turns in all, so that one visit does not empty a trading hall. Children are not counted.").defineInRange("adultsPerVisit", 1, 0, 20);
        BREW = b.comment("Whether the antidote can be brewed in a cauldron: a golden carrot, a spider eye and a glass bottle thrown into a cauldron that has water in it make two (the water goes down a level).").define("cauldronBrew", true);
        BREW_NEEDS_FIRE = b.comment("Whether the cauldron has to have a fire under it (a lit campfire, fire, lava or magma) for the brew.").define("brewNeedsFire", true);
        POISON_CHANCE = b.comment("The chance that what the witch's gift turns out to be, at the moment it is eaten, is the mouse (and not a reward: a golden apple, emeralds, regeneration and absorption, speed, or luck). The gifts are all alike, and the outcome is not decided until it is eaten, so there is no telling them apart. 1.0 for always the mouse, as it was.")
                .defineInRange("giftPoisonChance", 0.5, 0.0, 1.0);
        PLAYER_RANGE = b.comment("How near a player has to be, in blocks, to children (or grown villagers) for the witch to come to them. The children are looked for in the whole of the part of the world that is loaded, in groups, and not only round a player. 0: no player has to be near (there still has to be a player somewhere, or nothing of the world is awake).")
                .defineInRange("witchPlayerRange", 128, 0, 1024);
        W_GIFT = b.comment("How likely she is to be the girl with a basket (who gives cakes), against the other three: this, and the next three, are shares of a whole.").defineInRange("kindGift", 35, 0, 1000);
        W_POTION = b.comment("... the old woman who sells potions.").defineInRange("kindPotion", 25, 0, 1000);
        W_CURSE = b.comment("... the trader who gives a cursed gift.").defineInRange("kindCurse", 20, 0, 1000);
        W_NANNY = b.comment("... the nanny who leads the children away (she is only the nanny where there are children within 48 blocks).").defineInRange("kindNanny", 20, 0, 1000);
        POTION_PRICE = b.comment("Emeralds the old woman asks for a potion.").defineInRange("potionPrice", 2, 1, 64);
        POTION_POISON = b.comment("The chance that a potion the old woman sold is the potion of the mouse, in addition to what it looks to be (decided when it is drunk).").defineInRange("potionPoisonChance", 0.4, 0.0, 1.0);
        CURSE_SECONDS = b.comment("How long, in all, a cursed gift can be carried before it makes a mouse of the one who carries it. It is told of at a third and at two thirds of it.").defineInRange("curseSeconds", 90, 10, 3600);
        NANNY_MAX = b.comment("How many children the nanny gathers at most.").defineInRange("nannyMaxChildren", 3, 1, 12);
        SUSPICION = b.comment("The chance that the witch, offered a spiked bit of food while she is dressed up, smells something and shows what she is (and is on the one who offered it), instead of eating it.").defineInRange("witchSuspicion", 0.3, 0.0, 1.0);
        WITCH_MOUSE_SECONDS = b.comment("How long the witch is a mouse, when she has been made one (by what she ate, or a splash potion of the mouse), before she is herself again with the health she had. As a mouse she has 10 health, is quick, flees, and has none of her tricks.").defineInRange("witchMouseSeconds", 60, 5, 600);
        BROOM = b.comment("Whether the witch, when she has shown what she is and is going after someone more than 6 blocks away, rides a broom, low over the ground, instead of running; she gets off when she is near. She leaves a broom, now and then, when she dies.").define("witchBroom", true);
        BROOM_SPEED = b.comment("How fast the witch flies on her broom, in blocks a tick (0.33 is about 6.6 blocks a second: a little more than she runs when she is in a rage, and not as much as a mouse sprinting).").defineInRange("witchBroomSpeed", 0.33, 0.05, 1.5);
        SPEC = b.build();
    }

    private WitchConfig() {
    }
}

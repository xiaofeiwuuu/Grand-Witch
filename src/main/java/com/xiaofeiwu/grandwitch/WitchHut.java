package com.xiaofeiwu.grandwitch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The witch's little house, built where she comes and nowhere that is not clear: 7 by 7, spruce, a door on the south side, a cauldron, shelves,
 * a brewing stand, and a chest with antidote in it. It is built by hand (block by block) and not as a structure of the world, so that where it goes
 * can be looked at first: on ground that is flat enough and has nothing on it that someone made.
 */
@Mod.EventBusSubscriber(modid = GrandWitchMod.MODID)
public final class WitchHut {

    private WitchHut() {
    }

    /** A house, and the witch who lives in it, if there is one. */
    record Hut(BlockPos pos, UUID witch) {
    }

    /** The huts there are, so that one is not built beside another, and whose they are. */
    static final class Huts extends SavedData {

        private final ListTag list = new ListTag();            // each: {Pos: long, Witch: uuid (if any)}

        static Huts get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(Huts::load, Huts::new, "grandwitch_huts");
        }

        static Huts load(CompoundTag tag) {
            Huts huts = new Huts();
            huts.list.addAll(tag.getList("Entries", 10));
            for (var old : tag.getList("Huts", 4)) {                 // as they were kept before they had a witch
                CompoundTag e = new CompoundTag();
                e.putLong("Pos", ((LongTag) old).getAsLong());
                huts.list.add(e);
            }
            return huts;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.put("Entries", list);
            return tag;
        }

        void add(BlockPos pos) {
            CompoundTag e = new CompoundTag();
            e.putLong("Pos", pos.asLong());
            list.add(e);
            setDirty();
        }

        /** Test hook: no house is on record. */
        void forgetAll() {
            list.clear();
            setDirty();
        }

        void bind(BlockPos pos, UUID witch) {
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                if (e.getLong("Pos") == pos.asLong()) {
                    e.putUUID("Witch", witch);
                    setDirty();
                }
            }
        }

        List<Hut> all() {
            List<Hut> out = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                out.add(new Hut(BlockPos.of(e.getLong("Pos")), e.hasUUID("Witch") ? e.getUUID("Witch") : null));
            }
            return out;
        }

        boolean near(BlockPos pos, int range) {
            for (Hut h : all()) {
                if (h.pos().distSqr(pos) <= (long) range * range) {
                    return true;
                }
            }
            return false;
        }
    }

    static boolean hutNear(ServerLevel level, BlockPos pos, int range) {
        return Huts.get(level).near(pos, range);
    }

    /** The witch whose house this is. */
    static void bind(ServerLevel level, BlockPos floor, GrandWitch witch) {
        Huts.get(level).bind(floor, witch.getUUID());
        witch.setHome(floor);
    }

    /** Test hook: people who are not in the world's list of players, to be looked for in the houses. */
    static final List<Player> extraPeople = new ArrayList<>();

    /** The room: the 5 by 5 inside, from the floor up to the roof. */
    static AABB interior(BlockPos floor) {
        return new AABB(floor.getX() - 2.0D, floor.getY() + 1.0D, floor.getZ() - 2.0D, floor.getX() + 3.0D, floor.getY() + 4.0D, floor.getZ() + 3.0D);
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) {
            if (level.getGameTime() % 10L == 0L && WitchConfig.HUT_ALARM.get()) {
                checkIntruders(level);
            }
            if (level.getGameTime() % 1200L == 0L) {
                respawn(level);
            }
        }
    }

    /**
     * Each minute, a house that has no witch (hers is dead or gone) has a small chance of one coming to it, if a player is within 96 blocks of it (there is no one to
     * see her, else) and no other witch is within 160 blocks of it. She comes to the front of the door, dressed, and the house is hers.
     * @return the witch, or null
     */
    static GrandWitch respawn(ServerLevel level) {
        if (!WitchConfig.HUT_RESPAWN.get()) {
            return null;
        }
        for (Hut hut : Huts.get(level).all()) {
            if (!level.hasChunkAt(hut.pos()) || (hut.witch() != null && level.getEntity(hut.witch()) instanceof GrandWitch w && w.isAlive())) {
                continue;
            }
            boolean someone = false;
            List<Player> people = new ArrayList<>(level.players());
            people.addAll(extraPeople);
            for (Player p : people) {
                if (!p.isSpectator() && p.isAlive() && p.distanceToSqr(hut.pos().getX() + 0.5D, hut.pos().getY(), hut.pos().getZ() + 0.5D) <= 96.0D * 96.0D) {
                    someone = true;
                }
            }
            if (!someone || level.random.nextDouble() >= WitchConfig.HUT_RESPAWN_CHANCE.get()
                    || !level.getEntitiesOfClass(GrandWitch.class, new AABB(hut.pos()).inflate(160.0D)).isEmpty()) {
                continue;
            }
            GrandWitch witch = ModEntities.GRAND_WITCH.get().create(level);
            if (witch == null) {
                continue;
            }
            BlockPos at = hut.pos().offset(0, 1, 5);
            witch.moveTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            witch.dressUp();
            level.addFreshEntity(witch);
            bind(level, hut.pos(), witch);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, at.getX() + 0.5D, at.getY() + 0.5D, at.getZ() + 0.5D, 20, 0.3D, 0.5D, 0.3D, 0.03D);
            return witch;
        }
        return null;
    }

    /** Whoever is in a house whose witch is about sets her on them. */
    static void checkIntruders(ServerLevel level) {
        for (Hut hut : Huts.get(level).all()) {
            if (hut.witch() == null || !level.hasChunkAt(hut.pos())) {
                continue;
            }
            AABB room = interior(hut.pos());
            List<Player> people = new ArrayList<>(level.getEntitiesOfClass(Player.class, room, p -> !p.isSpectator() && p.isAlive()));
            for (Player p : extraPeople) {
                if (!p.isSpectator() && p.isAlive() && room.contains(p.position())) {
                    people.add(p);
                }
            }
            if (!people.isEmpty() && level.getEntity(hut.witch()) instanceof GrandWitch witch && witch.isAlive()) {
                witch.alert(hut.pos(), people.get(0));
            }
        }
    }

    private static boolean clear(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty());
    }

    /** What can be cleared away for a house: air and the small things that grow, leaves, soft ground and stone, sand, snow: what no one built. */
    static boolean natural(BlockState s) {
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND)
                || s.is(BlockTags.FLOWERS) || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY) || s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.SANDSTONE);
    }

    /**
     * Whether a hut can stand on the ground with {@code floor} for the middle of its floor. Within the house and its roof (9 across, 9 deep, 10 high) there may
     * be nothing but what no one built (see {@link #natural}), and no water or lava: a hillock is cut down, and a hole of up to three blocks under the floor is
     * filled. A block that someone put there (planks, a door, a chest, a trunk of a tree) stops it.
     */
    static boolean fits(ServerLevel level, BlockPos floor) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                BlockPos column = floor.offset(dx, 0, dz);
                for (int y = 1; y <= 10; y++) {
                    BlockState s = level.getBlockState(column.above(y));
                    if (!natural(s) || !s.getFluidState().isEmpty()) {
                        return false;
                    }
                }
                if (Math.abs(dx) > 3 || Math.abs(dz) > 3) {
                    continue;                                      // the eaves, with nothing under them to hold
                }
                BlockState at = level.getBlockState(column);
                if (!natural(at) || !at.getFluidState().isEmpty()) {
                    return false;
                }
                boolean held = false;
                for (int d = 1; d <= 3 && !held; d++) {
                    BlockState below = level.getBlockState(column.below(d));
                    held = below.blocksMotion() && below.getFluidState().isEmpty();
                    if (!held && !below.isAir() && !below.canBeReplaced()) {
                        return false;                         // water, say, under it
                    }
                }
                if (!held && !level.getBlockState(column.below()).blocksMotion()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** A house with no witch: hers is dead, or gone, and the place round it is loaded; the nearest within the range. */
    static Hut ownerless(ServerLevel level, BlockPos near, int range) {
        Hut best = null;
        for (Hut h : Huts.get(level).all()) {
            if (h.pos().distSqr(near) > (long) range * range || !level.hasChunkAt(h.pos())) {
                continue;
            }
            boolean owned = h.witch() != null && level.getEntity(h.witch()) instanceof GrandWitch w && w.isAlive();
            if (!owned && (best == null || h.pos().distSqr(near) < best.pos().distSqr(near))) {
                best = h;
            }
        }
        return best;
    }

    private static void put(ServerLevel level, BlockPos floor, int dx, int dy, int dz, BlockState state) {
        level.setBlock(floor.offset(dx, dy, dz), state, 3);
    }

    private static BlockState stair(Direction facing) {
        return Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing);
    }

    /** Whether there is room, and ground, for a small thing of the garden or the porch where it is meant to go: it is skipped where there is not. */
    private static boolean freeSpot(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return clear(level, pos) && below.blocksMotion() && below.getFluidState().isEmpty();
    }

    private static void decor(ServerLevel level, BlockPos floor, int dx, int dy, int dz, BlockState state) {
        BlockPos pos = floor.offset(dx, dy, dz);
        if (freeSpot(level, pos)) {
            level.setBlock(pos, state, 3);
        }
    }

    /**
     * Builds the hut with {@code floor} the middle of its floor (a block of the ground), if it fits: a cottage of dark oak and spruce on a cobblestone
     * footing, with a roof as steep as it is wide, a chimney that smokes at the top, a porch at the front with a lantern on each post, a garden at the side with
     * mushrooms and pumpkins in a fence, and inside it a cauldron over a fire, a brewing stand, shelves, a lectern with the diary on it, a chest with the antidote.
     * The inside, and the places in it, are as they were; so is the door, on the south side.
     * @return the place in front of the door, outside, or null if it did not fit
     */
    public static BlockPos build(ServerLevel level, BlockPos floor) {
        if (!fits(level, floor)) {
            return null;
        }
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState beam = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState band = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState bandZ = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        java.util.Random random = new java.util.Random(floor.asLong());
        // the place is cleared of what grew or lay there: the ground is cut down to the floor, and the trees' leaves and the small plants go
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int y = 1; y <= 10; y++) {
                    BlockPos cell = floor.offset(dx, y, dz);
                    if (!level.getBlockState(cell).isAir()) {
                        level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        // the footing, the floor and the walls
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos column = floor.offset(dx, 0, dz);
                for (int d = 1; d <= 3; d++) {              // under the floor, whatever is missing
                    if (!level.getBlockState(column.below(d)).blocksMotion()) {
                        level.setBlock(column.below(d), cobble, 3);
                    }
                }
                boolean edge = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                boolean corner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
                level.setBlock(column, edge ? (random.nextInt(3) == 0 ? mossy : cobble) : planks, 3);
                for (int y = 1; y <= 3; y++) {
                    BlockState wall = Blocks.AIR.defaultBlockState();
                    if (edge) {
                        wall = corner ? beam : (y == 3 ? (Math.abs(dx) == 3 ? bandZ : band) : planks);
                    }
                    level.setBlock(column.above(y), wall, 3);
                }
                level.setBlock(column.above(4), planks, 3);       // the ceiling
            }
        }
        // the roof: stairs up either side, as steep as it is wide, and over the ends of the house a lip (the eaves) at the front and the back
        for (int dz = -4; dz <= 4; dz++) {
            for (int step = 0; step < 4; step++) {
                put(level, floor, -4 + step, 4 + step, dz, stair(Direction.EAST));          // a stair faces the way it rises: up toward the ridge
                put(level, floor, 4 - step, 4 + step, dz, stair(Direction.WEST));
            }
            put(level, floor, 0, 8, dz, Blocks.SPRUCE_SLAB.defaultBlockState());
        }
        // the ends of the roof, shut with planks, in the shape of a gable
        for (int end : new int[]{-3, 3}) {
            for (int step = 1; step < 4; step++) {
                for (int dx = -(3 - step); dx <= 3 - step; dx++) {
                    put(level, floor, dx, 4 + step, end, planks);
                }
            }
        }
        // the door, on the south side, with a post to each side of it, and the windows
        BlockPos door = floor.offset(0, 1, 3);
        BlockState lower = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        level.setBlock(door, lower, 3);
        level.setBlock(door.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
        for (int side : new int[]{-1, 1}) {
            put(level, floor, side, 1, 3, beam);
            put(level, floor, side, 2, 3, beam);
        }
        for (BlockPos w : new BlockPos[]{floor.offset(3, 2, 0), floor.offset(-3, 2, 0), floor.offset(0, 2, -3), floor.offset(3, 2, 2), floor.offset(-3, 2, -2)}) {
            level.setBlock(w, Blocks.GLASS_PANE.defaultBlockState(), 3);
        }
        // the chimney, up through the roof, and a fire on the top of it that smokes high enough to be seen from a long way
        for (int y = 5; y <= 9; y++) {
            put(level, floor, -2, y, -2, y % 2 == 0 ? mossy : cobble);
        }
        put(level, floor, -2, 10, -2, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true).setValue(CampfireBlock.SIGNAL_FIRE, true));
        // what is in it
        level.setBlock(floor.offset(-2, 0, -2), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 3);        // under the cauldron, a fire
        level.setBlock(floor.offset(-2, 1, -2), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), 3);
        level.setBlock(floor.offset(-2, 1, 2), Blocks.BREWING_STAND.defaultBlockState(), 3);
        for (int dx = 0; dx <= 1; dx++) {
            level.setBlock(floor.offset(dx, 1, -2), Blocks.BOOKSHELF.defaultBlockState(), 3);
        }
        level.setBlock(floor.offset(0, 2, -2), Blocks.BOOKSHELF.defaultBlockState(), 3);
        level.setBlock(floor.offset(1, 2, -2), Blocks.PURPLE_CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 3), 3);
        level.setBlock(floor.offset(-2, 3, 2), Blocks.COBWEB.defaultBlockState(), 3);
        level.setBlock(floor.offset(2, 3, -2), Blocks.COBWEB.defaultBlockState(), 3);
        level.setBlock(floor.offset(0, 3, 0), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 3);
        for (int dx = -1; dx <= 1; dx++) {                                                                                       // a rug
            for (int dz = -1; dz <= 1; dz++) {
                put(level, floor, dx, 1, dz, (dx + dz) % 2 == 0 ? Blocks.PURPLE_CARPET.defaultBlockState() : Blocks.BLACK_CARPET.defaultBlockState());
            }
        }
        // the lectern, with the diary open on it
        BlockPos lectern = floor.offset(2, 1, 2);
        level.setBlock(lectern, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.WEST).setValue(LecternBlock.HAS_BOOK, true), 3);
        if (level.getBlockEntity(lectern) instanceof LecternBlockEntity le) {
            le.setBook(WitchDiary.create());
        }
        BlockPos chest = floor.offset(2, 1, -2);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), 3);
        if (level.getBlockEntity(chest) instanceof Container c) {
            c.setItem(3, new ItemStack(ModItems.ANTIDOTE.get(), 2));
            c.setItem(4, WitchDiary.create());
            c.setItem(5, ModEffects.drinkable());
            c.setItem(6, ModEffects.splash());
            c.setItem(10, new ItemStack(Items.GOLDEN_CARROT, 3));
            c.setItem(11, new ItemStack(Items.SPIDER_EYE, 4));
            c.setItem(12, new ItemStack(Items.GLASS_BOTTLE, 3));
            c.setItem(19, new ItemStack(Items.GLOWSTONE_DUST, 3));
            c.setChanged();
        }
        // the cage, in the west of the room between the cauldron (at its north end) and the brewing stand: a cell a block wide and two long (z -1 and 0), two high, the wall
        // to its west. Iron bars are posts and no wall unless they have something at each side to join to, so each line of them runs from solid to solid: the east side is a
        // log at each end and the gate between (the bars over the gate join the logs), the south end is bars from the wall to the log, the north end the cauldron. Two wild mice in it.
        BlockState bars = Blocks.IRON_BARS.defaultBlockState();
        BlockState post = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
        for (int y = 1; y <= 2; y++) {
            put(level, floor, -1, y, -1, post);
            put(level, floor, -1, y, 1, post);
            put(level, floor, -2, y, 1, bars);
        }
        put(level, floor, -1, 1, 0, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(net.minecraft.world.level.block.FenceGateBlock.FACING, Direction.EAST));
        put(level, floor, -1, 2, 0, bars);
        put(level, floor, -2, 2, -2, bars);
        put(level, floor, -2, 3, -1, bars);
        put(level, floor, -2, 3, 0, bars);
        for (int i = 0; i < 2; i++) {
            VillageMouse caught = ModEntities.VILLAGE_MOUSE.get().create(level);
            if (caught != null) {
                caught.moveTo(floor.getX() - 2 + 0.5D, floor.getY() + 1.0D, floor.getZ() + i - 1 + 0.5D, random.nextFloat() * 360.0F, 0.0F);
                caught.setCaged(true);
                level.addFreshEntity(caught);
            }
        }
        // outside, where there is room for each thing: a porch under the eaves with a post and a lantern at each side, jack-o'-lanterns at the door, the garden
        for (int side : new int[]{-2, 2}) {
            decor(level, floor, side, 1, 4, Blocks.SPRUCE_FENCE.defaultBlockState());
            decor(level, floor, side, 2, 4, Blocks.SPRUCE_FENCE.defaultBlockState());
            decor(level, floor, side, 3, 4, Blocks.LANTERN.defaultBlockState());
        }
        decor(level, floor, -1, 1, 4, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, Direction.SOUTH));
        decor(level, floor, 1, 1, 4, Blocks.JACK_O_LANTERN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, Direction.SOUTH));
        // the garden at the west side: a fence round some bare earth with what a witch grows in it
        for (int gx = -8; gx <= -5; gx++) {
            for (int gz = -1; gz <= 2; gz++) {
                BlockPos ground = floor.offset(gx, -1, gz);
                BlockPos above = ground.above();
                boolean rim = gx == -8 || gx == -5 || gz == -1 || gz == 2;
                if (!(level.getBlockState(ground).is(Blocks.GRASS_BLOCK) || level.getBlockState(ground).is(Blocks.DIRT)) || !clear(level, above) || !clear(level, above.above())) {
                    continue;
                }
                if (rim) {
                    level.setBlock(above, Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
                } else {
                    level.setBlock(ground, Blocks.PODZOL.defaultBlockState(), 3);
                    BlockState plant = switch (random.nextInt(5)) {
                        case 0 -> Blocks.RED_MUSHROOM.defaultBlockState();
                        case 1 -> Blocks.BROWN_MUSHROOM.defaultBlockState();
                        case 2 -> Blocks.FERN.defaultBlockState();
                        case 3 -> Blocks.PUMPKIN.defaultBlockState();
                        default -> Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE, 3);
                    };
                    level.setBlock(above, plant, 3);
                }
            }
        }
        for (BlockPos hole : new BlockPos[]{floor.offset(-4, 0, 1)}) {       // by the wall, a hole, the tunnel running out from the wall
            if (level.getBlockState(hole).blocksMotion() && level.getBlockState(hole.above()).isAir()) {
                level.setBlock(hole, ModBlocks.MOUSE_HOLE.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST), 3);
            }
        }
        Huts.get(level).add(floor);
        return floor.offset(0, 1, 5);
    }
}

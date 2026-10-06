package com.xiaofeiwu.grandwitch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;

/**
 * What a mouse leaves where it has dug: soft ground with a room in the middle of it, 12 sixteenths wide and 8 high. Each of its four sides is open or shut: it is open
 * where the mouse dug through (to where it came from, and on to what it dug next), and where it gives onto open air, like the mouth of a cave; and shut, with a wall of earth,
 * where there is ground beyond, so that a way is opened by digging and not by being dug into. Two tunnels side by side are not joined of themselves: the wall between them is dug
 * through by hand (it is pointed at, being a wall of the block, where what is beyond it is a tunnel). (A tunnel made before sides were kept has all of them open, as it was.)
 * <p>
 * It can also be open above (the roof is gone) and below: a mouse digs straight up and straight down, and the blocks it makes so are a shaft, which is climbed as a ladder is,
 * by what is as small as a mouse. A mouse goes through (0.4 across and high), and nothing bigger does.
 * <p>
 * The eye of a mouse (what it points at) goes through a shut wall or roof to the ground beyond it, which is what it digs; anyone else's is stopped by the roof and the posts, as by
 * any block. It looks like ground from outside, and it is ground: dug out of grass it has grass on it, and bare, it takes grass from what is by it as dirt does; plants can be set
 * on it. Anyone can dig it out with a shovel, and it gives dirt.
 */
public class MouseTunnelBlock extends Block {

    /** Grass on the top of it. */
    public static final BooleanProperty GRASSY = BooleanProperty.create("grassy");

    /** An open (true) or shut (false) side, as in a fence; open on all four is how it was and how it comes if nothing says otherwise. */
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    /** Open to the block above (the roof is gone: a way up) and to the block below (a way down); a block open either way can be climbed, as a ladder is. */
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final VoxelShape ROOF = Block.box(0, 8, 0, 16, 16, 16);
    private static final VoxelShape POSTS = Shapes.or(Block.box(0, 0, 0, 2, 8, 2), Block.box(14, 0, 0, 16, 8, 2), Block.box(0, 0, 14, 2, 8, 16), Block.box(14, 0, 14, 16, 8, 16));
    /** The roof, and a post at each corner: what is between the posts, below the roof, is open. */
    private static final VoxelShape SHAPE = Shapes.or(ROOF, POSTS);

    /** What stands in the way on a side that is shut: the wall, the lower half of that side of the block. */
    private static final VoxelShape WALL_NORTH = Block.box(0, 0, 0, 16, 8, 2);
    private static final VoxelShape WALL_SOUTH = Block.box(0, 0, 14, 16, 8, 16);
    private static final VoxelShape WALL_WEST = Block.box(0, 0, 0, 2, 8, 16);
    private static final VoxelShape WALL_EAST = Block.box(14, 0, 0, 16, 8, 16);

    public MouseTunnelBlock() {
        super(Properties.of().mapColor(MapColor.DIRT).strength(0.5F).sound(SoundType.GRAVEL).noOcclusion().randomTicks());
        registerDefaultState(stateDefinition.any().setValue(GRASSY, false).setValue(NORTH, true).setValue(EAST, true).setValue(SOUTH, true).setValue(WEST, true)
                .setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GRASSY, NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    /** The side property for a direction. */
    public static BooleanProperty side(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case UP -> UP;
            case DOWN -> DOWN;
            default -> WEST;
        };
    }

    /** A tunnel block with every side shut, and a roof. */
    public static BlockState shut(boolean grassy) {
        return ModBlocks.MOUSE_TUNNEL.get().defaultBlockState().setValue(GRASSY, grassy).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false);
    }

    /** Whether this block, which is a tunnel, is open on that side. */
    public static boolean isOpen(BlockState state, Direction direction) {
        return state.getValue(side(direction));
    }

    /**
     * Whether the block beside this one, the way this direction points, is a tunnel, or (to the side) a hole whose ends are that way: what a wall between could be dug through to.
     */
    public static boolean joinsToward(BlockGetter level, BlockPos neighbour, Direction fromTunnel) {
        BlockState there = level.getBlockState(neighbour);
        if (fromTunnel == Direction.UP && !there.is(ModBlocks.MOUSE_TUNNEL.get()) && !there.blocksMotion() && there.getFluidState().isEmpty()) {
            return true;                                   // open air above the roof: there is no ground to dig through it, so the roof itself is dug, and the way is open to the sky
        }
        if (there.is(ModBlocks.MOUSE_HOLE.get())) {
            return fromTunnel.getAxis().isHorizontal() && there.getValue(HorizontalDirectionalBlock.FACING).getAxis() == fromTunnel.getAxis();
        }
        return there.is(ModBlocks.MOUSE_TUNNEL.get());
    }

    /** The wall (or roof) on this side of this tunnel block is dug through: it is open, and so is the side of the tunnel beyond it that faces it, if that is a tunnel. */
    public static void openSide(Level level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.MOUSE_TUNNEL.get())) {
            return;
        }
        level.setBlock(pos, state.setValue(side(side), true), 3);
        BlockPos beyond = pos.relative(side);
        BlockState there = level.getBlockState(beyond);
        if (there.is(ModBlocks.MOUSE_TUNNEL.get())) {
            level.setBlock(beyond, there.setValue(side(side.getOpposite()), true), 3);
        }
    }

    /** A shaft: open above or below. */
    private static boolean isShaft(BlockState state) {
        return state.getValue(UP) || state.getValue(DOWN);
    }

    /** The walls of a shaft: 3.25 sixteenths thick, the whole height of the block, so that what is inside is 9.5 across: a mouse (0.4, 6.4 sixteenths) goes in easily, and a person (0.6, 9.6) does not. */
    private static final VoxelShape SHAFT_NORTH = Block.box(0, 0, 0, 16, 16, 3.25);
    private static final VoxelShape SHAFT_SOUTH = Block.box(0, 0, 12.75, 16, 16, 16);
    private static final VoxelShape SHAFT_WEST = Block.box(0, 0, 0, 3.25, 16, 16);
    private static final VoxelShape SHAFT_EAST = Block.box(12.75, 0, 0, 16, 16, 16);
    /** Where a side of a shaft is open: the lintel over the opening only, 8 high, as in a tunnel, so that the opening is a mouse's and no one else's. */
    private static final VoxelShape LINTEL_NORTH = Block.box(0, 8, 0, 16, 16, 3.25);
    private static final VoxelShape LINTEL_SOUTH = Block.box(0, 8, 12.75, 16, 16, 16);
    private static final VoxelShape LINTEL_WEST = Block.box(0, 8, 0, 3.25, 16, 16);
    private static final VoxelShape LINTEL_EAST = Block.box(12.75, 8, 0, 16, 16, 16);

    /** What a shaft is made of, for what walks and for what is pointed at by anyone but a mouse: the walls all round, a lintel where a side is open, and a roof if it has one. */
    private static VoxelShape shaftShape(BlockState state) {
        VoxelShape shape = state.getValue(UP) ? Shapes.empty() : ROOF;
        shape = Shapes.or(shape, state.getValue(NORTH) ? LINTEL_NORTH : SHAFT_NORTH);
        shape = Shapes.or(shape, state.getValue(SOUTH) ? LINTEL_SOUTH : SHAFT_SOUTH);
        shape = Shapes.or(shape, state.getValue(WEST) ? LINTEL_WEST : SHAFT_WEST);
        shape = Shapes.or(shape, state.getValue(EAST) ? LINTEL_EAST : SHAFT_EAST);
        return shape;
    }

    /**
     * What the eye is stopped by (and what is drawn round). For anyone but a mouse: the roof (if it is not gone) and the posts, or, for a shaft, its walls. For a mouse: the posts, and
     * the roof and the walls only where what is beyond is another tunnel (or a hole), so that they can be pointed at and dug through; where there is ground beyond them, the eye of a
     * mouse goes through to the ground, which is what it digs: sideways, up, or (there being no floor) down.
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean mouse = context instanceof EntityCollisionContext e && e.getEntity() instanceof Player p && Mice.isMouse(p);
        boolean shaft = isShaft(state);
        if (!mouse) {
            return shaft ? shaftShape(state) : SHAPE;
        }
        VoxelShape shape = shaft ? Shapes.empty() : POSTS;
        if (!state.getValue(UP) && joinsToward(level, pos.above(), Direction.UP)) {
            shape = Shapes.or(shape, ROOF);
        }
        if (!state.getValue(NORTH) && joinsToward(level, pos.north(), Direction.NORTH)) {
            shape = Shapes.or(shape, shaft ? SHAFT_NORTH : WALL_NORTH);
        }
        if (!state.getValue(SOUTH) && joinsToward(level, pos.south(), Direction.SOUTH)) {
            shape = Shapes.or(shape, shaft ? SHAFT_SOUTH : WALL_SOUTH);
        }
        if (!state.getValue(WEST) && joinsToward(level, pos.west(), Direction.WEST)) {
            shape = Shapes.or(shape, shaft ? SHAFT_WEST : WALL_WEST);
        }
        if (!state.getValue(EAST) && joinsToward(level, pos.east(), Direction.EAST)) {
            shape = Shapes.or(shape, shaft ? SHAFT_EAST : WALL_EAST);
        }
        return shape;
    }

    /** What stops what walks: the roof (if it is not gone) and the posts, and the wall on every side that is shut; in a shaft, the walls all round, the whole height of it. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (isShaft(state)) {
            return shaftShape(state);
        }
        VoxelShape shape = SHAPE;
        if (!state.getValue(NORTH)) {
            shape = Shapes.or(shape, WALL_NORTH);
        }
        if (!state.getValue(SOUTH)) {
            shape = Shapes.or(shape, WALL_SOUTH);
        }
        if (!state.getValue(WEST)) {
            shape = Shapes.or(shape, WALL_WEST);
        }
        if (!state.getValue(EAST)) {
            shape = Shapes.or(shape, WALL_EAST);
        }
        return shape;
    }

    /** A block that is open above or below is climbed as a ladder is, by what is as small as a mouse (a person or a cat is too wide to be in one). */
    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return (state.getValue(UP) || state.getValue(DOWN)) && entity.getBbWidth() <= 0.45F;
    }

    /** Test hook: whether there is light over it, in place of looking (the rooms of the tests have no sky light). */
    static Boolean lightOverride;

    /** Grass can stay on it, and grow on it, where light can come down to it. */
    private static boolean lightOnTop(LevelReader level, BlockPos pos) {
        if (lightOverride != null) {
            return lightOverride;
        }
        BlockPos above = pos.above();
        BlockState on = level.getBlockState(above);
        return on.getLightBlock(level, above) < level.getMaxLightLevel() && level.getMaxLocalRawBrightness(above) >= 4;
    }

    /** Grass dies under a block and where it is dark, and comes from grass blocks near, as it does on dirt. */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(GRASSY)) {
            if (!lightOnTop(level, pos)) {
                level.setBlockAndUpdate(pos, state.setValue(GRASSY, false));
            }
            return;
        }
        if (!lightOnTop(level, pos) || (lightOverride == null && level.getMaxLocalRawBrightness(pos.above()) < 9)) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            BlockPos from = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            BlockState near = level.getBlockState(from);
            if (near.is(Blocks.GRASS_BLOCK) || (near.is(ModBlocks.MOUSE_TUNNEL.get()) && near.getValue(GRASSY))) {
                level.setBlockAndUpdate(pos, state.setValue(GRASSY, true));
                return;
            }
        }
    }

    /** Grass, flowers and saplings can stand on it, as on any ground. */
    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos, Direction facing, IPlantable plantable) {
        PlantType type = plantable.getPlantType(level, pos.relative(facing));
        return facing == Direction.UP && type == PlantType.PLAINS;
    }
}

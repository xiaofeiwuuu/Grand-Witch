package com.xiaofeiwu.grandwitch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A stone with a tunnel through it, 12 sixteenths wide and 8 high, along the way it faces: a mouse (0.4 wide and high) goes through without having to be
 * lined up with it, and nothing bigger does (what keeps a cat or a player out is the height: a cat is 0.7, and a player 1.8, and the tunnel is 0.5),
 * and a mouse inside is out of the sight of the witch and out of the reach of cats. Now and then, with a player near, a wild mouse comes out of an end
 * of it, never more than three about.
 */
public class MouseHoleBlock extends HorizontalDirectionalBlock {

    /** Along z (facing north or south): the roof over the tunnel and a wall on each side of it. */
    private static final VoxelShape ALONG_Z = Shapes.or(Block.box(0, 8, 0, 16, 16, 16), Block.box(0, 0, 0, 2, 8, 16), Block.box(14, 0, 0, 16, 8, 16));
    private static final VoxelShape ALONG_X = Shapes.or(Block.box(0, 8, 0, 16, 16, 16), Block.box(0, 0, 0, 16, 8, 2), Block.box(0, 0, 14, 16, 8, 16));

    /** Test hook: no player has to be near. */
    static boolean requirePlayer = true;
    /** Test hook: the chance, in place of a random one. */
    static Boolean alwaysOut;

    public MouseHoleBlock() {
        super(Properties.of().mapColor(MapColor.STONE).strength(1.5F, 6.0F).requiresCorrectToolForDrops().randomTicks().noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    static VoxelShape shapeFor(BlockState state) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ALONG_Z : ALONG_X;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean out = alwaysOut != null ? alwaysOut : random.nextInt(4) == 0;
        if (!out || !WitchConfig.WILD_MICE.get()) {
            return;
        }
        if (requirePlayer) {
            Player p = level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 24.0D, false);
            if (p == null) {
                return;
            }
        }
        if (level.getEntitiesOfClass(VillageMouse.class, new AABB(pos).inflate(12.0D)).size() >= 3) {
            return;
        }
        Direction way = state.getValue(FACING);
        // out of an end of the tunnel, or else from the top
        for (Direction d : new Direction[]{way, way.getOpposite(), Direction.UP}) {
            BlockPos out1 = pos.relative(d);
            if (level.getBlockState(out1).isAir()) {
                VillageMouse mouse = ModEntities.VILLAGE_MOUSE.get().create(level);
                if (mouse != null) {
                    mouse.moveTo(out1.getX() + 0.5D, out1.getY(), out1.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
                    level.addFreshEntity(mouse);
                    level.sendParticles(ParticleTypes.POOF, mouse.getX(), mouse.getY() + 0.2D, mouse.getZ(), 6, 0.1D, 0.1D, 0.1D, 0.01D);
                }
                return;
            }
        }
    }

    /** Whether something is in a mouse hole, in the tunnel of it. */
    static boolean hidden(net.minecraft.world.entity.Entity e) {
        BlockState in = e.level().getBlockState(e.blockPosition());
        return in.is(ModBlocks.MOUSE_HOLE.get()) || in.is(ModBlocks.MOUSE_TUNNEL.get());
    }
}

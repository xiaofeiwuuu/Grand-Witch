package com.xiaofeiwu.grandwitch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;

/**
 * What a mouse leaves where it has dug: soft ground with a room in the middle of it, 12 sixteenths wide and 8 high, in the shape of a cross: a way out on every
 * side, always, whatever is on the other side of it. So a mouse inside, looking any way, sees the next block of ground in that direction, with nothing of this
 * one in the way, and can dig it, and what it digs is a cross too; a corner, a branch, a crossing are only where it chose to dig. A mouse goes through
 * (0.4 across and high), and nothing bigger does. It looks like ground from outside, and it is ground: dug out of grass it has grass on it, and bare, it takes grass
 * from what is by it as dirt does; plants can be set on it. Anyone can dig it out with a shovel, and it gives dirt.
 */
public class MouseTunnelBlock extends Block {

    /** Grass on the top of it. */
    public static final BooleanProperty GRASSY = BooleanProperty.create("grassy");

    /** The roof, and a post at each corner: what is between the posts, below the roof, is open. */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 8, 0, 16, 16, 16),
            Block.box(0, 0, 0, 2, 8, 2), Block.box(14, 0, 0, 16, 8, 2), Block.box(0, 0, 14, 2, 8, 16), Block.box(14, 0, 14, 16, 8, 16));

    public MouseTunnelBlock() {
        super(Properties.of().mapColor(MapColor.DIRT).strength(0.5F).sound(SoundType.GRAVEL).noOcclusion().randomTicks());
        registerDefaultState(stateDefinition.any().setValue(GRASSY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GRASSY);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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

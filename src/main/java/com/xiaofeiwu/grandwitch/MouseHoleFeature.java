package com.xiaofeiwu.grandwitch;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A mouse hole in the side of a bank of soft ground, where the ground steps down: the hole in the face of the bank, open to the low ground, with a tunnel of two or
 * three blocks running in from it. It is tried now and then in a chunk (the chance is in the setting), in the biomes of the plains, forests and hills.
 */
public final class MouseHoleFeature extends Feature<NoneFeatureConfiguration> {

    public MouseHoleFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!WitchConfig.WORLD_HOLES.get() || context.random().nextDouble() >= WitchConfig.WORLD_HOLE_CHANCE.get()) {
            return false;
        }
        return placeNear(context.level(), context.origin(), 5, context.random(), 24, Integer.MIN_VALUE);
    }

    private static boolean soft(WorldGenLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.is(BlockTags.DIRT) && !s.is(ModBlocks.MOUSE_TUNNEL.get()) && level.getFluidState(pos).isEmpty();
    }

    private static boolean free(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
    }

    /** The y of the top block of the ground in this column (looking down from {@code scanFrom}, or from just over the world's own height of it), or the least int if it is under water. */
    private static int surface(WorldGenLevel level, int x, int z, int scanFrom) {
        int start = scanFrom != Integer.MIN_VALUE ? scanFrom : Math.min(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + 2, level.getMaxBuildHeight() - 1);
        for (int y = start; y > level.getMinBuildHeight(); y--) {
            BlockPos p = new BlockPos(x, y, z);
            BlockState s = level.getBlockState(p);
            if (!level.getFluidState(p).isEmpty()) {
                return Integer.MIN_VALUE;
            }
            if (!s.isAir() && !s.canBeReplaced()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Looks, up to {@code attempts} times, at places within {@code radius} blocks of {@code center}, for a column of soft ground that stands at least a block over the ground
     * beside it, and makes the hole in the face of that, at the level of the low ground, with a tunnel two or three blocks long running in. {@code scanFrom} is where to
     * look down from (a test sets it), or the least int to go by the world's own heights.
     * @return whether one was made
     */
    static boolean placeNear(WorldGenLevel level, BlockPos center, int radius, RandomSource random, int attempts, int scanFrom) {
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = center.getX() + random.nextInt(radius * 2 + 1) - radius, z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;
            int top = surface(level, x, z, scanFrom);
            if (top == Integer.MIN_VALUE) {
                continue;
            }
            List<Direction> ways = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
            Collections.shuffle(ways, new java.util.Random(random.nextLong()));
            for (Direction out : ways) {
                int low = surface(level, x + out.getStepX(), z + out.getStepZ(), scanFrom);
                if (low == Integer.MIN_VALUE || low >= top) {
                    continue;
                }
                BlockPos hole = new BlockPos(x, low + 1, z);             // level with the air over the low ground
                BlockPos front = hole.relative(out);
                if (!soft(level, hole) || !free(level, front) || !free(level, front.above()) || !level.getBlockState(front.below()).isFaceSturdy(level, front.below(), Direction.UP)) {
                    continue;
                }
                int length = 2 + random.nextInt(2);
                List<BlockPos> tunnel = new ArrayList<>();
                for (int k = 1; k <= length && soft(level, hole.relative(out.getOpposite(), k)); k++) {
                    tunnel.add(hole.relative(out.getOpposite(), k));
                }
                if (tunnel.size() < 2) {
                    continue;
                }
                level.setBlock(hole, ModBlocks.MOUSE_HOLE.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, out.getOpposite()), 2);
                for (BlockPos p : tunnel) {
                    boolean grass = level.getBlockState(p).is(Blocks.GRASS_BLOCK);
                    level.setBlock(p, ModBlocks.MOUSE_TUNNEL.get().defaultBlockState().setValue(MouseTunnelBlock.GRASSY, grass), 2);
                }
                return true;
            }
        }
        return false;
    }
}

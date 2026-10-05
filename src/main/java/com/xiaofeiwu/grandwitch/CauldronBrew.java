package com.xiaofeiwu.grandwitch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Brewing the antidote: a golden carrot, a spider eye and a glass bottle, thrown into a cauldron that has water in it (and a fire under it), make two,
 * and the water goes down a level. Only the item entities that are of those three kinds are kept in mind (they are noted as they come into the world),
 * so that nothing is searched for when there are none.
 */
@Mod.EventBusSubscriber(modid = GrandWitchMod.MODID)
public final class CauldronBrew {

    private static final Set<ItemEntity> NOTED = Collections.newSetFromMap(new IdentityHashMap<>());

    private CauldronBrew() {
    }

    /** Test hook: whether a fire is needed, in place of the setting. */
    static Boolean needsFireOverride;

    static int notedCount() {
        return NOTED.size();
    }

    private static boolean ingredient(ItemStack s) {
        return s.is(Items.GOLDEN_CARROT) || s.is(Items.SPIDER_EYE) || s.is(Items.GLASS_BOTTLE);
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof ItemEntity item && ingredient(item.getItem())) {
            NOTED.add(item);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level && level.getGameTime() % 10L == 0L && !NOTED.isEmpty() && WitchConfig.BREW.get()) {
            process(level);
        }
    }

    /** Whether a fire is under the cauldron. */
    static boolean heated(ServerLevel level, BlockPos cauldron) {
        BlockState below = level.getBlockState(cauldron.below());
        return below.is(BlockTags.FIRE) || (below.is(BlockTags.CAMPFIRES) && below.getValue(CampfireBlock.LIT)) || below.is(Blocks.MAGMA_BLOCK) || below.getFluidState().is(FluidTags.LAVA);
    }

    /** Looks at the noted items in each cauldron, and brews what can be brewed. */
    static void process(ServerLevel level) {
        process(level, needsFireOverride != null ? needsFireOverride : WitchConfig.BREW_NEEDS_FIRE.get());
    }

    /** @param needsFire whether a fire has to be under the cauldron (a test says so, whatever the setting is) */
    static void process(ServerLevel level, boolean needsFire) {
        // each world is looked at in its own turn: what is noted in another world is not this one's to forget
        NOTED.removeIf(ItemEntity::isRemoved);
        List<BlockPos> done = new ArrayList<>();
        for (ItemEntity item : new ArrayList<>(NOTED)) {
            if (!item.level().dimension().equals(level.dimension())) {
                continue;
            }
            BlockPos pos = item.blockPosition();
            if (done.contains(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.WATER_CAULDRON) || (needsFire && !heated(level, pos))) {
                continue;
            }
            ItemEntity carrot = find(level, pos, Items.GOLDEN_CARROT), eye = find(level, pos, Items.SPIDER_EYE), bottle = find(level, pos, Items.GLASS_BOTTLE);
            if (carrot == null || eye == null || bottle == null) {
                continue;
            }
            done.add(pos);
            for (ItemEntity used : new ItemEntity[]{carrot, eye, bottle}) {
                used.getItem().shrink(1);
                if (used.getItem().isEmpty()) {
                    used.discard();
                }
            }
            LayeredCauldronBlock.lowerFillLevel(state, level, pos);
            ItemEntity out = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, new ItemStack(ModItems.ANTIDOTE.get(), 2));
            out.setDeltaMovement(0.0D, 0.25D, 0.0D);
            level.addFreshEntity(out);
            level.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 20, 0.25D, 0.1D, 0.25D, 0.05D);
            level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 12, 0.25D, 0.2D, 0.25D, 0.05D);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private static ItemEntity find(ServerLevel level, BlockPos cauldron, Item item) {
        for (ItemEntity i : NOTED) {
            if (!i.isRemoved() && i.level().dimension().equals(level.dimension()) && i.blockPosition().equals(cauldron) && i.getItem().is(item)) {
                return i;
            }
        }
        return null;
    }
}

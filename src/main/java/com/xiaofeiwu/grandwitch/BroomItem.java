package com.xiaofeiwu.grandwitch;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A broom: used, it is put under the player, who is on it; it is taken up again by crouching and using it, or it breaks when it is worn out. */
public class BroomItem extends Item {

    private final BroomKind kind;

    public BroomItem(BroomKind kind, Properties properties) {
        super(properties.durability(kind.durability));
        this.kind = kind;
    }

    public BroomKind kind() {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isPassenger()) {
            return InteractionResultHolder.pass(stack);
        }
        if (Mice.isMouse(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.grandwitch.broom.mouse"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            BroomEntity broom = ModEntities.BROOM.get().create(level);
            if (broom == null) {
                return InteractionResultHolder.fail(stack);
            }
            broom.setItem(stack.copy());
            broom.moveTo(player.getX(), player.getY() + 0.05D, player.getZ(), player.getYRot(), 0.0F);
            level.addFreshEntity(broom);
            player.startRiding(broom, true);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

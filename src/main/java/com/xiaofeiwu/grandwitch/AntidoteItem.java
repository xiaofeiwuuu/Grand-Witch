package com.xiaofeiwu.grandwitch;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** The antidote: drunk, it turns a mouse back into the player; given to another mouse (a player, or a child), it does the same for it. */
public class AntidoteItem extends Item {

    public AntidoteItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // it can always be begun: whether it is of use is for the server to say, when it has been drunk (what this side knows of whether the player is a mouse
        // may be a moment behind, and a drink that cannot be begun for that is no drink)
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !level.isClientSide) {
            if (!Mice.isMouse(player)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.grandwitch.antidote_unneeded"), true);
                return stack;                                     // nothing to cure: it is not used up
            }
            Mice.cure(player, true);
            return used(stack, player);
        }
        return stack;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) {
            return target instanceof VillageMouse || Mice.isMouse(target) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        boolean cured = false;
        if (target instanceof VillageMouse mouse) {
            cured = mouse.revert();
        } else if (target instanceof Player other && Mice.isMouse(other)) {
            Mice.cure(other, true);
            cured = true;
        }
        if (!cured) {
            return InteractionResult.PASS;
        }
        player.setItemInHand(hand, used(stack, player));
        return InteractionResult.SUCCESS;
    }

    /** One used: the stack is a bottle less, and an empty bottle comes back. */
    private static ItemStack used(ItemStack stack, Player player) {
        if (player.getAbilities().instabuild) {
            return stack;
        }
        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (stack.isEmpty()) {
            return bottle;
        }
        if (!player.getInventory().add(bottle)) {
            player.drop(bottle, false);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.grandwitch.antidote.tip").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}

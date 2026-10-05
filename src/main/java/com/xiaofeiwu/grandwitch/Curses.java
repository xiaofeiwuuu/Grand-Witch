package com.xiaofeiwu.grandwitch;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The trader's gift is good, and cursed: carried for long enough, in all (not all at one time: it is counted while it is on a player, and kept when it is not),
 * it makes a mouse of the one who carries it, and is a plain thing after that. It is told by a line on it, that it is warm to the touch, and by what is said at a
 * third and at two thirds of the time.
 */
@Mod.EventBusSubscriber(modid = GrandWitchMod.MODID)
public final class Curses {

    static final String TAG = "GrandWitchCurse";
    static final String COUNT = "GrandWitchCursedFor";

    /** Test hook: how many seconds, in place of the setting. */
    static int secondsOverride = -1;

    private Curses() {
    }

    /** The thing, with the curse of this witch on it, and a line that says it is warm. */
    static ItemStack curse(ItemStack stack, UUID witch) {
        stack.getOrCreateTag().putUUID(TAG, witch);
        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable("item.grandwitch.curse_lore").withStyle(net.minecraft.ChatFormatting.GRAY))));
        display.put("Lore", lore);
        return stack;
    }

    static boolean cursed(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().hasUUID(TAG);
    }

    private static List<ItemStack> carried(Player player) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : player.getInventory().items) {
            if (cursed(s)) {
                out.add(s);
            }
        }
        for (ItemStack s : player.getInventory().armor) {
            if (cursed(s)) {
                out.add(s);
            }
        }
        if (cursed(player.getOffhandItem())) {
            out.add(player.getOffhandItem());
        }
        return out;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide && event.player.tickCount % 20 == 0) {
            second(event.player);
        }
    }

    /** One second of whatever is carried. */
    static void second(Player player) {
        List<ItemStack> mine = carried(player);
        if (mine.isEmpty()) {
            return;
        }
        int n = player.getPersistentData().getInt(COUNT) + 1;
        player.getPersistentData().putInt(COUNT, n);
        int limit = secondsOverride >= 0 ? secondsOverride : WitchConfig.CURSE_SECONDS.get();
        if (n >= limit) {
            UUID witch = mine.get(0).getTag().getUUID(TAG);
            for (ItemStack s : mine) {
                s.getTag().remove(TAG);
                CompoundTag display = s.getTag().getCompound("display");
                display.remove("Lore");
                if (display.isEmpty()) {
                    s.getTag().remove("display");
                }
            }
            player.getPersistentData().putInt(COUNT, 0);
            if (!Mice.isMouse(player)) {
                Mice.become(player, witch, WitchConfig.MOUSE_SECONDS.get());
            }
        } else if (n == Math.max(1, limit / 3) || n == Math.max(2, limit * 2 / 3)) {
            player.displayClientMessage(Component.translatable(n < limit / 2 ? "message.grandwitch.curse.warm" : "message.grandwitch.curse.hot"), true);
        }
    }
}

package com.xiaofeiwu.grandwitch;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** For trying it out, operators only: {@code /grandwitch witch}, {@code /grandwitch mouse} (turn yourself into one, or back), {@code /grandwitch cure}. */
@Mod.EventBusSubscriber(modid = GrandWitchMod.MODID)
public final class WitchCommand {

    private WitchCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("grandwitch").requires(s -> s.hasPermission(2))
                .then(Commands.literal("witch").executes(c -> witch(c.getSource())))
                .then(Commands.literal("hut").executes(c -> hut(c.getSource())))
                .then(Commands.literal("mouse").executes(c -> mouse(c.getSource())))
                .then(Commands.literal("cure").executes(c -> cure(c.getSource()))));
    }

    private static int witch(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        GrandWitch witch = WitchVisits.visit(source.getLevel(), player, true);
        source.sendSuccess(() -> Component.translatable(witch == null ? "command.grandwitch.no_room" : "command.grandwitch.came", witch == null ? "" : witch.blockPosition().toShortString()), false);
        return witch == null ? 0 : 1;
    }

    private static int hut(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        net.minecraft.core.BlockPos floor = player.blockPosition().below().relative(player.getDirection(), 6);
        net.minecraft.core.BlockPos door = WitchHut.build(source.getLevel(), floor);
        source.sendSuccess(() -> Component.translatable(door == null ? "command.grandwitch.no_hut" : "command.grandwitch.hut", floor.toShortString()), false);
        return door == null ? 0 : 1;
    }

    private static int mouse(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        if (Mice.isMouse(player)) {
            Mice.cure(player, true);
        } else {
            Mice.become(player, null, WitchConfig.MOUSE_SECONDS.get());
        }
        return 1;
    }

    private static int cure(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            Mice.cure(player, true);
        }
        return 1;
    }
}

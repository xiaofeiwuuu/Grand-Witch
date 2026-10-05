package com.xiaofeiwu.grandwitch;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Tells the clients which players are mice, so that they draw them small and as mice, and size their boxes alike. */
public record MousePacket(UUID player, boolean mouse) {

    static void encode(MousePacket p, FriendlyByteBuf buf) {
        buf.writeUUID(p.player);
        buf.writeBoolean(p.mouse);
    }

    static MousePacket decode(FriendlyByteBuf buf) {
        return new MousePacket(buf.readUUID(), buf.readBoolean());
    }

    static void handle(MousePacket p, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.xiaofeiwu.grandwitch.client.MouseClient.apply(p.player, p.mouse)));
        context.get().setPacketHandled(true);
    }
}

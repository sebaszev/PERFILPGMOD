package com.liberticraft.profile.network;

import com.liberticraft.profile.server.ProfileService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Cliente -> servidor: "quiero ver el perfil de este jugador" (clic con la rueda). */
public class RequestProfilePacket {
    public final UUID target;

    public RequestProfilePacket(UUID target) {
        this.target = target;
    }

    public static void encode(RequestProfilePacket p, FriendlyByteBuf buf) {
        buf.writeUUID(p.target);
    }

    public static RequestProfilePacket decode(FriendlyByteBuf buf) {
        return new RequestProfilePacket(buf.readUUID());
    }

    public static void handle(RequestProfilePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            ServerPlayer target = sender.server.getPlayerList().getPlayer(p.target);
            if (target == null) return;
            ProfileService.send(sender, ProfileService.build(sender, target, true));
        });
        ctx.get().setPacketHandled(true);
    }
}

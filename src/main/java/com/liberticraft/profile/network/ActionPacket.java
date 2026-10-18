package com.liberticraft.profile.network;

import com.liberticraft.profile.server.ProfileService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Cliente -> servidor: acciones dentro de la interfaz (elegir fondo/estampilla, desafiar, corazon). */
public class ActionPacket {
    public static final byte SET_BACKGROUND = 0;
    public static final byte SET_STAMP = 1;
    public static final byte CHALLENGE = 2;
    public static final byte HEART = 3;

    public final byte type;
    public final String value;
    public final UUID target;

    public ActionPacket(byte type, String value, UUID target) {
        this.type = type;
        this.value = value;
        this.target = target;
    }

    public static void encode(ActionPacket p, FriendlyByteBuf buf) {
        buf.writeByte(p.type);
        buf.writeUtf(p.value, 64);
        buf.writeUUID(p.target);
    }

    public static ActionPacket decode(FriendlyByteBuf buf) {
        return new ActionPacket(buf.readByte(), buf.readUtf(64), buf.readUUID());
    }

    public static void handle(ActionPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) ProfileService.handleAction(sender, p);
        });
        ctx.get().setPacketHandled(true);
    }
}

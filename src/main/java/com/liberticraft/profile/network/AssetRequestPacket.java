package com.liberticraft.profile.network;

import com.liberticraft.profile.server.AssetService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.function.Supplier;

/** Cliente -> servidor: "enviame este PNG". */
public class AssetRequestPacket {
    private static final int CHUNK = 60000;

    public final String kind;
    public final String id;

    public AssetRequestPacket(String kind, String id) {
        this.kind = kind;
        this.id = id;
    }

    public static void encode(AssetRequestPacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.kind, 32);
        buf.writeUtf(p.id, 64);
    }

    public static AssetRequestPacket decode(FriendlyByteBuf buf) {
        return new AssetRequestPacket(buf.readUtf(32), buf.readUtf(64));
    }

    public static void handle(AssetRequestPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            byte[] data = AssetService.read(p.kind, p.id);
            if (data == null) {
                send(sender, new AssetPacket(p.kind, p.id, false, 0, 1, new byte[0]));
                return;
            }
            int count = (data.length + CHUNK - 1) / CHUNK;
            for (int i = 0; i < count; i++) {
                byte[] part = Arrays.copyOfRange(data, i * CHUNK, Math.min(data.length, (i + 1) * CHUNK));
                send(sender, new AssetPacket(p.kind, p.id, true, i, count, part));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void send(ServerPlayer to, AssetPacket packet) {
        ProfileNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> to), packet);
    }
}

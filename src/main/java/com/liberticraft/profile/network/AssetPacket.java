package com.liberticraft.profile.network;

import com.liberticraft.profile.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: un trozo de un PNG. */
public class AssetPacket {
    public final String kind;
    public final String id;
    public final boolean found;
    public final int index;
    public final int count;
    public final byte[] data;

    public AssetPacket(String kind, String id, boolean found, int index, int count, byte[] data) {
        this.kind = kind;
        this.id = id;
        this.found = found;
        this.index = index;
        this.count = count;
        this.data = data;
    }

    public static void encode(AssetPacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.kind, 32);
        buf.writeUtf(p.id, 64);
        buf.writeBoolean(p.found);
        buf.writeVarInt(p.index);
        buf.writeVarInt(p.count);
        buf.writeByteArray(p.data);
    }

    public static AssetPacket decode(FriendlyByteBuf buf) {
        return new AssetPacket(buf.readUtf(32), buf.readUtf(64), buf.readBoolean(),
                buf.readVarInt(), buf.readVarInt(), buf.readByteArray(70000));
    }

    public static void handle(AssetPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.handleAsset(p)));
        ctx.get().setPacketHandled(true);
    }
}

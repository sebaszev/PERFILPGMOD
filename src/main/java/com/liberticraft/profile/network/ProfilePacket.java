package com.liberticraft.profile.network;

import com.liberticraft.profile.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Servidor -> cliente: todos los datos que se muestran en el perfil de un jugador. */
public class ProfilePacket {
    public record RoleInfo(String label, int color) {}

    public UUID target;
    public String name = "";
    /** true si el que mira es el propio dueño del perfil. */
    public boolean self;
    /** true: abrir la pantalla si no esta abierta; false: solo refrescarla si ya esta abierta. */
    public boolean open;

    public float health;
    public float maxHealth;
    public int xpLevel;
    public long playTicks;
    public int kills;
    public int hearts;

    public int frameColor;
    public String frameDesign = "";
    public String background = "";
    public String stamp = "";

    public boolean premium;
    public List<RoleInfo> roles = new ArrayList<>();
    /** -1 = oculto. */
    public long coins = -1;
    public boolean canHeart;
    /** Huella de los PNG del servidor; si cambia, el cliente vuelve a pedir las imagenes. */
    public long assetRev;

    // Solo se rellenan para el propio jugador:
    public List<String> backgrounds = new ArrayList<>();
    public List<String> frames = new ArrayList<>();
    public List<String> stamps = new ArrayList<>();

    public static void encode(ProfilePacket p, FriendlyByteBuf buf) {
        buf.writeUUID(p.target);
        buf.writeUtf(p.name, 64);
        buf.writeBoolean(p.self);
        buf.writeBoolean(p.open);
        buf.writeFloat(p.health);
        buf.writeFloat(p.maxHealth);
        buf.writeVarInt(p.xpLevel);
        buf.writeLong(p.playTicks);
        buf.writeVarInt(p.kills);
        buf.writeVarInt(p.hearts);
        buf.writeInt(p.frameColor);
        buf.writeUtf(p.frameDesign, 64);
        buf.writeUtf(p.background, 64);
        buf.writeUtf(p.stamp, 64);
        buf.writeBoolean(p.premium);
        buf.writeVarInt(p.roles.size());
        for (RoleInfo r : p.roles) {
            buf.writeUtf(r.label(), 32);
            buf.writeInt(r.color());
        }
        buf.writeLong(p.coins);
        buf.writeBoolean(p.canHeart);
        buf.writeLong(p.assetRev);
        writeList(buf, p.backgrounds);
        writeList(buf, p.frames);
        writeList(buf, p.stamps);
    }

    public static ProfilePacket decode(FriendlyByteBuf buf) {
        ProfilePacket p = new ProfilePacket();
        p.target = buf.readUUID();
        p.name = buf.readUtf(64);
        p.self = buf.readBoolean();
        p.open = buf.readBoolean();
        p.health = buf.readFloat();
        p.maxHealth = buf.readFloat();
        p.xpLevel = buf.readVarInt();
        p.playTicks = buf.readLong();
        p.kills = buf.readVarInt();
        p.hearts = buf.readVarInt();
        p.frameColor = buf.readInt();
        p.frameDesign = buf.readUtf(64);
        p.background = buf.readUtf(64);
        p.stamp = buf.readUtf(64);
        p.premium = buf.readBoolean();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) p.roles.add(new RoleInfo(buf.readUtf(32), buf.readInt()));
        p.coins = buf.readLong();
        p.canHeart = buf.readBoolean();
        p.assetRev = buf.readLong();
        p.backgrounds = readList(buf);
        p.frames = readList(buf);
        p.stamps = readList(buf);
        return p;
    }

    private static void writeList(FriendlyByteBuf buf, List<String> list) {
        buf.writeVarInt(list.size());
        for (String s : list) buf.writeUtf(s, 64);
    }

    private static List<String> readList(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(buf.readUtf(64));
        return out;
    }

    public static void handle(ProfilePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.handleProfile(p)));
        ctx.get().setPacketHandled(true);
    }
}

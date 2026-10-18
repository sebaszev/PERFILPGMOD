package com.liberticraft.profile.client;

import com.liberticraft.profile.network.AssetPacket;
import com.liberticraft.profile.network.ProfilePacket;
import net.minecraft.client.Minecraft;

/** Puntos de entrada del cliente cuando llegan paquetes del servidor. */
public final class ClientHooks {
    private static long knownRevision = Long.MIN_VALUE;

    private ClientHooks() {}

    public static void handleProfile(ProfilePacket p) {
        // Si el servidor cambio algun PNG, descartar las imagenes guardadas para volver a pedirlas.
        if (p.assetRev != knownRevision) {
            ImageStore.clear();
            knownRevision = p.assetRev;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ProfileScreen screen && screen.targetId().equals(p.target)) {
            screen.update(p);
        } else if (p.open) {
            mc.setScreen(new ProfileScreen(p));
        }
    }

    public static void handleAsset(AssetPacket p) {
        ImageStore.onChunk(p);
    }

    public static void onDisconnect() {
        knownRevision = Long.MIN_VALUE;
        ImageStore.clear();
    }
}

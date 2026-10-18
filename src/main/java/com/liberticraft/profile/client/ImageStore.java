package com.liberticraft.profile.client;

import com.liberticraft.profile.LibertiProfile;
import com.liberticraft.profile.network.AssetPacket;
import com.liberticraft.profile.network.AssetRequestPacket;
import com.liberticraft.profile.network.ProfileNetwork;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Imagenes (fondos, marcos, estampillas, iconos) descargadas del servidor bajo demanda.
 * Los PNG estan en el servidor: config/libertiprofile/<tipo>/<id>.png
 * Mientras una imagen no ha llegado, get() devuelve null (la interfaz dibuja un hueco).
 */
public final class ImageStore {
    public record Tex(ResourceLocation loc, int w, int h) {}

    private static final Map<String, Optional<Tex>> CACHE = new HashMap<>();
    private static final Set<String> PENDING = new HashSet<>();
    private static final Map<String, ByteArrayOutputStream> ASSEMBLY = new HashMap<>();

    private ImageStore() {}

    public static Tex get(String kind, String id) {
        if (id == null || id.isEmpty()) return null;
        String key = kind + "/" + id;
        Optional<Tex> cached = CACHE.get(key);
        if (cached != null) return cached.orElse(null);
        if (PENDING.add(key) && Minecraft.getInstance().getConnection() != null) {
            ProfileNetwork.CHANNEL.sendToServer(new AssetRequestPacket(kind, id));
        }
        return null;
    }

    /** Llega un trozo de imagen desde el servidor (hilo principal). */
    public static void onChunk(AssetPacket p) {
        String key = p.kind + "/" + p.id;
        if (!p.found) {
            PENDING.remove(key);
            ASSEMBLY.remove(key);
            CACHE.put(key, Optional.empty());
            return;
        }
        ByteArrayOutputStream buf = ASSEMBLY.computeIfAbsent(key, k -> new ByteArrayOutputStream());
        buf.writeBytes(p.data);
        if (p.index < p.count - 1) return;

        ASSEMBLY.remove(key);
        PENDING.remove(key);
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(buf.toByteArray()));
            int w = img.getWidth();
            int h = img.getHeight();
            ResourceLocation loc = new ResourceLocation(LibertiProfile.MODID, "dyn/" + p.kind + "/" + p.id);
            Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(img));
            CACHE.put(key, Optional.of(new Tex(loc, w, h)));
        } catch (Exception e) {
            LibertiProfile.LOGGER.warn("[LibertiProfile] PNG no valido {}: {}", key, e.toString());
            CACHE.put(key, Optional.empty());
        }
    }

    /** Vacia todo (cambio de archivos en el servidor, recarga F3+T o desconexion). */
    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        for (Optional<Tex> t : CACHE.values()) {
            t.ifPresent(tex -> mc.getTextureManager().release(tex.loc()));
        }
        CACHE.clear();
        PENDING.clear();
        ASSEMBLY.clear();
    }
}

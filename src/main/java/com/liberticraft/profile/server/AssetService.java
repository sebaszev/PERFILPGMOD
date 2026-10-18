package com.liberticraft.profile.server;

import com.liberticraft.profile.LibertiProfile;
import com.liberticraft.profile.ProfileConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Las imagenes viven en el SERVIDOR: config/libertiprofile/<tipo>/<id>.png
 * El servidor se las envia a los clientes cuando las necesitan, asi que añadir, cambiar o quitar un PNG
 * solo requiere tocar esa carpeta (sin volver a crear el mod).
 */
public final class AssetService {
    public static final List<String> KINDS = List.of("backgrounds", "frames", "stamps", "ui");
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]{1,48}");
    private static final String[] DEFAULTS = {
            "ui/vip.png", "stamps/calabaza.png", "stamps/cono.png", "backgrounds/tormenta.png", "frames/dorado.png"};

    private static long cachedFingerprint = 0;
    private static long cachedAt = 0;

    private AssetService() {}

    public static Path root() {
        return FMLPaths.CONFIGDIR.get().resolve("libertiprofile");
    }

    public static boolean validId(String id) {
        return id != null && ID.matcher(id).matches();
    }

    public static Path file(String kind, String id) {
        if (!KINDS.contains(kind) || !validId(id)) return null;
        return root().resolve(kind).resolve(id + ".png");
    }

    /** Crea las carpetas y, la primera vez, copia los ejemplos incluidos en el mod. */
    public static void init() {
        try {
            Path root = root();
            boolean first = !Files.isDirectory(root);
            for (String kind : KINDS) Files.createDirectories(root.resolve(kind));
            if (first) {
                for (String d : DEFAULTS) {
                    try (InputStream in = AssetService.class.getResourceAsStream("/libertiprofile_defaults/" + d)) {
                        if (in != null) Files.copy(in, root.resolve(d));
                    }
                }
            }
        } catch (IOException e) {
            LibertiProfile.LOGGER.warn("[LibertiProfile] No se pudieron preparar las carpetas de imagenes: {}", e.toString());
        }
    }

    public static boolean exists(String kind, String id) {
        Path p = file(kind, id);
        return p != null && Files.isRegularFile(p);
    }

    /** Devuelve los bytes del PNG o null si no existe / es demasiado grande. */
    public static byte[] read(String kind, String id) {
        Path p = file(kind, id);
        if (p == null || !Files.isRegularFile(p)) return null;
        try {
            long max = ProfileConfig.MAX_IMAGE_KB.get() * 1024L;
            if (Files.size(p) > max) {
                LibertiProfile.LOGGER.warn("[LibertiProfile] {}/{}.png supera el limite de {} KB", kind, id, ProfileConfig.MAX_IMAGE_KB.get());
                return null;
            }
            return Files.readAllBytes(p);
        } catch (IOException e) {
            return null;
        }
    }

    /** Ids (nombres de PNG sin extension) disponibles en una carpeta. */
    public static List<String> list(String kind) {
        if (!KINDS.contains(kind)) return List.of();
        try (Stream<Path> s = Files.list(root().resolve(kind))) {
            return s.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".png"))
                    .map(n -> n.substring(0, n.length() - 4))
                    .filter(ID.asMatchPredicate())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Huella de todos los PNG; cambia cuando añades, quitas o sustituyes un archivo. */
    public static synchronized long fingerprint() {
        long now = System.currentTimeMillis();
        if (now - cachedAt < 3000) return cachedFingerprint;
        long h = 17;
        for (String kind : KINDS) {
            try (Stream<Path> s = Files.list(root().resolve(kind))) {
                for (Path p : s.filter(x -> x.getFileName().toString().endsWith(".png")).sorted().toList()) {
                    h = h * 31 + p.getFileName().toString().hashCode();
                    h = h * 31 + Files.size(p);
                    h = h * 31 + Files.getLastModifiedTime(p).toMillis();
                }
            } catch (IOException ignored) {
            }
        }
        cachedFingerprint = h;
        cachedAt = now;
        return h;
    }
}

package com.liberticraft.profile.server;

import com.liberticraft.profile.LibertiProfile;
import com.liberticraft.profile.ProfileConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

import java.lang.reflect.Method;
import java.util.UUID;

/** Lee los Liberticoins (monedas de SDMShop) segun ProfileConfig.COINS_SOURCE. */
public final class CoinProvider {
    private static String cachedSpec = null;
    private static Method cachedMethod = null;
    private static Class<?> cachedParam = null;
    private static boolean warned = false;

    private CoinProvider() {}

    public static long get(ServerPlayer player) {
        String spec = ProfileConfig.COINS_SOURCE.get().trim();
        try {
            if (spec.isEmpty() || spec.equalsIgnoreCase("none")) return 0;
            if (spec.equalsIgnoreCase("sdmshop")) return fromSdmShop(player);
            if (spec.startsWith("scoreboard:")) return fromScoreboard(player, spec.substring("scoreboard:".length()).trim());
            if (spec.startsWith("nbt:")) return fromNbt(player, spec.substring("nbt:".length()).trim());
            if (spec.startsWith("method:")) return fromMethod(player, spec);
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                LibertiProfile.LOGGER.warn("[LibertiProfile] No se pudieron leer las monedas con '{}': {}", spec, t.toString());
            }
        }
        return 0;
    }

    /** SDMShop Rework: EconomyManager.economy.get() es una Function<Player, Long> (via reflexion, sin dependencia). */
    @SuppressWarnings("unchecked")
    private static long fromSdmShop(ServerPlayer player) throws Exception {
        Class<?> mgr = Class.forName("net.sixik.sdmshoprework.economy.EconomyManager");
        Object module = mgr.getField("economy").get(null);
        if (module == null) return 0;
        Object fn = module.getClass().getMethod("get").invoke(module);
        if (fn instanceof java.util.function.Function<?, ?> f) {
            Object r = ((java.util.function.Function<Player, Object>) f).apply(player);
            if (r instanceof Number n) return n.longValue();
        }
        return 0;
    }

    private static long fromScoreboard(ServerPlayer player, String objective) {
        Scoreboard sb = player.getScoreboard();
        Objective obj = sb.getObjective(objective);
        if (obj == null) return 0;
        return sb.getOrCreatePlayerScore(player.getScoreboardName(), obj).getScore();
    }

    private static long fromNbt(ServerPlayer player, String path) {
        Tag cur = player.getPersistentData();
        for (String part : path.split("\\.")) {
            if (!(cur instanceof CompoundTag ct) || !ct.contains(part)) return 0;
            cur = ct.get(part);
        }
        if (cur instanceof NumericTag n) return n.getAsLong();
        if (cur instanceof StringTag s) {
            try {
                return (long) Double.parseDouble(s.getAsString());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private static long fromMethod(ServerPlayer player, String spec) throws Exception {
        if (!spec.equals(cachedSpec)) {
            cachedSpec = spec;
            cachedMethod = null;
            cachedParam = null;
            String body = spec.substring("method:".length());
            int hash = body.indexOf('#');
            if (hash < 0) throw new IllegalArgumentException("Formato esperado method:clase#metodo");
            Class<?> cls = Class.forName(body.substring(0, hash).trim());
            String name = body.substring(hash + 1).trim();
            for (Class<?> param : new Class<?>[]{ServerPlayer.class, Player.class, UUID.class}) {
                try {
                    cachedMethod = cls.getMethod(name, param);
                    cachedParam = param;
                    break;
                } catch (NoSuchMethodException ignored) {
                }
            }
            if (cachedMethod == null) throw new NoSuchMethodException(name);
        }
        Object arg = cachedParam == UUID.class ? player.getUUID() : player;
        Object result = cachedMethod.invoke(null, arg);
        if (result instanceof Number n) return n.longValue();
        return 0;
    }
}

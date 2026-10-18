package com.liberticraft.profile.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Almacen de perfiles de todos los jugadores, guardado en data/libertiprofile.dat del mundo. */
public class ProfileSavedData extends SavedData {
    public static final String NAME = "libertiprofile";

    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();
    /** clave "dador|receptor" (o solo "dador") -> dia (epoch day) del ultimo corazon. */
    private final Map<String, Long> heartLog = new HashMap<>();

    public static ProfileSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ProfileSavedData::load, ProfileSavedData::new, NAME);
    }

    public PlayerProfile profile(UUID id) {
        return profiles.computeIfAbsent(id, k -> new PlayerProfile());
    }

    public long lastHeartDay(String key) {
        return heartLog.getOrDefault(key, Long.MIN_VALUE);
    }

    public void setHeartDay(String key, long day) {
        heartLog.put(key, day);
        setDirty();
    }

    public static ProfileSavedData load(CompoundTag root) {
        ProfileSavedData d = new ProfileSavedData();
        CompoundTag players = root.getCompound("players");
        for (String key : players.getAllKeys()) {
            try {
                d.profiles.put(UUID.fromString(key), PlayerProfile.load(players.getCompound(key)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        CompoundTag log = root.getCompound("heartLog");
        for (String key : log.getAllKeys()) d.heartLog.put(key, log.getLong(key));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        CompoundTag players = new CompoundTag();
        for (Map.Entry<UUID, PlayerProfile> e : profiles.entrySet()) {
            players.put(e.getKey().toString(), e.getValue().save());
        }
        root.put("players", players);
        CompoundTag log = new CompoundTag();
        for (Map.Entry<String, Long> e : heartLog.entrySet()) log.putLong(e.getKey(), e.getValue());
        root.put("heartLog", log);
        return root;
    }
}

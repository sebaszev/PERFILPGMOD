package com.liberticraft.profile.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashSet;
import java.util.Set;

/** Datos de perfil de un jugador (se guardan en el mundo). */
public class PlayerProfile {
    /** -1 = usar el color por defecto de la configuracion. */
    public int frameColor = -1;
    public String frameDesign = "";
    public String background = "";
    public String stamp = "";
    public int hearts = 0;
    public int kills = 0;

    public final LinkedHashSet<String> backgrounds = new LinkedHashSet<>();
    public final LinkedHashSet<String> frames = new LinkedHashSet<>();
    public final LinkedHashSet<String> stamps = new LinkedHashSet<>();

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("frameColor", frameColor);
        t.putString("frameDesign", frameDesign);
        t.putString("background", background);
        t.putString("stamp", stamp);
        t.putInt("hearts", hearts);
        t.putInt("kills", kills);
        putList(t, "backgrounds", backgrounds);
        putList(t, "frames", frames);
        putList(t, "stamps", stamps);
        return t;
    }

    public static PlayerProfile load(CompoundTag t) {
        PlayerProfile p = new PlayerProfile();
        p.frameColor = t.contains("frameColor") ? t.getInt("frameColor") : -1;
        p.frameDesign = t.getString("frameDesign");
        p.background = t.getString("background");
        p.stamp = t.getString("stamp");
        p.hearts = t.getInt("hearts");
        p.kills = t.getInt("kills");
        readList(t, "backgrounds", p.backgrounds);
        readList(t, "frames", p.frames);
        readList(t, "stamps", p.stamps);
        return p;
    }

    private static void putList(CompoundTag t, String key, Set<String> set) {
        ListTag list = new ListTag();
        for (String s : set) list.add(StringTag.valueOf(s));
        t.put(key, list);
    }

    private static void readList(CompoundTag t, String key, Set<String> set) {
        ListTag list = t.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) set.add(list.getString(i));
    }
}

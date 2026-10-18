package com.liberticraft.profile.server;

import com.liberticraft.profile.ProfileConfig;
import com.liberticraft.profile.data.PlayerProfile;
import com.liberticraft.profile.data.ProfileSavedData;
import com.liberticraft.profile.network.ActionPacket;
import com.liberticraft.profile.network.ProfileNetwork;
import com.liberticraft.profile.network.ProfilePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.network.PacketDistributor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Logica del servidor: construir los datos del perfil y ejecutar las acciones de la interfaz. */
public final class ProfileService {
    private static final Map<UUID, Long> CHALLENGE_COOLDOWN = new HashMap<>();

    private ProfileService() {}

    public static void send(ServerPlayer to, ProfilePacket packet) {
        ProfileNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> to), packet);
    }

    public static ProfilePacket build(ServerPlayer viewer, ServerPlayer target, boolean open) {
        ProfileSavedData data = ProfileSavedData.get(target.server);
        PlayerProfile prof = data.profile(target.getUUID());
        boolean self = viewer.getUUID().equals(target.getUUID());

        ProfilePacket p = new ProfilePacket();
        p.target = target.getUUID();
        p.name = target.getGameProfile().getName();
        p.self = self;
        p.open = open;
        p.health = target.getHealth();
        p.maxHealth = target.getMaxHealth();
        p.xpLevel = target.experienceLevel;
        p.playTicks = target.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        p.kills = prof.kills;
        p.hearts = prof.hearts;
        p.frameColor = prof.frameColor >= 0 ? prof.frameColor : ProfileConfig.defaultFrameColor();
        p.frameDesign = prof.frameDesign;
        p.background = prof.background;
        p.stamp = prof.stamp;

        p.premium = isPremium(target);
        p.assetRev = AssetService.fingerprint();
        for (String entry : ProfileConfig.ROLES.get()) {
            String[] parts = entry.split("\\|");
            if (parts.length < 2) continue;
            boolean hidden = target.getTags().contains(ProfileConfig.HIDE_TAG.get().trim());
            boolean showOk = parts.length < 4 || parts[3].trim().isEmpty() || target.getTags().contains(parts[3].trim());
            if (!hidden && showOk && target.getTags().contains(parts[0].trim())) {
                int color = parts.length > 2 ? ProfileConfig.parseColor(parts[2], 0xFFFFFF) : 0xFFFFFF;
                p.roles.add(new ProfilePacket.RoleInfo(parts[1].trim(), color));
            }
        }

        if (self || ProfileConfig.SHOW_COINS_TO_OTHERS.get()) p.coins = CoinProvider.get(target);
        p.canHeart = !self && !gaveHeartToday(data, viewer.getUUID(), target.getUUID());

        if (self) {
            p.backgrounds = new ArrayList<>(prof.backgrounds);
            p.frames = new ArrayList<>(prof.frames);
            p.stamps = new ArrayList<>(prof.stamps);
        }
        return p;
    }

    public static void handleAction(ServerPlayer sender, ActionPacket a) {
        MinecraftServer server = sender.server;
        ProfileSavedData data = ProfileSavedData.get(server);
        PlayerProfile mine = data.profile(sender.getUUID());

        switch (a.type) {
            case ActionPacket.SET_BACKGROUND -> {
                if (a.value.isEmpty() || mine.backgrounds.contains(a.value)) {
                    mine.background = a.value;
                    data.setDirty();
                }
                send(sender, build(sender, sender, false));
            }
            case ActionPacket.SET_STAMP -> {
                if (a.value.isEmpty() || mine.stamps.contains(a.value)) {
                    mine.stamp = a.value;
                    data.setDirty();
                }
                send(sender, build(sender, sender, false));
            }
            case ActionPacket.CHALLENGE -> {
                ServerPlayer target = server.getPlayerList().getPlayer(a.target);
                if (target == null || target == sender) return;
                long now = System.currentTimeMillis();
                long cooldownMs = ProfileConfig.PVP_COOLDOWN_SECONDS.get() * 1000L;
                Long last = CHALLENGE_COOLDOWN.get(sender.getUUID());
                if (last != null && now - last < cooldownMs) {
                    long left = (cooldownMs - (now - last) + 999) / 1000;
                    sender.sendSystemMessage(Component.literal("Espera " + left + " s para volver a desafiar.")
                            .withStyle(ChatFormatting.RED));
                    return;
                }
                CHALLENGE_COOLDOWN.put(sender.getUUID(), now);
                Component msg = Component.literal("⚔ ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(sender.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" ha desafiado a ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(target.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" a un PvP!").withStyle(ChatFormatting.GRAY));
                server.getPlayerList().broadcastSystemMessage(msg, false);
            }
            case ActionPacket.HEART -> {
                ServerPlayer target = server.getPlayerList().getPlayer(a.target);
                if (target == null || target == sender) return;
                if (gaveHeartToday(data, sender.getUUID(), target.getUUID())) {
                    send(sender, build(sender, target, false));
                    return;
                }
                data.setHeartDay(heartKey(sender.getUUID(), target.getUUID()), today());
                PlayerProfile theirs = data.profile(target.getUUID());
                theirs.hearts++;
                data.setDirty();
                sender.sendSystemMessage(Component.literal("❤ Has dejado un corazón a ")
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal(target.getGameProfile().getName()).withStyle(ChatFormatting.WHITE)));
                target.sendSystemMessage(Component.literal("❤ ")
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal(sender.getGameProfile().getName()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" te ha dejado un corazón.").withStyle(ChatFormatting.RED)));
                send(sender, build(sender, target, false));
            }
            default -> {
            }
        }
    }

    /** Premium = tiene el tag configurado O una puntuacion >= 1 en el objetivo configurado (p. ej. bp_premium). */
    private static boolean isPremium(ServerPlayer target) {
        String tag = ProfileConfig.PREMIUM_TAG.get().trim();
        if (!tag.isEmpty() && target.getTags().contains(tag)) return true;
        String obj = ProfileConfig.PREMIUM_SCOREBOARD.get().trim();
        if (!obj.isEmpty()) {
            Scoreboard sb = target.getScoreboard();
            Objective o = sb.getObjective(obj);
            if (o != null && sb.getOrCreatePlayerScore(target.getScoreboardName(), o).getScore() >= 1) return true;
        }
        return false;
    }

    private static String heartKey(UUID giver, UUID target) {
        return ProfileConfig.HEART_ONCE_PER_TARGET.get() ? giver + "|" + target : giver.toString();
    }

    private static long today() {
        return LocalDate.now().toEpochDay();
    }

    private static boolean gaveHeartToday(ProfileSavedData data, UUID giver, UUID target) {
        return data.lastHeartDay(heartKey(giver, target)) >= today();
    }
}

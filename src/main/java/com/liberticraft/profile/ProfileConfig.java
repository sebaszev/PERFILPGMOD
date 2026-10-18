package com.liberticraft.profile;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/** Configuración del mod (archivo config/libertiprofile-common.toml, en el servidor). */
public final class ProfileConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<String> PREMIUM_TAG;
    public static final ForgeConfigSpec.ConfigValue<String> PREMIUM_SCOREBOARD;
    public static final ForgeConfigSpec.ConfigValue<String> HIDE_TAG;
    public static final ForgeConfigSpec.IntValue MAX_IMAGE_KB;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ROLES;
    public static final ForgeConfigSpec.ConfigValue<String> COINS_SOURCE;
    public static final ForgeConfigSpec.BooleanValue SHOW_COINS_TO_OTHERS;
    public static final ForgeConfigSpec.IntValue PVP_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.BooleanValue HEART_ONCE_PER_TARGET;
    public static final ForgeConfigSpec.ConfigValue<String> DEFAULT_FRAME_COLOR;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("roles");
        PREMIUM_TAG = b.comment(
                "Tag de jugador que activa el simbolo VIP del perfil.",
                "sr_vip lo da el datapack de roles (BOOSTER, PREMIUM, FORZAKEN: 'VIP habilitado').",
                "Si prefieres solo el rol premium, pon sr_role_premium.")
                .define("premiumTag", "sr_vip");
        PREMIUM_SCOREBOARD = b.comment(
                "Objetivo de scoreboard cuyo valor >= 1 tambien activa el VIP (p. ej. bp_premium del pase de batalla).",
                "Vacio = desactivado.")
                .define("premiumScoreboard", "");
        ROLES = b.comment(
                "Roles que se muestran arriba del cuadro del jugador, uno por linea con el formato:",
                "  tagDelRol|ETIQUETA|#COLOR|tagDeMostrar",
                "Se muestra la etiqueta si el jugador tiene tagDelRol Y tagDeMostrar (el rol que eligio mostrar",
                "con el datapack de roles) y no tiene el tag de ocultar. tagDeMostrar es opcional.")
                .defineListAllowEmpty(List.of("roleLabels"),
                        () -> List.of(
                                "sr_role_admin|ADMIN|#FF5555|sr_show_admin",
                                "sr_role_moder|MODER|#AA0000|sr_show_moder",
                                "sr_role_booster|BOOSTER|#55FFFF|sr_show_booster",
                                "sr_role_tester|TESTER|#FFFF55|sr_show_tester",
                                "sr_role_premium|PREMIUM|#FF55FF|sr_show_premium",
                                "sr_role_builder|BUILDER|#55FF55|sr_show_builder",
                                "sr_role_crafter|CRAFTER|#FFAA00|sr_show_crafter",
                                "sr_role_shopper|SHOPPER|#5555FF|sr_show_shopper",
                                "sr_role_forzaken|FORZAKEN|#AA00AA|sr_show_forzaken"),
                        o -> o instanceof String);
        HIDE_TAG = b.comment("Tag que oculta la etiqueta de rol (el datapack lo pone con 'ocultar').")
                .define("hideTag", "sr_oculto");
        b.pop();

        b.push("coins");
        COINS_SOURCE = b.comment(
                "De donde se leen los Liberticoins (monedas de SDMShop). Opciones:",
                "  sdmshop                       -> monedas del mod SDMShop Rework (recomendado)",
                "  none                          -> no muestra monedas",
                "  scoreboard:<objetivo>         -> lee una puntuacion del scoreboard",
                "  nbt:<ruta.en.persistentData>  -> lee un numero de los datos persistentes del jugador",
                "  method:<clase.Completa>#<metodoEstatico> -> llama a un metodo estatico (recibe ServerPlayer, Player o UUID)",
                "")
                .define("coinsSource", "sdmshop");
        SHOW_COINS_TO_OTHERS = b.comment("Si es true, los demas jugadores tambien ven tus monedas.")
                .define("showCoinsToOthers", false);
        b.pop();

        b.push("images");
        MAX_IMAGE_KB = b.comment("Tamaño maximo (KB) de cada PNG que el servidor envia a los jugadores.")
                .defineInRange("maxImageKB", 2048, 16, 8192);
        b.pop();

        b.push("social");
        PVP_COOLDOWN_SECONDS = b.comment("Segundos de espera entre desafios PvP de un mismo jugador.")
                .defineInRange("pvpCooldownSeconds", 30, 0, 3600);
        HEART_ONCE_PER_TARGET = b.comment(
                "true  = un corazon al dia a cada jugador (puedes dar uno a cada persona cada dia).",
                "false = un corazon al dia en total.")
                .define("heartOncePerTargetPerDay", true);
        b.pop();

        b.push("appearance");
        DEFAULT_FRAME_COLOR = b.comment("Color del marco por defecto (hexadecimal).")
                .define("defaultFrameColor", "#3040E8");
        b.pop();

        SPEC = b.build();
    }

    private ProfileConfig() {}

    /** Convierte "#RRGGBB" o "RRGGBB" a entero; devuelve fallback si no es valido. */
    public static int parseColor(String s, int fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        if (t.startsWith("#")) t = t.substring(1);
        if (t.length() != 6) return fallback;
        try {
            return Integer.parseInt(t, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static int defaultFrameColor() {
        return parseColor(DEFAULT_FRAME_COLOR.get(), 0x3040E8);
    }
}

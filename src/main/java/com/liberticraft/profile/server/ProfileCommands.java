package com.liberticraft.profile.server;

import com.liberticraft.profile.ProfileConfig;
import com.liberticraft.profile.data.PlayerProfile;
import com.liberticraft.profile.data.ProfileSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * /perfil                         -> abre tu perfil
 * /perfil color <color>           -> cambia el color del marco (cualquier jugador)
 * /perfil marco <id|ninguno>      -> elige un marco con diseño que el admin te haya dado
 * /perfil admin fondo|marco|estampilla dar|quitar|lista ...   (solo admin, permiso 2)
 */
public final class ProfileCommands {
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_.-]{1,48}");

    private static final Map<String, Integer> COLOR_NAMES = Map.ofEntries(
            Map.entry("azul", 0x3040E8), Map.entry("rojo", 0xD82F2F), Map.entry("verde", 0x2FAE3A),
            Map.entry("amarillo", 0xF2D12E), Map.entry("naranja", 0xF28A1E), Map.entry("rosa", 0xF05FA8),
            Map.entry("morado", 0x8A3FD6), Map.entry("cian", 0x25C2D8), Map.entry("blanco", 0xF2F2F2),
            Map.entry("negro", 0x1A1A1A), Map.entry("gris", 0x808080), Map.entry("marron", 0x7A4B28));

    private enum Kind {
        BACKGROUND("fondo", "fondo", "backgrounds"),
        FRAME("marco", "marco", "frames"),
        STAMP("estampilla", "estampilla", "stamps");

        final String command;
        final String label;
        /** Carpeta en config/libertiprofile/ donde estan los PNG de este tipo. */
        final String folder;

        Kind(String command, String label, String folder) {
            this.command = command;
            this.label = label;
            this.folder = folder;
        }

        Set<String> set(PlayerProfile p) {
            return switch (this) {
                case BACKGROUND -> p.backgrounds;
                case FRAME -> p.frames;
                case STAMP -> p.stamps;
            };
        }

        void clearIfSelected(PlayerProfile p, String id) {
            switch (this) {
                case BACKGROUND -> {
                    if (id.equals(p.background)) p.background = "";
                }
                case FRAME -> {
                    if (id.equals(p.frameDesign)) p.frameDesign = "";
                }
                case STAMP -> {
                    if (id.equals(p.stamp)) p.stamp = "";
                }
            }
        }
    }

    private ProfileCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        SuggestionProvider<CommandSourceStack> ownFrames = (ctx, builder) -> {
            ServerPlayer p = ctx.getSource().getPlayer();
            if (p != null) {
                PlayerProfile prof = ProfileSavedData.get(p.server).profile(p.getUUID());
                return SharedSuggestionProvider.suggest(prof.frames, builder);
            }
            return builder.buildFuture();
        };

        d.register(Commands.literal("perfil")
                .executes(ProfileCommands::openSelf)
                .then(Commands.literal("color")
                        .then(Commands.argument("color", StringArgumentType.greedyString())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(COLOR_NAMES.keySet(), b))
                                .executes(ProfileCommands::setColor)))
                .then(Commands.literal("marco")
                        .then(Commands.literal("ninguno").executes(c -> setFrame(c, "")))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ownFrames)
                                .executes(c -> setFrame(c, StringArgumentType.getString(c, "id")))))
                .then(Commands.literal("admin")
                        .requires(s -> s.hasPermission(2))
                        .then(grant(Kind.BACKGROUND))
                        .then(grant(Kind.FRAME))
                        .then(grant(Kind.STAMP))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> grant(Kind kind) {
        return Commands.literal(kind.command)
                .then(Commands.literal("dar")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(AssetService.list(kind.folder), b))
                                        .executes(c -> give(c, kind, true)))))
                .then(Commands.literal("quitar")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(c -> give(c, kind, false)))))
                .then(Commands.literal("archivos").executes(c -> files(c, kind)))
                .then(Commands.literal("lista")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(c -> list(c, kind))));
    }

   private static int openSelf(CommandContext<CommandSourceStack> ctx)
        throws CommandSyntaxException {
    try {
        ServerPlayer p = ctx.getSource().getPlayerOrException();

        ProfileService.send(
                p,
                ProfileService.build(p, p, true)
        );

        return 1;

    } catch (Exception e) {
        com.mojang.logging.LogUtils.getLogger().error(
                "ERROR AL EJECUTAR /perfil",
                e
        );

        ctx.getSource().sendFailure(
                Component.literal(
                        "Error al abrir el perfil. Revisa la consola."
                )
        );

        return 0;
    }
}

    private static int setColor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        String raw = StringArgumentType.getString(ctx, "color").trim().toLowerCase(Locale.ROOT);
        Integer color = COLOR_NAMES.get(raw);
        if (color == null) {
            int parsed = ProfileConfig.parseColor(raw, -1);
            if (parsed >= 0) color = parsed;
        }
        if (color == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "Color no valido. Usa un nombre (azul, rojo, verde...) o un hexadecimal como #FF8800."));
            return 0;
        }
        ProfileSavedData data = ProfileSavedData.get(p.server);
        data.profile(p.getUUID()).frameColor = color;
        data.setDirty();
        ctx.getSource().sendSuccess(() -> Component.literal("Color del marco actualizado.")
                .withStyle(ChatFormatting.GREEN), false);
        ProfileService.send(p, ProfileService.build(p, p, false));
        return 1;
    }

    private static int setFrame(CommandContext<CommandSourceStack> ctx, String id) throws CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ProfileSavedData data = ProfileSavedData.get(p.server);
        PlayerProfile prof = data.profile(p.getUUID());
        if (!id.isEmpty() && !prof.frames.contains(id)) {
            ctx.getSource().sendFailure(Component.literal("No tienes ese marco. Solo un admin puede dartelo."));
            return 0;
        }
        prof.frameDesign = id;
        data.setDirty();
        ctx.getSource().sendSuccess(() -> Component.literal(id.isEmpty()
                ? "Marco con diseno quitado; se usa el color elegido."
                : "Marco '" + id + "' equipado.").withStyle(ChatFormatting.GREEN), false);
        ProfileService.send(p, ProfileService.build(p, p, false));
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> ctx, Kind kind, boolean add) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "id").toLowerCase(Locale.ROOT);
        if (!VALID_ID.matcher(id).matches()) {
            ctx.getSource().sendFailure(Component.literal(
                    "Id no valido: usa solo minusculas, numeros, _ - . (el nombre del PNG sin .png)."));
            return 0;
        }
        if (add && !AssetService.exists(kind.folder, id)) {
            ctx.getSource().sendFailure(Component.literal("No existe " + id + ".png en config/libertiprofile/"
                    + kind.folder + "/ del servidor. Sube el PNG a esa carpeta (nombre en minusculas)."));
            return 0;
        }
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "jugadores");
        int changed = 0;
        for (ServerPlayer target : players) {
            ProfileSavedData data = ProfileSavedData.get(target.server);
            PlayerProfile prof = data.profile(target.getUUID());
            boolean did;
            if (add) {
                did = kind.set(prof).add(id);
            } else {
                did = kind.set(prof).remove(id);
                if (did) kind.clearIfSelected(prof, id);
            }
            if (did) {
                data.setDirty();
                changed++;
                target.sendSystemMessage(Component.literal(add
                        ? "Has recibido el " + kind.label + " '" + id + "'. Abre tu perfil con /perfil."
                        : "Se te ha retirado el " + kind.label + " '" + id + "'.").withStyle(ChatFormatting.AQUA));
                ProfileService.send(target, ProfileService.build(target, target, false));
            }
        }
        final int n = changed;
        ctx.getSource().sendSuccess(() -> Component.literal(
                (add ? "Dado " : "Quitado ") + kind.label + " '" + id + "' a " + n + " jugador(es)."), true);
        return n;
    }

    private static int files(CommandContext<CommandSourceStack> ctx, Kind kind) {
        java.util.List<String> ids = AssetService.list(kind.folder);
        ctx.getSource().sendSuccess(() -> Component.literal("PNG en config/libertiprofile/" + kind.folder + "/: "
                + (ids.isEmpty() ? "ninguno" : String.join(", ", ids))), false);
        return ids.size();
    }

    private static int list(CommandContext<CommandSourceStack> ctx, Kind kind) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "jugador");
        PlayerProfile prof = ProfileSavedData.get(target.server).profile(target.getUUID());
        Set<String> set = kind.set(prof);
        ctx.getSource().sendSuccess(() -> Component.literal(
                target.getGameProfile().getName() + " tiene (" + kind.label + "): "
                        + (set.isEmpty() ? "ninguno" : String.join(", ", set))), false);
        return set.size();
    }
}

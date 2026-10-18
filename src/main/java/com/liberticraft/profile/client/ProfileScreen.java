package com.liberticraft.profile.client;

import com.liberticraft.profile.network.ActionPacket;
import com.liberticraft.profile.network.ProfileNetwork;
import com.liberticraft.profile.network.ProfilePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/**
 * Pantalla de perfil. Se dibuja en una cuadricula logica de 264 x 185 unidades que se centra
 * (y se reduce si la ventana es pequeña).
 *
 * Disposicion (unidades logicas):
 *   marco            0,0   - 264,185
 *   barra de titulo  6,6   - 258,24
 *   cuadro jugador   8,28  - 100,163   (interior 10,30 - 98,161 = 88 x 131)
 *   monedas          bajo el cuadro, y = 169
 *   panel de datos   104,28 - 258,177
 *   estampilla       94,143 - 126,175  (la elegida, visible para todos)
 *   botones          PvP 132,152 - 152,172 | corazon 156,152 - 190,172
 *   flechas pagina   226,161 - 238,173 | 240,161 - 252,173   (solo el dueño)
 *   flechas fondo    74,149 - 84,159 | 86,149 - 96,159       (solo el dueño)
 */
public class ProfileScreen extends Screen {
    private static final ResourceLocation ICONS =
        new ResourceLocation("textures/gui/icons.png");
    private static final int W = 264;
    private static final int H = 185;
    private static final int STAMPS_PER_PAGE = 4;

    private static final int TEXT_DARK = 0xFF2B2B2B;

    private ProfilePacket d;
    private int stampPage = 0;
    private float scale = 1f;
    private float ox;
    private float oy;
    private Component tooltip;

    public ProfileScreen(ProfilePacket data) {
        super(Component.translatable("screen.libertiprofile.title", data.name));
        this.d = data;
    }

    public UUID targetId() {
        return d.target;
    }

    public void update(ProfilePacket p) {
        this.d = p;
        clampPage();
    }

    private void clampPage() {
        int pages = Math.max(1, (d.stamps.size() + STAMPS_PER_PAGE - 1) / STAMPS_PER_PAGE);
        stampPage = Math.max(0, Math.min(stampPage, pages - 1));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        scale = Math.min(1f, Math.min((width - 4f) / W, (height - 4f) / H));
        ox = (width - W * scale) / 2f;
        oy = (height - H * scale) / 2f;
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        tooltip = null;
        float mx = (mouseX - ox) / scale;
        float my = (mouseY - oy) / scale;

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(ox, oy, 0);
        pose.scale(scale, scale, 1f);

        int frame = 0xFF000000 | (d.frameColor & 0xFFFFFF);
        drawFrame(g, frame);
        drawTitle(g);
        drawCard(g, frame, mx, my);
        drawInfoPanel(g, mx, my);
        drawEquippedStamp(g);
        drawButtons(g, frame, mx, my);

        pose.popPose();

        if (tooltip != null) g.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    private void drawFrame(GuiGraphics g, int frame) {
        g.fill(0, 0, W, H, 0xFFFFFFFF);
        g.fill(2, 2, W - 2, H - 2, frame);
        ImageStore.Tex design = ImageStore.get("frames", d.frameDesign);
        if (design != null) {
            g.blit(design.loc(), 0, 0, W, H, 0f, 0f, design.w(), design.h(), design.w(), design.h());
        }
    }

    private void drawTitle(GuiGraphics g) {
        g.fill(6, 6, W - 6, 24, 0xFF3A3A3A);
        g.fill(7, 7, W - 7, 23, 0xFFA0A0A0);
        Component title = Component.translatable("screen.libertiprofile.title", d.name);
        int tw = font.width(title);
        int total = tw + 14;
        int x = (W - total) / 2;
        PlayerInfo info = minecraft != null && minecraft.getConnection() != null
                ? minecraft.getConnection().getPlayerInfo(d.target) : null;
        ResourceLocation skin = info != null ? info.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(d.target);
        g.blit(skin, x, 10, 8, 8, 8, 8, 64, 64);
        g.blit(skin, x, 10, 40, 8, 8, 8, 64, 64); 
        g.drawString(font, title, x + 14, 11, 0x55FFFF, true);
    }

    private void drawCard(GuiGraphics g, int frame, float mx, float my) {
        // borde + fondo (gris por defecto)
        g.fill(8, 28, 100, 163, darken(frame, 0.55f));
        g.fill(10, 30, 98, 161, 0xFF8A8A8A);
        ImageStore.Tex bg = ImageStore.get("backgrounds", d.background);
        if (bg != null) {
            g.blit(bg.loc(), 10, 30, 88, 131, 0f, 0f, bg.w(), bg.h(), bg.w(), bg.h());
        }

        // jugador 3D que sigue al raton, como en el inventario
        LivingEntity ent = findEntity();
        if (ent != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, 54, 152, 46, 54f - mx, (152f - 50f) - my, ent);
        } else {
            Component missing = Component.translatable("screen.libertiprofile.missing");
            g.drawString(font, missing, 54 - font.width(missing) / 2, 90, 0xFFFFFF, true);
        }

        // roles arriba del cuadro
        if (!d.roles.isEmpty()) {
            g.fill(10, 30, 98, 42, 0x88000000);
            int total = 0;
            for (ProfilePacket.RoleInfo r : d.roles) total += font.width("[" + r.label() + "]") + 3;
            total -= 3;
            int x = 54 - total / 2;
            for (ProfilePacket.RoleInfo r : d.roles) {
                String s = "[" + r.label() + "]";
                g.drawString(font, s, x, 32, 0xFF000000 | r.color(), false);
                x += font.width(s) + 3;
            }
        }

        // simbolo VIP (solo con el rol premium)
        if (d.premium) {
            ImageStore.Tex vip = ImageStore.get("ui", "vip");
            if (vip != null) {
                g.blit(vip.loc(), 11, 45, 24, 14, 0f, 0f, vip.w(), vip.h(), vip.w(), vip.h());
            } else {
                g.fill(11, 45, 35, 59, 0xFF202020);
                g.drawString(font, Component.translatable("screen.libertiprofile.vip"), 15, 48, 0xFFAA00, false);
            }
        }

        // flechas de fondo (solo el dueño y solo si tiene fondos concedidos)
        if (d.self && !d.backgrounds.isEmpty()) {
            arrow(g, 74, 149, 84, 159, "<", true, mx, my, "screen.libertiprofile.bg.prev");
            arrow(g, 86, 149, 96, 159, ">", true, mx, my, "screen.libertiprofile.bg.next");
        }

        // monedas (bajo el cuadro)
        if (d.coins >= 0) {
            Component coins = Component.translatable("screen.libertiprofile.coins", d.coins);
            g.drawString(font, coins, 54 - font.width(coins) / 2, 169, 0xFFFFFF, true);
        }
    }

    private void drawInfoPanel(GuiGraphics g, float mx, float my) {
        g.fill(104, 28, 258, 177, 0xFF4A4A4A);
        g.fill(105, 29, 257, 176, 0xFFA8A8A8);

        // salud
        g.blit(ICONS, 110, 33, 16, 0, 9, 9);
        g.blit(ICONS, 110, 33, 52, 0, 9, 9);
        int hp = (int) Math.ceil(d.health);
        int max = (int) Math.ceil(d.maxHealth);
        row(g, 34, "screen.libertiprofile.health", hp + " / " + max, 0xFF5555);
        row(g, 47, "screen.libertiprofile.xp", Integer.toString(d.xpLevel), 0x2FAE3A);
        row(g, 60, "screen.libertiprofile.playtime", formatTime(d.playTicks), 0x1F8FA8);
        row(g, 73, "screen.libertiprofile.kills", Integer.toString(d.kills), 0xB02A2A);
        for (int y : new int[]{44, 57, 70, 83}) g.fill(110, y, 252, y + 1, 0xFF8C8C8C);

        if (!d.self) return;

        // estampillas (solo las ve y las cambia el dueño)
        g.drawString(font, Component.translatable("screen.libertiprofile.stamps").append(":"), 110, 89, TEXT_DARK, false);
        for (int i = 0; i < STAMPS_PER_PAGE; i++) {
            int x = 110 + i * 32;
            int y = 99;
            int idx = stampPage * STAMPS_PER_PAGE + i;
            boolean has = idx < d.stamps.size();
            String id = has ? d.stamps.get(idx) : "";
            boolean selected = has && id.equals(d.stamp);
            boolean hover = has && in(mx, my, x, y, x + 28, y + 28);
            g.fill(x, y, x + 28, y + 28, selected ? 0xFFFFAA00 : 0xFF6E6E6E);
            g.fill(x + 1, y + 1, x + 27, y + 27, hover ? 0xFFD0D0D0 : 0xFFB8B8B8);
            if (has) {
                ImageStore.Tex t = ImageStore.get("stamps", id);
                if (t != null) {
                    g.blit(t.loc(), x + 2, y + 2, 24, 24, 0f, 0f, t.w(), t.h(), t.w(), t.h());
                } else {
                    g.drawString(font, "?", x + 11, y + 10, TEXT_DARK, false);
                }
                if (hover) {
                    tooltip = Component.literal(id + " - ").append(Component.translatable(selected
                            ? "screen.libertiprofile.stamp.unequip" : "screen.libertiprofile.stamp.equip"));
                }
            }
        }
        int pages = Math.max(1, (d.stamps.size() + STAMPS_PER_PAGE - 1) / STAMPS_PER_PAGE);
        arrow(g, 226, 161, 238, 173, "<", stampPage > 0, mx, my, "screen.libertiprofile.page.prev");
        arrow(g, 240, 161, 252, 173, ">", stampPage < pages - 1, mx, my, "screen.libertiprofile.page.next");
    }

    private void drawEquippedStamp(GuiGraphics g) {
        ImageStore.Tex t = ImageStore.get("stamps", d.stamp);
        if (t != null) {
            g.blit(t.loc(), 94, 143, 32, 32, 0f, 0f, t.w(), t.h(), t.w(), t.h());
        }
    }

    private void drawButtons(GuiGraphics g, int frame, float mx, float my) {
        int dark = darken(frame, 0.55f);
        if (!d.self) {
            // desafio PvP
            boolean hover = in(mx, my, 132, 152, 152, 172);
            g.fill(132, 152, 152, 172, dark);
            g.fill(133, 153, 151, 171, hover ? 0xFFD8D8D8 : 0xFFB0B0B0);
            g.renderItem(new ItemStack(Items.IRON_SWORD), 134, 154);
            if (hover) tooltip = Component.translatable("screen.libertiprofile.challenge");
        }

        // corazon (boton para los demas, contador para el dueño)
        boolean active = !d.self && d.canHeart;
        boolean hover = !d.self && in(mx, my, 156, 152, 190, 172);
        g.fill(156, 152, 190, 172, dark);
        g.fill(157, 153, 189, 171, hover && active ? 0xFFD8D8D8 : 0xFFB0B0B0);
        if (!active && !d.self) RenderSystem.setShaderColor(0.45f, 0.45f, 0.45f, 1f);
        g.blit(ICONS, 160, 157, 16, 0, 9, 9);
        g.blit(ICONS, 160, 157, 52, 0, 9, 9);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.drawString(font, Integer.toString(d.hearts), 172, 158, TEXT_DARK, false);
        if (in(mx, my, 156, 152, 190, 172)) {
            tooltip = Component.translatable(d.self ? "screen.libertiprofile.heart.count"
                    : (d.canHeart ? "screen.libertiprofile.heart.give" : "screen.libertiprofile.heart.done"));
        }
    }

    // ------------------------------------------------------------------ ayudas de dibujo

    private void row(GuiGraphics g, int y, String labelKey, String value, int valueColor) {
        MutableComponent line = Component.translatable(labelKey).append(": ");
        g.drawString(font, line, 124, y, TEXT_DARK, false);
        g.drawString(font, value, 124 + font.width(line), y, 0xFF000000 | valueColor, false);
    }

    private void arrow(GuiGraphics g, int x1, int y1, int x2, int y2, String label, boolean enabled,
                       float mx, float my, String tipKey) {
        boolean hover = enabled && in(mx, my, x1, y1, x2, y2);
        g.fill(x1, y1, x2, y2, 0xFF3A3A3A);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, hover ? 0xFFE0E0E0 : (enabled ? 0xFFB8B8B8 : 0xFF8A8A8A));
        int tx = x1 + (x2 - x1 - font.width(label)) / 2;
        int ty = y1 + (y2 - y1 - 8) / 2 + 1;
        g.drawString(font, label, tx, ty, enabled ? 0xFF1A1A1A : 0xFF666666, false);
        if (hover) tooltip = Component.translatable(tipKey);
    }

    private static boolean in(float mx, float my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }

    private static int darken(int argb, float f) {
        int r = (int) (((argb >> 16) & 0xFF) * f);
        int gg = (int) (((argb >> 8) & 0xFF) * f);
        int b = (int) ((argb & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static String formatTime(long ticks) {
        long minutes = ticks / 20 / 60;
        long h = minutes / 60;
        long m = minutes % 60;
        return h + "h " + (m < 10 ? "0" : "") + m + "m";
    }

    private LivingEntity findEntity() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        Player p = mc.level.getPlayerByUUID(d.target);
        if (p != null) return p;
        if (mc.player != null && mc.player.getUUID().equals(d.target)) return mc.player;
        return null;
    }

    // ------------------------------------------------------------------ entrada

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float mx = (float) ((mouseX - ox) / scale);
            float my = (float) ((mouseY - oy) / scale);

            if (!d.self) {
                if (in(mx, my, 132, 152, 152, 172)) {
                    click();
                    ProfileNetwork.CHANNEL.sendToServer(new ActionPacket(ActionPacket.CHALLENGE, "", d.target));
                    return true;
                }
                if (d.canHeart && in(mx, my, 156, 152, 190, 172)) {
                    click();
                    d.canHeart = false;
                    d.hearts++;
                    ProfileNetwork.CHANNEL.sendToServer(new ActionPacket(ActionPacket.HEART, "", d.target));
                    return true;
                }
            } else {
                // fondos
                if (!d.backgrounds.isEmpty()) {
                    if (in(mx, my, 74, 149, 84, 159)) {
                        cycleBackground(-1);
                        return true;
                    }
                    if (in(mx, my, 86, 149, 96, 159)) {
                        cycleBackground(1);
                        return true;
                    }
                }
                // estampillas
                int pages = Math.max(1, (d.stamps.size() + STAMPS_PER_PAGE - 1) / STAMPS_PER_PAGE);
                if (in(mx, my, 226, 161, 238, 173) && stampPage > 0) {
                    click();
                    stampPage--;
                    return true;
                }
                if (in(mx, my, 240, 161, 252, 173) && stampPage < pages - 1) {
                    click();
                    stampPage++;
                    return true;
                }
                for (int i = 0; i < STAMPS_PER_PAGE; i++) {
                    int idx = stampPage * STAMPS_PER_PAGE + i;
                    int x = 110 + i * 32;
                    if (idx < d.stamps.size() && in(mx, my, x, 99, x + 28, 127)) {
                        String id = d.stamps.get(idx);
                        String next = id.equals(d.stamp) ? "" : id;
                        click();
                        d.stamp = next;
                        ProfileNetwork.CHANNEL.sendToServer(new ActionPacket(ActionPacket.SET_STAMP, next, d.target));
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void cycleBackground(int dir) {
        List<String> options = new ArrayList<>();
        options.add("");
        options.addAll(d.backgrounds);
        int i = Math.max(0, options.indexOf(d.background));
        i = (i + dir + options.size()) % options.size();
        String next = options.get(i);
        click();
        d.background = next;
        ProfileNetwork.CHANNEL.sendToServer(new ActionPacket(ActionPacket.SET_BACKGROUND, next, d.target));
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

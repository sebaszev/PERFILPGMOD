package com.liberticraft.profile.client;

import com.liberticraft.profile.LibertiProfile;
import com.liberticraft.profile.network.ProfileNetwork;
import com.liberticraft.profile.network.RequestProfilePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ClientEvents {
    private ClientEvents() {}

    /** Eventos del bus de Forge: clic con la rueda del raton sobre un jugador. */
    @Mod.EventBusSubscriber(modid = LibertiProfile.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Forge {
        private Forge() {}

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientHooks.onDisconnect();
        }

        @SubscribeEvent
        public static void onMouse(InputEvent.MouseButton.Pre event) {
            if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE || event.getAction() != GLFW.GLFW_PRESS) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || mc.player == null || mc.level == null) return;
            Entity looked = mc.crosshairPickEntity;
            if (looked instanceof Player other && other != mc.player) {
                event.setCanceled(true);
                ProfileNetwork.CHANNEL.sendToServer(new RequestProfilePacket(other.getUUID()));
            }
        }
    }

    /** Eventos del bus del mod: limpiar la cache de imagenes al recargar recursos. */
    @Mod.EventBusSubscriber(modid = LibertiProfile.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {}

        @SubscribeEvent
        public static void onReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) rm -> ImageStore.clear());
        }
    }
}

package com.liberticraft.profile.server;

import com.liberticraft.profile.LibertiProfile;
import com.liberticraft.profile.data.ProfileSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LibertiProfile.MODID)
public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void onServerStart(ServerAboutToStartEvent event) {
        AssetService.init();
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        ProfileCommands.register(event.getDispatcher());
    }

    /** Cuenta los jugadores eliminados por otro jugador (PvP). */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim
                && event.getSource().getEntity() instanceof ServerPlayer killer
                && killer != victim) {
            ProfileSavedData data = ProfileSavedData.get(killer.server);
            data.profile(killer.getUUID()).kills++;
            data.setDirty();
        }
    }
}

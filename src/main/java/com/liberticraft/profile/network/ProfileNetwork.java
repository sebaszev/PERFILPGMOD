package com.liberticraft.profile.network;

import com.liberticraft.profile.LibertiProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ProfileNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LibertiProfile.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ProfileNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, RequestProfilePacket.class,
                RequestProfilePacket::encode, RequestProfilePacket::decode, RequestProfilePacket::handle);
        CHANNEL.registerMessage(id++, ProfilePacket.class,
                ProfilePacket::encode, ProfilePacket::decode, ProfilePacket::handle);
        CHANNEL.registerMessage(id++, ActionPacket.class,
                ActionPacket::encode, ActionPacket::decode, ActionPacket::handle);
        CHANNEL.registerMessage(id++, AssetRequestPacket.class,
                AssetRequestPacket::encode, AssetRequestPacket::decode, AssetRequestPacket::handle);
        CHANNEL.registerMessage(id++, AssetPacket.class,
                AssetPacket::encode, AssetPacket::decode, AssetPacket::handle);
    }
}

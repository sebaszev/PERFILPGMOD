package com.liberticraft.profile;

import com.liberticraft.profile.network.ProfileNetwork;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(LibertiProfile.MODID)
public class LibertiProfile {
    public static final String MODID = "libertiprofile";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LibertiProfile() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ProfileConfig.SPEC, "libertiprofile-common.toml");
        ProfileNetwork.register();
    }
}

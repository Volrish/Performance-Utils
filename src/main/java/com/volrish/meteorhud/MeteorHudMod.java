package com.volrish.meteorhud;

import com.volrish.meteorhud.network.MeteorCrashPayload;
import com.volrish.meteorhud.network.MeteorSpawnPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeteorHudMod implements ModInitializer {

    public static final String MOD_ID = "meteorhud";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Register both S2C packets so a server companion mod can drive the client
        PayloadTypeRegistry.playS2C().register(MeteorSpawnPayload.ID,  MeteorSpawnPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MeteorCrashPayload.ID,  MeteorCrashPayload.CODEC);

        LOGGER.info("[MeteorHUD] Initialized");
    }
}

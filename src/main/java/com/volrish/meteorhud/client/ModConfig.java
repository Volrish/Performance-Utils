package com.volrish.meteorhud.client;

import com.google.gson.*;
import com.volrish.meteorhud.MeteorHudMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

/**
 * ============================================================
 *  MOD CONFIG  —  config/meteorhud/config.json
 * ============================================================
 *
 *  All toggles and URLs live here. Changes are saved to disk
 *  automatically when you click a button in the settings screen.
 *
 *  TO ADD A NEW TOGGLE:
 *    1. Add a public field below (e.g. public boolean myFeature = true;)
 *    2. Add obj.addProperty("myFeature", myFeature); in save()
 *    3. Add if (obj.has("myFeature")) myFeature = ... in load()
 *    4. Use ModConfig.getInstance().myFeature in your renderer
 */
public class ModConfig {

    private static final ModConfig INSTANCE = new ModConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path savePath;

    // ---- METEOR HUD ----
    /** Show/hide the main meteor HUD panel */
    public boolean meteorHudEnabled  = true;

    /** Show/hide the "Status: Safe/Wild/Danger" row */
    public boolean showStatusRow     = true;

    /** Overall HUD opacity — 0.1 (nearly invisible) to 1.0 (fully opaque) */
    public float   hudOpacity        = 1.0f;

    // ---- BEAM RENDERER ----
    /** Show/hide the colored beacon beam at each active meteor */
    public boolean beamEnabled       = true;

    // ---- CHAT DETECTION ----
    /** Parse chat messages to detect meteor/merchant events */
    public boolean chatParseEnabled  = true;

    // ---- ZONE SYNC ----
    /** Fetch zones.json and friends.json from GitHub on login */
    public boolean zoneSyncEnabled   = true;

    /**
     * URL to your zones.json file on GitHub.
     * Format: https://raw.githubusercontent.com/YOU/REPO/main/zones.json
     */
    public String  zonesUrl  = "https://raw.githubusercontent.com/Volrish/volrish-access-list/main/zones.json";

    /**
     * URL to your friends.json file on GitHub.
     * Format: https://raw.githubusercontent.com/YOU/REPO/main/friends.json
     */
    public String  friendsUrl = "https://raw.githubusercontent.com/Volrish/volrish-access-list/main/friends.json";

    // ---- MERCHANT HUD ----
    /** Show/hide the merchant tracker panel */
    public boolean merchantHudEnabled = true;

    // ---- UTILS HUD (satchels + absorbers) ----
    /** Show/hide the utils icon grid */
    public boolean absorberHudEnabled = true;

    // ---- KEYBINDS ----
    // GLFW key codes: https://www.glfw.org/docs/latest/group__keys.html
    // Common codes: K=75, J=74, O=79, L=76, P=80, G=71, H=72, N=78
    // Change these numbers to reassign keys without recompiling.
    // Set to -1 to disable a keybind entirely.
    public int keyAbsorberToggle  = 75;  // K
    public int keyAbsorberManual  = 74;  // J
    public int keyCombineToggle   = 79;  // O
    public int keySellToggle      = 76;  // L
    public int keyMerchantArchive = 77;  // M — open merchant archive GUI

    // ---- AUTO MODS ----
    /** Enable/disable Auto Absorber (key: K) */
    public boolean autoAbsorberEnabled = true;
    /** Enable/disable Auto Combine  (key: O) */
    public boolean autoCombineEnabled  = true;
    /** Enable/disable Auto Sell     (key: L) */
    public boolean autoSellEnabled     = true;

    // ---- COOLDOWN HUD ----
    /** Show/hide the cooldown timer panel */
    public boolean cooldownHudEnabled  = true;

    // ---- DISPLAY ----
    public boolean transparentHud       = false;
    /** Show nearby players HUD (hides in safe zones). */
    public boolean nearbyPlayersHudEnabled = true;
    public boolean compassHudEnabled       = true;
    public boolean itemValueOverlayEnabled = true;
    /** Show gang + outpost status HUD. */

    // ---- LEGACY (unused, kept for config compatibility) ----
    public boolean showDescription   = true;
    public boolean showMeteorList    = true;

    // ============================================================
    //  Internals — you shouldn't need to change anything below
    // ============================================================

    private ModConfig() {
        savePath = FabricLoader.getInstance()
                .getConfigDir().resolve("meteorhud").resolve("config.json");
        load();
    }

    public static ModConfig getInstance() { return INSTANCE; }

    public void save() {
        try {
            File dir = savePath.getParent().toFile();
            if (!dir.exists()) dir.mkdirs();
            JsonObject obj = new JsonObject();
            obj.addProperty("meteorHudEnabled",   meteorHudEnabled);
            obj.addProperty("showStatusRow",       showStatusRow);
            obj.addProperty("hudOpacity",          hudOpacity);
            obj.addProperty("beamEnabled",         beamEnabled);
            obj.addProperty("chatParseEnabled",    chatParseEnabled);
            obj.addProperty("zoneSyncEnabled",     zoneSyncEnabled);
            obj.addProperty("zonesUrl",            zonesUrl);
            obj.addProperty("friendsUrl",          friendsUrl);
            obj.addProperty("merchantHudEnabled",  merchantHudEnabled);
            obj.addProperty("absorberHudEnabled",  absorberHudEnabled);
            obj.addProperty("keyAbsorberToggle",   keyAbsorberToggle);
            obj.addProperty("keyAbsorberManual",   keyAbsorberManual);
            obj.addProperty("keyCombineToggle",    keyCombineToggle);
            obj.addProperty("keySellToggle",       keySellToggle);
            obj.addProperty("keyMerchantArchive", keyMerchantArchive);
            obj.addProperty("autoAbsorberEnabled",  autoAbsorberEnabled);
            obj.addProperty("autoCombineEnabled",    autoCombineEnabled);
            obj.addProperty("autoSellEnabled",       autoSellEnabled);
            obj.addProperty("cooldownHudEnabled",    cooldownHudEnabled);
            obj.addProperty("transparentHud",          transparentHud);
            obj.addProperty("nearbyPlayersHudEnabled", nearbyPlayersHudEnabled);
            obj.addProperty("compassHudEnabled",       compassHudEnabled);
            obj.addProperty("itemValueOverlayEnabled", itemValueOverlayEnabled);
            try (Writer w = new FileWriter(savePath.toFile())) { GSON.toJson(obj, w); }
        } catch (IOException e) {
            MeteorHudMod.LOGGER.warn("[MeteorHUD] Failed to save config", e);
        }
    }

    private void load() {
        File file = savePath.toFile();
        if (!file.exists()) return;
        try (Reader r = new FileReader(file)) {
            JsonObject obj = GSON.fromJson(r, JsonObject.class);
            if (obj == null) return;
            if (obj.has("meteorHudEnabled"))   meteorHudEnabled   = obj.get("meteorHudEnabled").getAsBoolean();
            if (obj.has("showStatusRow"))       showStatusRow       = obj.get("showStatusRow").getAsBoolean();
            if (obj.has("hudOpacity"))          hudOpacity          = obj.get("hudOpacity").getAsFloat();
            if (obj.has("beamEnabled"))         beamEnabled         = obj.get("beamEnabled").getAsBoolean();
            if (obj.has("chatParseEnabled"))    chatParseEnabled    = obj.get("chatParseEnabled").getAsBoolean();
            if (obj.has("zoneSyncEnabled"))     zoneSyncEnabled     = obj.get("zoneSyncEnabled").getAsBoolean();
            if (obj.has("zonesUrl"))            zonesUrl            = obj.get("zonesUrl").getAsString();
            if (obj.has("friendsUrl"))          friendsUrl          = obj.get("friendsUrl").getAsString();
            if (obj.has("merchantHudEnabled"))  merchantHudEnabled  = obj.get("merchantHudEnabled").getAsBoolean();
            if (obj.has("absorberHudEnabled"))  absorberHudEnabled  = obj.get("absorberHudEnabled").getAsBoolean();
            if (obj.has("keyAbsorberToggle"))   keyAbsorberToggle   = obj.get("keyAbsorberToggle").getAsInt();
            if (obj.has("keyAbsorberManual"))   keyAbsorberManual   = obj.get("keyAbsorberManual").getAsInt();
            if (obj.has("keyCombineToggle"))    keyCombineToggle    = obj.get("keyCombineToggle").getAsInt();
            if (obj.has("keySellToggle"))       keySellToggle       = obj.get("keySellToggle").getAsInt();
            if (obj.has("keyMerchantArchive")) keyMerchantArchive = obj.get("keyMerchantArchive").getAsInt();
            if (obj.has("autoAbsorberEnabled"))  autoAbsorberEnabled  = obj.get("autoAbsorberEnabled").getAsBoolean();
            if (obj.has("autoCombineEnabled"))    autoCombineEnabled    = obj.get("autoCombineEnabled").getAsBoolean();
            if (obj.has("autoSellEnabled"))       autoSellEnabled       = obj.get("autoSellEnabled").getAsBoolean();
            if (obj.has("cooldownHudEnabled"))    cooldownHudEnabled    = obj.get("cooldownHudEnabled").getAsBoolean();
            if (obj.has("transparentHud"))          transparentHud          = obj.get("transparentHud").getAsBoolean();
            if (obj.has("nearbyPlayersHudEnabled")) nearbyPlayersHudEnabled = obj.get("nearbyPlayersHudEnabled").getAsBoolean();
            if (obj.has("compassHudEnabled"))       compassHudEnabled       = obj.get("compassHudEnabled").getAsBoolean();
            if (obj.has("itemValueOverlayEnabled")) itemValueOverlayEnabled = obj.get("itemValueOverlayEnabled").getAsBoolean();
        } catch (Exception e) {
            MeteorHudMod.LOGGER.warn("[MeteorHUD] Failed to load config", e);
        }
    }
}

package com.volrish.meteorhud.client;

import com.google.gson.*;
import com.volrish.meteorhud.MeteorHudMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 *  HUD POSITION — x, y, width, opacity, scale per HUD panel
 * ============================================================
 *
 *  Scale works by multiplying panelWidth so ALL layout math
 *  (text, icons, fills) automatically uses the scaled size.
 *  This avoids the drawItem matrix bug from previous versions.
 *
 *  HUD IDs:
 *    METEOR    — Meteor HUD
 *    MERCHANT  — Merchant HUD
 *    ABSORBER  — Utils/Satchel HUD
 *    NEARBY    — Nearby Players HUD
 *    COMPASS   — Compass HUD (top-center, fixed position)
 *
 *  TO ADD A NEW HUD PANEL:
 *    1. Add a public static final String id
 *    2. Add default x/y to DEFAULTS
 *    3. Register in HudLayoutScreen HUD_IDS list
 *    4. Call HudPosition.get(HudPosition.YOURID) in your renderer
 */
public class HudPosition {

    // ---- HUD IDs ----
    public static final String METEOR   = "meteor";
    public static final String MERCHANT = "merchant";
    public static final String ABSORBER = "absorber";
    public static final String NEARBY   = "nearby";
    public static final String COMPASS  = "compass";

    // ---- Width limits ----
    public static final int   WIDTH_MIN     = 80;
    public static final int   WIDTH_MAX     = 600;
    public static final int   WIDTH_DEFAULT = 220;

    // ---- Scale limits ----
    public static final float SCALE_MIN     = 0.5f;
    public static final float SCALE_MAX     = 2.5f;
    public static final float SCALE_DEFAULT = 1.0f;
    public static final float SCALE_STEP    = 0.1f;

    // ---- Default screen positions {x, y} ----
    private static final Map<String, int[]> DEFAULTS = new java.util.HashMap<>(Map.of(
        METEOR,   new int[]{ 8,   8   },
        MERCHANT, new int[]{ 8,   200 },
        ABSORBER, new int[]{ 240, 8   },
        NEARBY,   new int[]{ 8,   340 },
        COMPASS,  new int[]{ 0,   0   }   // compass is centered dynamically
    ));

    // ---- Instance fields ----
    public int   x, y;
    public int   panelWidth;
    public float opacity;
    public float scale;        // scale multiplied into effective width at render time
    public final String id;

    private HudPosition(String id, int defX, int defY) {
        this.id         = id;
        this.x          = defX;
        this.y          = defY;
        this.panelWidth = WIDTH_DEFAULT;
        this.opacity    = 1.0f;
        this.scale      = SCALE_DEFAULT;
    }

    /**
     * Returns the effective panel width (panelWidth × scale).
     * All renderers should use this for layout math.
     */
    public int effectiveWidth() {
        return Math.max(WIDTH_MIN, Math.round(panelWidth * scale));
    }

    // ---- Registry ----
    private static final Map<String, HudPosition> REGISTRY = new HashMap<>();
    private static final Path SAVE_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("meteorhud").resolve("hud_positions.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean loaded = false;

    public static HudPosition get(String id) {
        if (!loaded) loadAll();
        return REGISTRY.computeIfAbsent(id, k -> {
            int[] def = DEFAULTS.getOrDefault(k, new int[]{8, 8});
            return new HudPosition(k, def[0], def[1]);
        });
    }

    public static HudPosition getInstance() { return get(METEOR); }

    public void set(int x, int y) { this.x = x; this.y = y; saveAll(); }

    public void reset() {
        int[] def   = DEFAULTS.getOrDefault(id, new int[]{8, 8});
        x           = def[0];
        y           = def[1];
        panelWidth  = WIDTH_DEFAULT;
        opacity     = 1.0f;
        scale       = SCALE_DEFAULT;
        saveAll();
    }

    // ---- Persistence ----
    public static void saveAll() {
        try {
            File dir = SAVE_PATH.getParent().toFile();
            if (!dir.exists()) dir.mkdirs();
            JsonObject root = new JsonObject();
            for (Map.Entry<String, HudPosition> e : REGISTRY.entrySet()) {
                HudPosition p = e.getValue();
                JsonObject obj = new JsonObject();
                obj.addProperty("x",          p.x);
                obj.addProperty("y",          p.y);
                obj.addProperty("panelWidth", p.panelWidth);
                obj.addProperty("opacity",    p.opacity);
                obj.addProperty("scale",      p.scale);
                root.add(e.getKey(), obj);
            }
            try (Writer w = new FileWriter(SAVE_PATH.toFile())) { GSON.toJson(root, w); }
        } catch (IOException e) {
            MeteorHudMod.LOGGER.warn("[PerfUtils] Failed to save HUD positions", e);
        }
    }

    private static void loadAll() {
        loaded = true;
        File file = SAVE_PATH.toFile();
        if (!file.exists()) return;
        try (Reader r = new FileReader(file)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return;
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                String id = e.getKey();
                JsonObject obj = e.getValue().getAsJsonObject();
                int[] def = DEFAULTS.getOrDefault(id, new int[]{8, 8});
                HudPosition p = new HudPosition(id, def[0], def[1]);
                if (obj.has("x"))          p.x          = obj.get("x").getAsInt();
                if (obj.has("y"))          p.y          = obj.get("y").getAsInt();
                if (obj.has("panelWidth")) p.panelWidth = obj.get("panelWidth").getAsInt();
                if (obj.has("opacity"))    p.opacity    = obj.get("opacity").getAsFloat();
                if (obj.has("scale"))      p.scale      = obj.get("scale").getAsFloat();
                REGISTRY.put(id, p);
            }
        } catch (Exception e) {
            MeteorHudMod.LOGGER.warn("[PerfUtils] Failed to load HUD positions", e);
        }
    }
}

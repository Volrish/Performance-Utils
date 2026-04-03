package com.volrish.meteorhud.client;

import com.google.gson.*;
import com.volrish.meteorhud.MeteorHudMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class WaypointManager {

    private static final WaypointManager INSTANCE = new WaypointManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, MeteorWaypoint> meteors = new LinkedHashMap<>();
    private final Path savePath;

    private WaypointManager() {
        savePath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("meteorhud")
                .resolve("meteors.json");
        load();
    }

    public static WaypointManager getInstance() { return INSTANCE; }

    public synchronized void add(MeteorWaypoint wp) {
        meteors.put(wp.id, wp);
        save();
    }

    public synchronized MeteorWaypoint remove(String id) {
        MeteorWaypoint removed = meteors.remove(id);
        if (removed != null) save();
        return removed;
    }

    public synchronized void clearAll() {
        meteors.clear();
        save();
    }

    public synchronized List<MeteorWaypoint> getAll() {
        return new ArrayList<>(meteors.values());
    }

    public synchronized Optional<MeteorWaypoint> get(String id) {
        return Optional.ofNullable(meteors.get(id));
    }

    public void save() {
        try {
            File dir = savePath.getParent().toFile();
            if (!dir.exists()) dir.mkdirs();

            JsonArray arr = new JsonArray();
            for (MeteorWaypoint wp : meteors.values()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id",           wp.id);
                obj.addProperty("location",     wp.location.ordinal());
                obj.addProperty("trigger",      wp.trigger.ordinal());
                obj.addProperty("x",            wp.x);
                obj.addProperty("y",            wp.y);
                obj.addProperty("z",            wp.z);
                obj.addProperty("summonerName", wp.summonerName);
                obj.addProperty("visible",      wp.visible);
                arr.add(obj);
            }

            try (Writer w = new FileWriter(savePath.toFile())) {
                GSON.toJson(arr, w);
            }
        } catch (IOException e) {
            MeteorHudMod.LOGGER.error("[MeteorHUD] Failed to save meteors", e);
        }
    }

    private void load() {
        File file = savePath.toFile();
        if (!file.exists()) return;

        try (Reader r = new FileReader(file)) {
            JsonArray arr = GSON.fromJson(r, JsonArray.class);
            if (arr == null) return;

            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String id              = obj.get("id").getAsString();
                MeteorLocation location = MeteorLocation.fromOrdinal(obj.get("location").getAsInt());
                MeteorTrigger  trigger  = MeteorTrigger.fromOrdinal(obj.get("trigger").getAsInt());
                int x                  = obj.get("x").getAsInt();
                int y                  = obj.get("y").getAsInt();
                int z                  = obj.get("z").getAsInt();
                String name            = obj.has("summonerName") ? obj.get("summonerName").getAsString() : "";

                MeteorWaypoint wp = new MeteorWaypoint(id, location, trigger, x, y, z, name);
                wp.visible = !obj.has("visible") || obj.get("visible").getAsBoolean();
                meteors.put(id, wp);
            }

            MeteorHudMod.LOGGER.info("[MeteorHUD] Loaded {} saved meteors", meteors.size());
        } catch (Exception e) {
            MeteorHudMod.LOGGER.error("[MeteorHUD] Failed to load meteors", e);
        }
    }
}

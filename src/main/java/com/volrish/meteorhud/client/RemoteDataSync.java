package com.volrish.meteorhud.client;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.volrish.meteorhud.MeteorHudMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/**
 * ============================================================
 *  REMOTE DATA SYNC — fetches zones.json and friends.json
 * ============================================================
 *
 *  Both files live in your GitHub repo and are fetched on login.
 *  A local cache is saved so the mod still works if GitHub is unreachable.
 *
 *  ---- ZONES.JSON FORMAT ----
 *
 *  Each zone is a rectangle defined by two corners (minX/maxX, minZ/maxZ).
 *  Y coordinate is ignored — only X and Z matter.
 *
 *  Basic zone:
 *  {
 *    "minX": 1080, "maxX": 1440,
 *    "minZ": -95,  "maxZ": 205,
 *    "name": "Diamond Mine"
 *  }
 *
 *  Planet-specific zone (only active on that planet):
 *  {
 *    "minX": 500, "maxX": 900,
 *    "minZ": 100, "maxZ": 500,
 *    "name": "Aether Gold Mine",
 *    "planet": "aether"
 *  }
 *  Supported planet values: "celestial" (default), "aether", "*" (all planets)
 *
 *  TO ADD A ZONE: just add a new entry to the JSON array.
 *  TO FIND COORDINATES: open Xaero's World Map, hover cursor over mine edges,
 *  read X and Z from the top of the screen.
 *
 *  ---- FRIENDS.JSON FORMAT ----
 *  {
 *    "friends": ["Volrish", "FriendIGN"]
 *  }
 *  Friend IGNs are case-insensitive. When a meteor is summoned by a friend,
 *  the HUD shows [Friend] in green instead of [Unknown] in red.
 */
public class RemoteDataSync {

    // ---- Zone definition ----

    public static class RemoteZone {
        public int    minX, maxX, minZ, maxZ;
        public String name   = "";
        public String type   = "safe";       // "safe" or "wild" (future use)
        public String planet = "celestial";  // which planet this zone belongs to

        /** Returns true if the given world coordinates are inside this rectangle */
        public boolean contains(int px, int pz) {
            return px >= minX && px <= maxX && pz >= minZ && pz <= maxZ;
        }

        /** Distance from point to zone center — used to find nearest zone */
        public double distanceTo(int px, int pz) {
            int cx = (minX + maxX) / 2;
            int cz = (minZ + maxZ) / 2;
            double dx = px - cx, dz = pz - cz;
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    // ---- State ----

    private static final Gson       GSON      = new Gson();
    private static final Type       ZONE_TYPE = new TypeToken<List<RemoteZone>>(){}.getType();
    private static final HttpClient HTTP      = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).build();

    private static volatile List<RemoteZone> zones         = new ArrayList<>();
    private static volatile List<String>     friends       = new ArrayList<>();
    private static volatile boolean          fetching      = false;

    /** Current planet — set automatically when you join a world */
    private static volatile String           currentPlanet = "celestial";

    // ---- Planet detection ----

    /**
     * Called on world join with the dimension path (e.g. "overworld", "aether").
     * Automatically detects which planet you're on.
     * TO ADD A NEW PLANET: add an else-if checking dimensionPath.contains("yourplanet")
     */
    public static void setPlanet(String dimensionPath) {
        if (dimensionPath.contains("aether")) currentPlanet = "aether";
        else                                   currentPlanet = "celestial";
        MeteorHudMod.LOGGER.info("[MeteorHUD] Planet: {}", currentPlanet);
    }

    public static String getCurrentPlanet() { return currentPlanet; }

    // ---- Fetch ----

    /** Called on login — fetches zones and friends in a background thread */
    public static void fetchAsync() {
        if (fetching) return;
        fetching = true;
        Thread.ofVirtual().start(() -> {
            try { fetchZones(); fetchFriends(); }
            finally { fetching = false; }
        });
    }

    private static void fetchZones() {
        String url = ModConfig.getInstance().zonesUrl;
        if (url == null || url.isBlank()) return;
        try {
            String body = get(url);
            List<RemoteZone> fetched = GSON.fromJson(body, ZONE_TYPE);
            if (fetched != null) {
                zones = fetched;
                cache("zones_cache.json", body);
                MeteorHudMod.LOGGER.info("[MeteorHUD] Loaded {} zone(s).", fetched.size());
            }
        } catch (Exception e) {
            MeteorHudMod.LOGGER.warn("[MeteorHUD] Zone fetch failed: {}", e.getMessage());
            // Fall back to cached version
            String cached = loadCache("zones_cache.json");
            if (cached != null) {
                List<RemoteZone> fb = GSON.fromJson(cached, ZONE_TYPE);
                if (fb != null) zones = fb;
            }
        }
    }

    private static void fetchFriends() {
        String url = ModConfig.getInstance().friendsUrl;
        if (url == null || url.isBlank()) return;
        try {
            String body = get(url);
            JsonObject obj = GSON.fromJson(body, JsonObject.class);
            if (obj != null && obj.has("friends")) {
                List<String> raw = GSON.fromJson(obj.get("friends"),
                        new TypeToken<List<String>>(){}.getType());
                if (raw != null) {
                    // Store lowercase for case-insensitive matching
                    List<String> lower = raw.stream().map(String::toLowerCase).toList();
                    friends = lower;
                    cache("friends_cache.json", body);
                    MeteorHudMod.LOGGER.info("[MeteorHUD] Loaded {} friend(s).", lower.size());
                }
            }
        } catch (Exception e) {
            MeteorHudMod.LOGGER.warn("[MeteorHUD] Friends fetch failed: {}", e.getMessage());
            String cached = loadCache("friends_cache.json");
            if (cached != null) {
                JsonObject obj = GSON.fromJson(cached, JsonObject.class);
                if (obj != null && obj.has("friends")) {
                    List<String> raw = GSON.fromJson(obj.get("friends"),
                            new TypeToken<List<String>>(){}.getType());
                    if (raw != null) friends = raw.stream().map(String::toLowerCase).toList();
                }
            }
        }
    }

    // ---- Zone lookups ----

    /**
     * Returns the zone at these coordinates on the current planet, or null.
     * This is how the mod knows if a meteor is Safe, Wild, or Danger.
     */
    public static RemoteZone findZone(int x, int z) {
        for (RemoteZone zone : zones) {
            if (!zone.planet.equals("*") && !zone.planet.equalsIgnoreCase(currentPlanet)) continue;
            if (zone.contains(x, z)) return zone;
        }
        return null;
    }

    /** Returns GUARDED (safe zone) or UNKNOWN (wild), used for meteor status */
    public static MeteorLocation resolveZone(int x, int z) {
        return findZone(x, z) != null ? MeteorLocation.GUARDED : MeteorLocation.UNKNOWN;
    }

    /** Returns the zone name at these coords (e.g. "Gold Mine"), or null */
    public static String zoneName(int x, int z) {
        RemoteZone zone = findZone(x, z);
        if (zone == null || zone.name == null || zone.name.isBlank()) return null;
        return zone.name;
    }

    /** Returns true if these coordinates are inside any safe zone */
    public static boolean isInAnyZone(int x, int z) {
        return findZone(x, z) != null;
    }

    /** Returns the name of the closest named zone (used for "Near X Mine" label) */
    public static String nearestZoneName(int x, int z) {
        RemoteZone best = null;
        double bestDist = Double.MAX_VALUE;
        for (RemoteZone zone : zones) {
            if (zone.name == null || zone.name.isBlank()) continue;
            if (!zone.planet.equals("*") && !zone.planet.equalsIgnoreCase(currentPlanet)) continue;
            double d = zone.distanceTo(x, z);
            if (d < bestDist) { bestDist = d; best = zone; }
        }
        return best != null ? best.name : null;
    }

    /**
     * Returns the nearest zone name AND distance in blocks from its center.
     * Used to display "[430 blocks] Gold Mine" instead of "Near Gold Mine".
     * Returns null if no zones are loaded.
     */
    public static String nearestZoneLabel(int x, int z) {
        RemoteZone best = null;
        double bestDist = Double.MAX_VALUE;
        for (RemoteZone zone : zones) {
            if (zone.name == null || zone.name.isBlank()) continue;
            if (!zone.planet.equals("*") && !zone.planet.equalsIgnoreCase(currentPlanet)) continue;
            double d = zone.distanceTo(x, z);
            if (d < bestDist) { bestDist = d; best = zone; }
        }
        if (best == null) return null;
        int dist = (int) Math.round(bestDist);
        return "[" + dist + " blocks] " + best.name;
    }

    // ---- Friend lookup ----

    /** Returns the full friends list (lowercase). */
    public static List<String> getFriendNames() { return new java.util.ArrayList<>(friends); }

    /** Returns true if the IGN is in your friends.json (case-insensitive) */
    public static boolean isFriend(String ign) {
        if (ign == null || ign.isBlank()) return false;
        return friends.contains(ign.toLowerCase());
    }

    public static int zoneCount()   { return zones.size(); }
    public static int friendCount() { return friends.size(); }

    // ---- HTTP + cache helpers ----

    private static String get(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url)).timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json").GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new IOException("HTTP " + resp.statusCode());
        return resp.body();
    }

    private static Path cacheDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("meteorhud");
    }

    private static void cache(String filename, String content) {
        try { Files.createDirectories(cacheDir()); Files.writeString(cacheDir().resolve(filename), content); }
        catch (IOException ignored) {}
    }

    private static String loadCache(String filename) {
        try { Path f = cacheDir().resolve(filename); if (Files.exists(f)) return Files.readString(f); }
        catch (IOException ignored) {}
        return null;
    }
}

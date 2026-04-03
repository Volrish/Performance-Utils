package com.volrish.meteorhud.client.chat;

import com.volrish.meteorhud.MeteorHudMod;
import com.volrish.meteorhud.client.*;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ============================================================
 *  CHAT LISTENER — reads server chat and fires HUD events
 * ============================================================
 *
 *  HOW IT WORKS:
 *  The server sends meteor/merchant info across multiple chat lines.
 *  We keep a rolling buffer of the last 4 lines and scan them together.
 *  When a pattern matches, we clear the buffer to avoid double-firing.
 *
 *  ---- METEOR PATTERNS ----
 *  Server sends something like:
 *    "(!) A meteor is falling from the sky at:"
 *    "-863x, 67y, 603z"
 *
 *  ---- MERCHANT PATTERNS ----
 *  Server sends something like:
 *    "(!) A Gold Ore Merchant traveled to 710x, 125y, -880z"
 *    "(!) A Gold Ore Merchant has been slain by ChosenCuh at 710x, 126y, -880z"
 *
 *  TO ADD A NEW EVENT TYPE:
 *    1. Add a Pattern constant below
 *    2. Add a Matcher check inside handleMessage()
 *    3. Call the appropriate handler
 */
public class ChatMeteorListener {

    // ---- COORDINATE FORMAT ----
    // Matches numbers like: 467  or  467x  or  -863x
    private static final String COORD = "(-?\\d+)(?:[xyz])?";

    // ---- METEOR PATTERNS ----

    /** Natural meteor: "(!) A meteor is falling from the sky at: -863x, 67y, 603z" */
    private static final Pattern NATURAL_FALLING = Pattern.compile(
            "\\(!\\)\\s+A meteor is falling from the sky at:[\\s,]*"
            + COORD + "[,\\s]+" + COORD + "[,\\s]+" + COORD,
            Pattern.CASE_INSENSITIVE);

    /** Player-summoned meteor: "(!) A meteor summoned by PlayerName is falling..." */
    private static final Pattern PLAYER_FALLING = Pattern.compile(
            "\\(!\\)\\s+A meteor summoned by (.+?) is falling from the sky at:[\\s,]*"
            + COORD + "[,\\s]+" + COORD + "[,\\s]+" + COORD,
            Pattern.CASE_INSENSITIVE);

    /** Meteor crash: "(!) A meteor has crashed at: -863x, 67y, 603z" */
    private static final Pattern CRASHED = Pattern.compile(
            "\\(!\\)\\s+A meteor has crashed at:[\\s,]*"
            + COORD + "[,\\s]+" + COORD + "[,\\s]+" + COORD,
            Pattern.CASE_INSENSITIVE);

    // ---- MERCHANT PATTERNS ----

    /**
     * Merchant spawn: "(!) A Gold Ore Merchant traveled to 710x, 125y, -880z"
     * The ore type (e.g. "Gold") is captured in group 1.
     * Supported types: Coal, Iron, Lapis, Redstone, Gold, Diamond, Emerald
     *
     * TO SUPPORT A NEW MERCHANT TYPE: just add it to MerchantWaypoint.MerchantType.fromChat()
     */
    private static final Pattern MERCHANT_SPAWN = Pattern.compile(
            "\\(!\\)\\s+A (\\w+(?:\\s+\\w+)*?)\\s+Ore Merchant traveled to[\\s,]*"
            + COORD + "[,\\s]+" + COORD + "[,\\s]+" + COORD,
            Pattern.CASE_INSENSITIVE);

    /**
     * Merchant killed: "(!) A Gold Ore Merchant has been slain by ... at 710x, 126y, -880z"
     */
    private static final Pattern MERCHANT_KILLED = Pattern.compile(
            "\\(!\\)\\s+A (\\w+(?:\\s+\\w+)*?)\\s+Ore Merchant has been slain.*?(?:at[\\s,]*)?"
            + COORD + "[,\\s]+" + COORD + "[,\\s]+" + COORD,
            Pattern.CASE_INSENSITIVE);

    // ---- INTERNALS ----

    /** Rolling buffer — joins last 4 lines so coords on the next line are caught */
    private static final Deque<String> BUFFER      = new ArrayDeque<>();
    private static final int           BUFFER_SIZE = 4;
    private static final AtomicInteger COUNTER     = new AtomicInteger(0);

    public static void register() {
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, timestamp) -> {
            handleMessage(message.getString());
            return true;
        });
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!overlay) handleMessage(message.getString());
            return true;
        });
    }

    private static synchronized void handleMessage(String raw) {
        if (!ModConfig.getInstance().chatParseEnabled) return;

        // Add line to buffer, drop oldest if full
        BUFFER.addLast(raw);
        if (BUFFER.size() > BUFFER_SIZE) BUFFER.pollFirst();

        // Detect planet change via server chat messages
        // e.g. "Welcome to Celestial!" or "Teleporting to Aether..."
        detectPlanet(raw);

        // Only scan meteor/merchant patterns if trigger prefix present
        String joined = String.join(" ", BUFFER);
        if (!joined.contains("(!)")) return;

        // Check patterns in priority order: crash > player meteor > natural meteor > merchant kill > merchant spawn

        Matcher cm = CRASHED.matcher(joined);
        if (cm.find()) {
            BUFFER.clear();
            handleCrash(parseInt(cm.group(1)), parseInt(cm.group(2)), parseInt(cm.group(3)));
            return;
        }

        Matcher pm = PLAYER_FALLING.matcher(joined);
        if (pm.find()) {
            BUFFER.clear();
            String summoner = pm.group(1).trim();
            int x = parseInt(pm.group(2)), y = parseInt(pm.group(3)), z = parseInt(pm.group(4));
            handleFalling(RemoteDataSync.resolveZone(x, z), MeteorTrigger.PLAYER_SUMMON, x, y, z, summoner);
            return;
        }

        Matcher nm = NATURAL_FALLING.matcher(joined);
        if (nm.find()) {
            BUFFER.clear();
            int x = parseInt(nm.group(1)), y = parseInt(nm.group(2)), z = parseInt(nm.group(3));
            handleFalling(RemoteDataSync.resolveZone(x, z), MeteorTrigger.NATURAL, x, y, z, "");
            return;
        }

        Matcher mk = MERCHANT_KILLED.matcher(joined);
        if (mk.find()) {
            BUFFER.clear();
            MerchantWaypoint.MerchantType type = MerchantWaypoint.MerchantType.fromChat(mk.group(1));
            int x = parseInt(mk.group(2)), y = parseInt(mk.group(3)), z = parseInt(mk.group(4));
            if (type != null) {
                MerchantManager.getInstance().markKilled(type, x, z);
                MeteorHudMod.LOGGER.info("[MeteorHUD] Merchant killed: {} at {},{},{}", type, x, y, z);
            }
            return;
        }

        Matcher ms = MERCHANT_SPAWN.matcher(joined);
        if (ms.find()) {
            BUFFER.clear();
            MerchantWaypoint.MerchantType type = MerchantWaypoint.MerchantType.fromChat(ms.group(1));
            int x = parseInt(ms.group(2)), y = parseInt(ms.group(3)), z = parseInt(ms.group(4));
            if (type != null) {
                String id = MerchantManager.getInstance().nextId();
                MerchantManager.getInstance().add(new MerchantWaypoint(id, type, x, y, z));
                MeteorHudMod.LOGGER.info("[MeteorHUD] Merchant spawned: {} at {},{},{}", type, x, y, z);
            }
        }
    }

    // ---- Planet detection ----
    // TO ADD A NEW PLANET: add an else-if with the chat message keyword
    private static void detectPlanet(String raw) {
        String lo = raw.toLowerCase();
        // Server sends messages when you travel to a planet
        // Common patterns: "Welcome to X", "Traveling to X", "You are now on X"
        if (lo.contains("celestial") && (lo.contains("welcome") || lo.contains("teleport")
                || lo.contains("planet") || lo.contains("arrived") || lo.contains("warp"))) {
            RemoteDataSync.setPlanet("celestial");
        } else if (lo.contains("aether") && (lo.contains("welcome") || lo.contains("teleport")
                || lo.contains("planet") || lo.contains("arrived") || lo.contains("warp"))) {
            RemoteDataSync.setPlanet("aether");
        }
    }

    // ---- EVENT HANDLERS ----

    private static void handleFalling(MeteorLocation loc, MeteorTrigger trig,
                                       int x, int y, int z, String summoner) {
        // Deduplicate — same X,Z already tracked means server broadcast it twice
        for (MeteorWaypoint existing : WaypointManager.getInstance().getAll()) {
            if (existing.x == x && existing.z == z) {
                MeteorHudMod.LOGGER.info("[MeteorHUD] Duplicate suppressed at {},{},{}", x, y, z);
                return;
            }
        }
        String id = "meteor_" + COUNTER.getAndIncrement();
        WaypointManager.getInstance().add(new MeteorWaypoint(id, loc, trig, x, y, z, summoner));
        MeteorNotification.set(new MeteorNotification(
                MeteorNotification.State.FALLING, loc, trig, x, y, z, summoner));
        MeteorHudMod.LOGGER.info("[MeteorHUD] Falling: {} {} at {},{},{}", loc, trig, x, y, z);
    }

    private static void handleCrash(int cx, int cy, int cz) {
        // Find closest active waypoint to the crash coords and remove it
        MeteorWaypoint closest = null;
        double best = Double.MAX_VALUE;
        for (MeteorWaypoint wp : WaypointManager.getInstance().getAll()) {
            double d = wp.distanceSq(cx, cy, cz);
            if (d < best) { best = d; closest = wp; }
        }
        String elapsed = closest != null ? closest.elapsedFormatted() : null;
        if (closest != null) WaypointManager.getInstance().remove(closest.id);
        MeteorNotification.markCrashed(elapsed);
        MeteorHudMod.LOGGER.info("[MeteorHUD] Crashed at {},{},{}", cx, cy, cz);
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }
}

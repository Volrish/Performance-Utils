package com.volrish.meteorhud.client;

/**
 * Current HUD notification state — single instance, replaced on each new event.
 *
 * Lifecycle:
 *   FALLING  → set when meteor is detected. Beam + Xaero dot active.
 *   CRASHED  → updated in-place when crash detected at the same coordinates.
 *              Beam + Xaero dot removed. Notification stays on screen.
 *   (next meteor detected) → notification replaced entirely.
 *
 * No timer. No fade. Stays until something new happens.
 */
public class MeteorNotification {

    public enum State { STANDBY, FALLING, CRASHED }

    public       State         state;
    public final MeteorLocation location;
    public final MeteorTrigger  trigger;
    public final int            x, y, z;
    public final String         summonerName;
    public       String         frozenElapsed = null;  // set on crash, holds final elapsed time

    public MeteorNotification(State state,
                              MeteorLocation location,
                              MeteorTrigger trigger,
                              int x, int y, int z,
                              String summonerName) {
        this.state        = state;
        this.location     = location;
        this.trigger      = trigger;
        this.x            = x;
        this.y            = y;
        this.z            = z;
        this.summonerName = summonerName != null ? summonerName : "";
    }

    public String titleLine() {
        if (state == State.CRASHED) {
            return "A meteor has crashed at:";
        }
        if (trigger == MeteorTrigger.PLAYER_SUMMON && !summonerName.isEmpty()) {
            String tag = RemoteDataSync.isFriend(summonerName) ? "[Friend]" : "[Unknown]";
            return "Summoned by " + summonerName + " " + tag + " at:";
        }
        return "A meteor is falling from the sky at:";
    }

    /** True if the summoner is a known friend. Used by HudRenderer for text color. */
    public boolean summonerIsFriend() {
        return trigger == MeteorTrigger.PLAYER_SUMMON
                && RemoteDataSync.isFriend(summonerName);
    }

    public String coordLine() {
        return x + ", " + y + ", " + z;
    }

    // ---- Singleton current notification ----

    private static MeteorNotification CURRENT = null;

    /** Replace with a brand new falling notification (new meteor detected). */
    public static void set(MeteorNotification n) {
        CURRENT = n;
    }

    /** Update existing notification to CRASHED state — freezes the elapsed timer. */
    /** Clear all meteor state — call on disconnect to prevent stale HUD. */
    public static void clear() {
        CURRENT.set(null);
    }

    public static void markCrashed(String elapsed) {
        if (CURRENT != null) {
            CURRENT.state         = State.CRASHED;
            CURRENT.frozenElapsed = elapsed;
        }
    }

    public static MeteorNotification getCurrent() {
        return CURRENT;
    }
}

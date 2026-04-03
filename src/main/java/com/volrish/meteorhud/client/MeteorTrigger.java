package com.volrish.meteorhud.client;

/**
 * WHO triggered the meteor — drives HUD notification text only.
 *
 * NATURAL       — no player involved
 * PLAYER_SUMMON — a player summoned it (their name is shown in the HUD)
 *
 * Does not affect beam color or Xaero waypoint color.
 * That is handled by MeteorLocation.
 */
public enum MeteorTrigger {
    NATURAL,
    PLAYER_SUMMON;

    public static MeteorTrigger fromOrdinal(int o) {
        MeteorTrigger[] v = values();
        return (o >= 0 && o < v.length) ? v[o] : NATURAL;
    }
}

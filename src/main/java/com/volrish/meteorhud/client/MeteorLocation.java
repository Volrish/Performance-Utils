package com.volrish.meteorhud.client;

/**
 * WHERE the meteor is — drives beam color, Xaero color, and status label.
 *
 * UNKNOWN  = not yet identified (no zone match, chat didn't say safe)
 * WILD     = surface / open world
 * GUARDED  = mine / guarded zone
 */
public enum MeteorLocation {

    UNKNOWN (0xFFAAAAAA, "Unknown", 0),   // grey  — pending identification
    WILD    (0xFFFF8800, "Wild",    1),   // orange
    GUARDED (0xFF44FF55, "Guarded", 5);   // lime/green

    public final int    color;
    public final String label;
    public final int    xaeroIndex;

    MeteorLocation(int color, String label, int xaeroIndex) {
        this.color      = color;
        this.label      = label;
        this.xaeroIndex = xaeroIndex;
    }

    public float r() { return ((color >> 16) & 0xFF) / 255f; }
    public float g() { return ((color >> 8)  & 0xFF) / 255f; }
    public float b() { return ( color        & 0xFF) / 255f; }

    public static MeteorLocation fromOrdinal(int o) {
        MeteorLocation[] v = values();
        return (o >= 0 && o < v.length) ? v[o] : UNKNOWN;
    }
}

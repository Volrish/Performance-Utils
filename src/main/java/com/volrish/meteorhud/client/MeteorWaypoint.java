package com.volrish.meteorhud.client;

public class MeteorWaypoint {

    public final String         id;
    public       MeteorLocation location;
    public final MeteorTrigger  trigger;
    public final int            x, y, z;
    public final String         summonerName;
    public       boolean        visible = true;
    public final long           detectedAtMs;   // System.currentTimeMillis() when first seen

    public MeteorWaypoint(String id, MeteorLocation location, MeteorTrigger trigger,
                          int x, int y, int z, String summonerName) {
        this.id           = id;
        this.location     = location;
        this.trigger      = trigger;
        this.x            = x;
        this.y            = y;
        this.z            = z;
        this.summonerName = summonerName != null ? summonerName : "";
        this.detectedAtMs = System.currentTimeMillis();
    }

    /** Elapsed seconds since this waypoint was detected. */
    public long elapsedSeconds() {
        return (System.currentTimeMillis() - detectedAtMs) / 1000L;
    }

    /** Formatted elapsed time: "0:47" or "5:12". */
    public String elapsedFormatted() {
        long s = elapsedSeconds();
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    public String status() {
        if (trigger == MeteorTrigger.PLAYER_SUMMON) {
            return location == MeteorLocation.GUARDED ? "Safe" : "Danger";
        }
        return switch (location) {
            case GUARDED -> "Safe";
            default      -> "Wild";
        };
    }

    public int color() {
        boolean safe = location == MeteorLocation.GUARDED;
        if (trigger == MeteorTrigger.PLAYER_SUMMON) {
            return safe ? 0xFF8800 : 0xFF2222;
        }
        return safe ? 0x44FF55 : 0xFF8800;
    }

    public int xaeroColorIndex() {
        if (trigger == MeteorTrigger.PLAYER_SUMMON) return 14;
        return location.xaeroIndex;
    }

    public double distanceSq(double px, double py, double pz) {
        double dx = x - px, dy = y - py, dz = z - pz;
        return dx*dx + dy*dy + dz*dz;
    }

    @Override public String toString() {
        return id + " [" + status() + "] @ " + x + "," + y + "," + z;
    }
}

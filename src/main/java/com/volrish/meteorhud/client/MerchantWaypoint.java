package com.volrish.meteorhud.client;

/**
 * ============================================================
 *  MERCHANT WAYPOINT — one active ore merchant on the server
 * ============================================================
 *
 *  Created by ChatMeteorListener when "(!) A X Ore Merchant traveled to..."
 *  is detected in chat.
 *
 *  TO ADD A NEW MERCHANT TYPE:
 *    1. Add an entry to the MerchantType enum below
 *    2. Add a keyword match in fromChat() (uses the ore name from chat)
 *    3. Add the icon item in MerchantHudRenderer.typeItem()
 */
public class MerchantWaypoint {

    /**
     * All supported ore merchant types.
     * label  — shown in the HUD
     * color  — beacon beam color (hex RGB)
     */
    public enum MerchantType {
        COAL    ("Coal",     0x555555),
        IRON    ("Iron",     0xD8D8D8),
        LAPIS   ("Lapis",    0x1B4ECC),
        REDSTONE("Redstone", 0xFF2222),
        GOLD    ("Gold",     0xFFD700),
        DIAMOND ("Diamond",  0x55FFFF),
        EMERALD ("Emerald",  0x22CC55);

        public final String label;
        public final int    color;

        MerchantType(String label, int color) {
            this.label = label;
            this.color = color;
        }

        /**
         * Match a raw chat string to a merchant type.
         * e.g. "Gold Ore" → GOLD
         *
         * TO SUPPORT A NEW TYPE: add an if-block here matching the ore name
         * as it appears in the chat message before "Ore Merchant".
         */
        public static MerchantType fromChat(String raw) {
            String lower = raw.toLowerCase();
            if (lower.contains("coal"))      return COAL;
            if (lower.contains("iron"))      return IRON;
            if (lower.contains("lapis"))     return LAPIS;
            if (lower.contains("redstone"))  return REDSTONE;
            if (lower.contains("gold"))      return GOLD;
            if (lower.contains("diamond"))   return DIAMOND;
            if (lower.contains("emerald"))   return EMERALD;
            return null; // unknown type — ignored
        }
    }

    // ---- Fields ----

    public final String       id;           // internal ID like "merchant_0"
    public final MerchantType type;
    public final int          x, y, z;      // world coordinates
    public       boolean      alive       = true;
    public       long         killedAtMs  = -1;         // -1 while alive
    public final long         spawnedAtMs = System.currentTimeMillis(); // when first seen in chat
    public       long         beamUntilMs = -1;
    public       boolean      restoredFromArchive = false;

    public MerchantWaypoint(String id, MerchantType type, int x, int y, int z) {
        this.id   = id;
        this.type = type;
        this.x    = x;
        this.y    = y;
        this.z    = z;
    }

    public int color() { return type.color; }

    public double distanceSq(double px, double py, double pz) {
        double dx = x - px, dy = y - py, dz = z - pz;
        return dx*dx + dy*dy + dz*dz;
    }
}

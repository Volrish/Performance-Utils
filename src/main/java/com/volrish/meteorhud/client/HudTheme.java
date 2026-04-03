package com.volrish.meteorhud.client;

/**
 * ============================================================
 *  HUD THEME — all visual constants in one place
 * ============================================================
 *  Change these to restyle every HUD at once.
 *
 *  FORMAT: 0xRRGGBB (no alpha — alpha is applied separately)
 *
 *  TO CUSTOMIZE:
 *    Edit the constants below and recompile.
 *    Each section controls a specific HUD panel.
 */
public final class HudTheme {

    private HudTheme() {}

    // ================================================================
    //  GLOBAL — shared by all HUD panels
    // ================================================================

    /** Main panel background color */
    public static final int BG           = 0x0B111A;
    /** Background opacity (0.0 = fully transparent, 1.0 = fully opaque) */
    public static final float BG_ALPHA   = 0.85f;
    /** Border line color */
    public static final int BORDER       = 0x2D3A4F;
    /** Top accent line color (changes per HUD) */
    public static final int ACCENT_DEFAULT = 0x4488AA;

    // ================================================================
    //  TEXT COLORS
    // ================================================================

    public static final int TEXT_WHITE   = 0xEEEEEE;
    public static final int TEXT_GREY    = 0x666666;
    public static final int TEXT_DIM     = 0x444444;
    public static final int TEXT_COORD   = 0x88EEFF;  // cyan — coordinates
    public static final int TEXT_DIST    = 0x888888;  // grey — distance labels

    // ================================================================
    //  METEOR HUD
    // ================================================================

    public static final int METEOR_HEADER   = 0xFFDD88;  // gold
    public static final int METEOR_SAFE     = 0x44FF77;  // green
    public static final int METEOR_WILD     = 0xFF8800;  // orange
    public static final int METEOR_DANGER   = 0xFF4444;  // red
    public static final int METEOR_SECT_BG  = 0x111A2A;  // darker than BG
    public static final int METEOR_DIVIDER  = 0x223344;

    // ================================================================
    //  MERCHANT HUD
    // ================================================================

    public static final int MERCHANT_HEADER  = 0xFFDD88;
    public static final int MERCHANT_LIVE    = 0xFFFFFF;
    public static final int MERCHANT_KILLED  = 0xFF4444;
    public static final int MERCHANT_MINI_BG = 0x0D1A0D;
    public static final int MERCHANT_ACCENT  = 0xFFDD88;

    // ================================================================
    //  UTILS HUD (satchels)
    // ================================================================

    public static final int SATCHEL_FULL   = 0xFF4444;  // red — 90%+
    public static final int SATCHEL_HIGH   = 0xFFDD44;  // yellow — 60–90%
    public static final int SATCHEL_OK     = 0x44FF77;  // green — 0–60%
    public static final int SATCHEL_HEADER = 0x88EEFF;
    public static final int ABSORBER_COLOR = 0x88EEFF;

    // ================================================================
    //  NEARBY PLAYERS HUD
    // ================================================================

    public static final int NEARBY_HEADER  = 0xFF4444;  // red — danger
    public static final int NEARBY_CLOSE   = 0xFF8888;  // <100 blocks
    public static final int NEARBY_FAR     = 0xEEEEEE;  // normal
    public static final int NEARBY_ACCENT  = 0xFF4444;

    // ================================================================
    //  COMPASS HUD
    // ================================================================

    public static final int COMPASS_BG       = 0x0B111A;
    public static final int COMPASS_BORDER   = 0x2D3A4F;
    public static final int COMPASS_CENTER   = 0x4488AA;  // center tick
    public static final int COMPASS_CARDINAL = 0xFFFFFF;  // N/S/E/W
    public static final int COMPASS_INTER    = 0x888888;  // NE/SW etc
    public static final int COMPASS_METEOR   = 0xFF5500;  // orange-red arrow
    public static final int COMPASS_MERCHANT = 0xFFDD44;  // gold arrow
    public static final float COMPASS_BG_ALPHA = 0.70f;

    // ================================================================
    //  ITEM VALUE OVERLAY — tier colors
    // ================================================================

    /** Simple tier — grey */
    public static final int TIER_SIMPLE     = 0xAAAAAA;
    /** Uncommon tier — green */
    public static final int TIER_UNCOMMON   = 0x55FF55;
    /** Elite tier — blue */
    public static final int TIER_ELITE      = 0x5555FF;
    /** Ultimate tier — purple */
    public static final int TIER_ULTIMATE   = 0xAA00AA;
    /** Legendary tier — gold */
    public static final int TIER_LEGENDARY  = 0xFFAA00;
    /** Mystic tier — cyan */
    public static final int TIER_MYSTIC     = 0x55FFFF;
    /** Godly tier — red */
    public static final int TIER_GODLY      = 0xFF5555;
    /** Mystery item — yellow */
    public static final int TIER_MYSTERY    = 0xFFFF55;

    /** XP Booster multiplier — light green */
    public static final int BOOSTER_XP      = 0xAAFF77;
    /** Energy Booster multiplier — sky blue */
    public static final int BOOSTER_ENERGY  = 0x55EEFF;
    /** Money Note value — green */
    public static final int MONEY_NOTE      = 0x55FF55;
    /** Gang Points — yellow */
    public static final int GANG_POINTS     = 0xFFDD00;
    /** Charge Orb / Dust percent — white */
    public static final int ORB_PCT         = 0xFFFFFF;

    // ================================================================
    //  HUD LAYOUT EDITOR
    // ================================================================

    public static final int EDITOR_BG       = 0xBF101723;
    public static final int EDITOR_HEADER   = 0x6B294264;
    public static final int EDITOR_OUTLINE  = 0xFFFFFFFF;
    public static final int EDITOR_OUTLINE_DIM = 0x55FFFFFF;
    public static final int EDITOR_HANDLE   = 0xFF4488AA;
    public static final int EDITOR_HANDLE_HOVER = 0xFFFFDD44;

    // ================================================================
    //  SETTINGS SCREEN
    // ================================================================

    public static final int SETTINGS_BG         = 0xCC080E16;
    public static final int SETTINGS_CARD_HOVER  = 0xBB1A2A3A;
    public static final int SETTINGS_CARD_NORMAL = 0xBB0D1A26;
    public static final int SETTINGS_CARD_ON     = 0xFF44AA66;
    public static final int SETTINGS_CARD_OFF    = 0xFF334455;
    public static final int SETTINGS_ICON_BG     = 0xFF1A2A3A;
    public static final int SETTINGS_POPUP_BG    = 0xFF0D1A26;
    public static final int SETTINGS_POPUP_BORDER = 0xFF2A6A8A;
}

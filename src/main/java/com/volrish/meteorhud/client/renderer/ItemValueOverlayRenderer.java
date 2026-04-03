package com.volrish.meteorhud.client.renderer;

import com.volrish.meteorhud.client.HudTheme;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ============================================================
 *  ITEM VALUE OVERLAY
 * ============================================================
 *  Draws overlays directly on inventory/chest slots.
 *  Registered via ScreenEvents in MeteorHudClient.
 *
 *  WHAT DRAWS WHERE:
 *
 *  BOTTOM-RIGHT (always):
 *    Charge Orb     → "13%"   white
 *    Dust           → "3%"    tier color
 *    XP Booster     → "1.5x"  light green
 *    Energy Booster → "1.5x"  sky blue
 *    Money Note     → "$2.3M" green
 *    Gang Points    → "1500"  yellow
 *    Armor/Weapon   → "L40"   tier color (NO pickaxe)
 *    All tier items → colored square (2×2 px box)
 *
 *  TIER ITEMS (show colored box, no text):
 *    Clue Scrolls, Randomization Scrolls, Enchant Books,
 *    XP Bottles, Shards, Contrabands, White Scroll,
 *    Cosmo-Slot Ticket, any other tiered item
 *
 *  TIER COLORS: see HudTheme.TIER_*
 *
 *  TO ADD A NEW ITEM: add a block in getOverlayInfo() below.
 *  TO CHANGE COLORS:  edit HudTheme.java.
 */
public class ItemValueOverlayRenderer {

    // ---- Patterns ----
    private static final Pattern PCT_IN_PARENS = Pattern.compile("\\((\\d+(?:\\.\\d+)?)%\\)");
    private static final Pattern PCT_PREFIX    = Pattern.compile("^(\\d+(?:\\.\\d+)?)%");
    private static final Pattern MONEY_VAL     = Pattern.compile("\\$([\\d,]+(?:\\.\\d+)?)");
    private static final Pattern MULTIPLIER    = Pattern.compile("(\\d+(?:\\.\\d+)?)x", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEVEL_NUM     = Pattern.compile("\\blevel\\s*(\\d+)\\b|\\bl(\\d+)\\b|\\[(\\d+)\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern POINTS_VAL    = Pattern.compile("([\\d,]+)\\s*(?:gang\\s*)?points?", Pattern.CASE_INSENSITIVE);

    /** Result of overlay calculation for one slot. */
    public record OverlayResult(
        String  text,       // text to draw (null = draw box only)
        int     color,      // text/box color
        boolean drawBox     // true = draw small tier box
    ) {}

    /**
     * Returns overlay info for this item, or null if nothing to show.
     * Called once per slot per frame.
     */
    public static OverlayResult getOverlay(ItemStack stack) {
        if (stack.isEmpty()) return null;
        String name   = clean(stack.getName().getString()).trim();
        String nameLo = name.toLowerCase();
        String lore   = loreText(stack);

        // ---- Charge Orb ----
        if (nameLo.contains("charge orb")) {
            Matcher m = PCT_PREFIX.matcher(name);
            if (m.find()) return new OverlayResult(m.group(1) + "%", HudTheme.ORB_PCT, false);
        }

        // ---- Dust ----
        if (nameLo.contains("dust") && !nameLo.contains("sand")) {
            Matcher m = PCT_IN_PARENS.matcher(name);
            if (m.find()) return new OverlayResult(m.group(1) + "%", tierColor(name), false);
        }

        // ---- XP Booster → multiplier in light green ----
        if (nameLo.contains("xp booster") || nameLo.contains("xp boost")) {
            String mult = extractMultiplier(lore != null ? lore : name);
            if (mult != null) return new OverlayResult(mult + "x", HudTheme.BOOSTER_XP, false);
        }

        // ---- Energy Booster → multiplier in sky blue ----
        if (nameLo.contains("energy booster") || nameLo.contains("energy boost")) {
            String mult = extractMultiplier(lore != null ? lore : name);
            if (mult != null) return new OverlayResult(mult + "x", HudTheme.BOOSTER_ENERGY, false);
        }

        // ---- Money Note → green dollar value ----
        if (nameLo.contains("money note") || (nameLo.contains("note") && !nameLo.contains("enchant"))) {
            String val = extractMoney(lore != null ? lore : name);
            if (val != null) return new OverlayResult(val, HudTheme.MONEY_NOTE, false);
        }

        // ---- Gang Points → yellow count ----
        if (nameLo.contains("gang point")) {
            String val = extractPoints(lore != null ? lore + " " + name : name);
            if (val != null) return new OverlayResult(val, HudTheme.GANG_POINTS, false);
        }

        // ---- Armor / Weapon (NO pickaxe) — level number ----
        boolean isArmor = nameLo.contains("helmet") || nameLo.contains("chestplate")
                       || nameLo.contains("leggings") || nameLo.contains("boots")
                       || nameLo.contains("armor");
        boolean isWeapon = (nameLo.contains("sword") || nameLo.contains("axe"))
                        && !nameLo.contains("pickaxe");
        if (isArmor || isWeapon) {
            String lvl = extractLevel(name, lore);
            if (lvl != null) return new OverlayResult("L" + lvl, tierColor(name), false);
        }

        // ---- Tier items — small colored box ----
        if (isTierItem(nameLo)) {
            int col = tierColor(name);
            if (col != 0xFFFFFF) return new OverlayResult(null, col, true);
        }

        return null;
    }

    // ---- Legacy compatibility accessors (kept so MeteorHudClient compiles) ----
    public static String  getOverlayText(ItemStack s)  { var r = getOverlay(s); return r != null ? r.text() : null; }
    public static int     getOverlayColor(ItemStack s) { var r = getOverlay(s); return r != null ? r.color() : 0xFFFFFF; }
    public static boolean isBoxOnly(ItemStack s)        { var r = getOverlay(s); return r != null && r.drawBox(); }
    public static String  getTopLeftText(ItemStack s)   { return null; }  // removed
    public static int     getTopLeftColor()             { return 0xFFFFFF; }

    // ================================================================
    //  Helpers
    // ================================================================

    private static int tierColor(String name) {
        String lo = name.toLowerCase();
        if (lo.contains("godly"))     return HudTheme.TIER_GODLY;
        if (lo.contains("mystic"))    return HudTheme.TIER_MYSTIC;
        if (lo.contains("legendary")) return HudTheme.TIER_LEGENDARY;
        if (lo.contains("ultimate"))  return HudTheme.TIER_ULTIMATE;
        if (lo.contains("elite"))     return HudTheme.TIER_ELITE;
        if (lo.contains("uncommon"))  return HudTheme.TIER_UNCOMMON;
        if (lo.contains("simple"))    return HudTheme.TIER_SIMPLE;
        if (lo.contains("mystery"))   return HudTheme.TIER_MYSTERY;
        return 0xFFFFFF;
    }

    private static boolean isTierItem(String nameLo) {
        return nameLo.contains("scroll") || nameLo.contains("clue")
            || nameLo.contains("enchant book") || nameLo.contains("xp bottle")
            || nameLo.contains("exp bottle")   || nameLo.contains("shard")
            || nameLo.contains("contraband")   || nameLo.contains("white scroll")
            || nameLo.contains("cosmo")        || nameLo.contains("bot ticket")
            || nameLo.contains("mystery");
    }

    private static String extractMultiplier(String text) {
        Matcher m = MULTIPLIER.matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }

    private static String extractMoney(String text) {
        Matcher m = MONEY_VAL.matcher(text);
        if (m.find()) {
            try {
                double v = Double.parseDouble(m.group(1).replace(",", ""));
                if (v >= 1_000_000_000) return String.format("$%.1fB", v/1e9);
                if (v >= 1_000_000)     return String.format("$%.1fM", v/1e6);
                if (v >= 1_000)         return String.format("$%.0fK", v/1e3);
                return "$" + (long)v;
            } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private static String extractPoints(String text) {
        Matcher m = POINTS_VAL.matcher(text);
        if (m.find()) {
            try {
                long v = Long.parseLong(m.group(1).replace(",", ""));
                if (v >= 1_000_000) return (v/1_000_000) + "M";
                if (v >= 1_000)     return (v/1_000) + "K";
                return String.valueOf(v);
            } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private static String extractLevel(String name, String lore) {
        // Check name first, then lore
        for (String src : new String[]{ name, lore != null ? lore : "" }) {
            if (src.isEmpty()) continue;
            Matcher m = LEVEL_NUM.matcher(src);
            while (m.find()) {
                for (int g = 1; g <= 3; g++) {
                    String v = m.group(g);
                    if (v != null) return v;
                }
            }
        }
        return null;
    }

    private static String loreText(ItemStack stack) {
        try {
            var lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) return null;
            var sb = new StringBuilder();
            for (var line : lore.lines())
                sb.append(clean(line.getString())).append(" ");
            return sb.toString().trim();
        } catch (Exception e) { return null; }
    }

    private static String clean(String s) {
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                .replaceAll("[^\\x20-\\x7E]", "").trim();
    }
}

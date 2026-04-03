package com.volrish.meteorhud.client.renderer;

import com.volrish.meteorhud.client.HudPosition;
import com.volrish.meteorhud.client.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.*;
import java.util.regex.*;

/**
 * Utils HUD — compact icon grid.
 * Satchels: icon + % overlay (green 0% → red 100%)
 * Absorbers: icon + count overlay
 * Icons flow in rows. New row when row exceeds panel width.
 * Absorber always last.
 */
public class AbsorberHudRenderer {

    private static final int HPAD    = 5;
    private static final int VPAD    = 4;
    private static final int SECT_H  = 18;
    private static final int ICON_SZ = 16;
    private static final int ICON_GAP = 3; // gap between icons

    private static final int COL_BG     = 0x0B111A;
    private static final int COL_BORDER = 0x2D3A4F;
    private static final int COL_HEADER = 0x88EEFF;

    private static final Pattern CAP = Pattern.compile(
            "\\((\\d[\\d,]*)\\s*/\\s*(\\d[\\d,]*)\\)");

    static class SatchelEntry {
        long cur, max;
        net.minecraft.item.Item icon;
        SatchelEntry(long cur, long max, net.minecraft.item.Item icon) {
            this.cur = cur; this.max = max; this.icon = icon;
        }
        int pct() { return max == 0 ? 0 : (int)(cur * 100 / max); }
        /** Color: green at 0%, yellow at 60%, red at 90%+ */
        int color() {
            int p = pct();
            if (p >= 90) return 0xFF4444;
            if (p >= 60) return 0xFFDD44;
            return 0x44FF77;
        }
    }

    private static int lastPanelH = 50;

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (client.options.hudHidden) return;
        if (!ModConfig.getInstance().absorberHudEnabled) return;

        List<SatchelEntry> satchels  = collectSatchels(client);
        int                absorbers = countAbsorbers(client);
        if (satchels.isEmpty() && absorbers == 0) return;

        renderInner(ctx, client, satchels, absorbers);
    }

    public static void renderPreview(DrawContext ctx, MinecraftClient client) {
        List<SatchelEntry> demo = new ArrayList<>();
        demo.add(new SatchelEntry(10,  50688, Items.DIAMOND_ORE));
        demo.add(new SatchelEntry(0,   25344, Items.DIAMOND));
        demo.add(new SatchelEntry(500, 50688, Items.COAL_ORE));
        renderInner(ctx, client, demo, 16);
    }

    public static int currentPanelWidth(MinecraftClient c) { return HudPosition.get(HudPosition.ABSORBER).effectiveWidth(); }
    public static int currentPanelHeight(MinecraftClient c) { return lastPanelH; }

    private static void renderInner(DrawContext ctx, MinecraftClient client,
                                    List<SatchelEntry> satchels, int absorbers) {
        TextRenderer tr     = client.textRenderer;
        HudPosition  pos    = HudPosition.get(HudPosition.ABSORBER);
        int panelW = pos.effectiveWidth();
        int          a      = alpha(pos.opacity);

        int stride  = ICON_SZ + ICON_GAP;
        int innerW  = panelW - 2 * HPAD;
        int perRow  = Math.max(1, innerW / stride);

        // Build icon list: satchels then absorber
        List<SatchelEntry> icons = new ArrayList<>(satchels);
        SatchelEntry absorberEntry = absorbers > 0
                ? new SatchelEntry(absorbers, absorbers, Items.SPONGE) : null;
        // absorberEntry is handled separately for count display

        int total = icons.size() + (absorbers > 0 ? 1 : 0);
        int rows  = Math.max(1, (int)Math.ceil((double)total / perRow));

        int panelH = VPAD + SECT_H + VPAD + rows * stride + VPAD;
        lastPanelH = panelH;

        int px = pos.x, py = pos.y;
        ctx.fill(px-2, py-2, px+panelW+2, py+panelH+2,
                withAlpha(0x000000, (int)(a*0.28f)));
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(px, py);

        if (!ModConfig.getInstance().transparentHud) ctx.fill(0, 0, panelW, panelH, withAlpha(COL_BG, (int)(a*0.82f)));

        // Header
        ctx.fill(1, 0, panelW-1, VPAD+SECT_H,   withAlpha(0x111A2A, (int)(a*0.9f)));
        ctx.drawItem(Items.BUNDLE.getDefaultStack(), HPAD, VPAD);
        ctx.drawTextWithShadow(tr, "Utils", HPAD+20, VPAD+(SECT_H-tr.fontHeight)/2,
                withAlpha(COL_HEADER, a));

        int ty = VPAD + SECT_H + VPAD;
        int col = 0;

        // Draw satchels
        for (SatchelEntry e : icons) {
            int ix = HPAD + col * stride;
            int iy = ty;
            ctx.drawItem(new ItemStack(e.icon), ix, iy);
            // % overlay bottom-right in item's color
            String pctStr = e.pct() + "%";
            ctx.drawTextWithShadow(tr, pctStr,
                    ix + ICON_SZ - tr.getWidth(pctStr),
                    iy + ICON_SZ - tr.fontHeight,
                    withAlpha(e.color(), a));
            col++;
            if (col >= perRow) { col = 0; ty += stride; }
        }

        // Draw absorber last
        if (absorbers > 0) {
            int ix = HPAD + col * stride;
            int iy = ty;
            ctx.drawItem(Items.SPONGE.getDefaultStack(), ix, iy);
            String cnt = String.valueOf(absorbers);
            ctx.drawTextWithShadow(tr, cnt,
                    ix + ICON_SZ - tr.getWidth(cnt),
                    iy + ICON_SZ - tr.fontHeight,
                    withAlpha(0x88EEFF, a));
        }

        ctx.getMatrices().popMatrix();
    }

    private static List<SatchelEntry> collectSatchels(MinecraftClient client) {
        Map<String, long[]>              totals = new LinkedHashMap<>();
        Map<String, net.minecraft.item.Item> icons = new LinkedHashMap<>();

        for (int i = 0; i < client.player.getInventory().size(); i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            String name = stack.getName().getString();
            if (!name.contains("Satchel")) continue;

            Matcher m = CAP.matcher(name);
            if (!m.find()) continue;

            long cur = parseLong(m.group(1));
            long max = parseLong(m.group(2));

            String typePart = name.substring(0, name.indexOf("Satchel")).trim()
                    .replaceAll("\\d+$", "").trim();
            boolean isOre  = typePart.endsWith("Ore");
            String oreType = isOre
                    ? typePart.substring(0, typePart.lastIndexOf("Ore")).trim()
                    : typePart;
            String key = oreType + (isOre ? "_ore" : "_ref");

            totals.merge(key, new long[]{cur, max}, (a2, b) -> new long[]{a2[0]+b[0], a2[1]+b[1]});
            icons.putIfAbsent(key, iconFor(oreType, isOre));
        }

        List<SatchelEntry> result = new ArrayList<>();
        for (Map.Entry<String, long[]> e : totals.entrySet()) {
            result.add(new SatchelEntry(e.getValue()[0], e.getValue()[1], icons.get(e.getKey())));
        }
        return result;
    }

    private static int countAbsorbers(MinecraftClient client) {
        int count = 0;
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty() || stack.getItem() != Items.SPONGE) continue;
            if (stack.getName().getString().toLowerCase().contains("absorber"))
                count += stack.getCount();
        }
        return count;
    }

    private static net.minecraft.item.Item iconFor(String t, boolean ore) {
        t = t.toLowerCase();
        if (t.contains("coal"))     return ore ? Items.COAL_ORE     : Items.COAL;
        if (t.contains("iron"))     return ore ? Items.IRON_ORE     : Items.IRON_INGOT;
        if (t.contains("lapis"))    return ore ? Items.LAPIS_ORE    : Items.LAPIS_LAZULI;
        if (t.contains("redstone")) return ore ? Items.REDSTONE_ORE : Items.REDSTONE;
        if (t.contains("gold"))     return ore ? Items.GOLD_ORE     : Items.GOLD_INGOT;
        if (t.contains("diamond"))  return ore ? Items.DIAMOND_ORE  : Items.DIAMOND;
        if (t.contains("emerald"))  return ore ? Items.EMERALD_ORE  : Items.EMERALD;
        return ore ? Items.STONE : Items.GOLD_NUGGET;
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s.replace(",","").trim()); }
        catch (NumberFormatException e) { return 0; }
    }
    private static int alpha(float o) { return Math.max(0,Math.min(255,(int)(o*255))); }
    private static int withAlpha(int rgb, int a) {
        return ((Math.max(0,Math.min(255,a))&0xFF)<<24)|(rgb&0x00FFFFFF);
    }
}

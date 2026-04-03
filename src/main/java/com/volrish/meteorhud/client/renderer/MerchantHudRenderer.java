package com.volrish.meteorhud.client.renderer;

import com.volrish.meteorhud.client.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.*;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Merchant HUD:
 * - Fresh merchants (<5min): full row — icon, type, [Live/Killed], coords, timer
 * - Old merchants (>5min, alive): minimized — icon row divider at bottom with count
 * - When player is inside a mine zone: all merchants for that zone expand to full view
 * - Killed merchants: show with [Killed] + countdown (30s linger), then disappear
 */
public class MerchantHudRenderer {

    private static final long   MINIMIZE_MS     = 5 * 60 * 1000L; // 5 min
    private static final int    HPAD            = 5;
    private static final int    VPAD            = 4;
    private static final int    SECT_H          = 18;
    private static final int    ICON_SZ         = 16;

    private static final int COL_BG      = 0x0B111A;
    private static final int COL_BORDER  = 0x2D3A4F;
    private static final int COL_HEADER  = 0xFFDD88;
    private static final int COL_COORD   = 0x88EEFF;
    private static final int COL_KILLED  = 0xFF4444;
    private static final int COL_LIVE    = 0xFFFFFF;
    private static final int COL_MINI_BG = 0x0D1A0D;

    private static int lastPanelH = 60;

    private static Item typeItem(MerchantWaypoint.MerchantType type) {
        return switch (type) {
            case COAL     -> Items.COAL;
            case IRON     -> Items.IRON_INGOT;
            case LAPIS    -> Items.LAPIS_LAZULI;
            case REDSTONE -> Items.REDSTONE;
            case GOLD     -> Items.GOLD_INGOT;
            case DIAMOND  -> Items.DIAMOND;
            case EMERALD  -> Items.EMERALD;
        };
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        if (client.options.hudHidden) return;
        if (!ModConfig.getInstance().merchantHudEnabled) return;
        MerchantManager.getInstance().pruneExpired();
        if (!MerchantManager.getInstance().hasVisible()) return;
        renderInner(ctx, client);
    }

    public static void renderPreview(DrawContext ctx, MinecraftClient client) {
        renderInner(ctx, client);
    }

    public static int currentPanelWidth(MinecraftClient c) { return HudPosition.get(HudPosition.MERCHANT).effectiveWidth(); }
    public static int currentPanelHeight(MinecraftClient c) { return lastPanelH; }

    private static void renderInner(DrawContext ctx, MinecraftClient client) {
        List<MerchantWaypoint> all = MerchantManager.getInstance().getAll();
        if (all.isEmpty()) {
            all = List.of(new MerchantWaypoint("preview",
                    MerchantWaypoint.MerchantType.GOLD, 760, 125, -483));
        }

        TextRenderer tr     = client.textRenderer;
        int          lineH  = tr.fontHeight + 2;
        HudPosition  pos    = HudPosition.get(HudPosition.MERCHANT);
        int panelW = pos.effectiveWidth();
        int          a      = alpha(pos.opacity);
        long         now    = System.currentTimeMillis();

        // Determine if player is inside a mine zone
        String nearMine = null;
        if (client.player != null) {
            int px = (int)client.player.getX(), pz = (int)client.player.getZ();
            String zn = RemoteDataSync.zoneName(px, pz);
            if (zn != null) nearMine = zn;
        }

        // Partition merchants: full view vs minimized
        List<MerchantWaypoint> fullList = new ArrayList<>();
        List<MerchantWaypoint> miniList = new ArrayList<>();

        for (MerchantWaypoint wp : all) {
            if (!wp.alive) {
                fullList.add(wp); // killed always shows full (with countdown)
                continue;
            }
            long age = now - wp.spawnedAtMs;
            boolean isOld = age > MINIMIZE_MS;
            // Expand minimized if player is at the merchant's zone
            boolean expand = (nearMine != null && RemoteDataSync.isInAnyZone(wp.x, wp.z)
                    && nearMine.equals(RemoteDataSync.zoneName(wp.x, wp.z)))
                    || wp.restoredFromArchive;  // tracked from archive = always show
            if (isOld && !expand) miniList.add(wp);
            else                   fullList.add(wp);
        }

        // Calculate panel height
        int panelH = VPAD + SECT_H; // header
        panelH += fullList.size() * (SECT_H + lineH + VPAD);
        if (!miniList.isEmpty()) {
            panelH += VPAD + SECT_H; // minimized divider row
        }
        panelH += VPAD;
        lastPanelH = panelH;

        int px = pos.x, py = pos.y;
        ctx.fill(px-3, py-3, px+panelW+3, py+panelH+3,
                withAlpha(0x000000, (int)(a*0.28f)));
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(px, py);

        if (!ModConfig.getInstance().transparentHud) ctx.fill(0, 0, panelW, panelH, withAlpha(COL_BG, (int)(a*0.82f)));

        // Panel header
        ctx.fill(1, 0, panelW-1, VPAD+SECT_H,   withAlpha(0x1A1A0A, (int)(a*0.9f)));
        ctx.drawItem(Items.VILLAGER_SPAWN_EGG.getDefaultStack(), HPAD, VPAD);
        ctx.drawTextWithShadow(tr, "Merchants", HPAD+20, VPAD+(SECT_H-tr.fontHeight)/2,
                withAlpha(COL_HEADER, a));

        int ty = VPAD + SECT_H + VPAD;

        // Full merchant rows
        for (MerchantWaypoint wp : fullList) {
            ty = renderFullRow(ctx, tr, wp, client, panelW, ty, lineH, a, now);
        }

        // Minimized divider
        if (!miniList.isEmpty()) {
            ctx.fill(1, ty, panelW-1, ty+SECT_H, withAlpha(COL_MINI_BG, (int)(a*0.9f)));
            ctx.fill(1, ty, 3, ty+SECT_H,         withAlpha(0x44FF77, (int)(a*0.6f)));
            ctx.drawTextWithShadow(tr, "Active:", HPAD+4, ty+(SECT_H-tr.fontHeight)/2,
                    withAlpha(0x888888, a));

            // Icon row for minimized merchants
            int ix = HPAD + 4 + tr.getWidth("Active:") + 6;
            for (MerchantWaypoint wp : miniList) {
                ctx.drawItem(new ItemStack(typeItem(wp.type)), ix, ty);
                // Small count overlay if more than 1 of same type
                ix += ICON_SZ + 2;
                if (ix + ICON_SZ > panelW - HPAD) break;
            }
            ty += SECT_H;
        }

        ctx.getMatrices().popMatrix();
    }

    private static int renderFullRow(DrawContext ctx, TextRenderer tr,
                                      MerchantWaypoint wp, MinecraftClient client,
                                      int panelW, int ty, int lineH, int a, long now) {
        boolean killed = !wp.alive;
        int accent     = wp.color();
        int stateColor = killed ? COL_KILLED : COL_LIVE;


        ctx.drawItem(new ItemStack(typeItem(wp.type)), HPAD, ty);

        String label = wp.type.label + " Merchant " + (killed ? "[Killed]" : "[Live]");
        ctx.drawTextWithShadow(tr, label, HPAD+20, ty+(SECT_H-tr.fontHeight)/2,
                withAlpha(stateColor, a));

        // Timer right-aligned
        String timer;
        if (killed && wp.killedAtMs > 0) {
            long rem = 30 - (now - wp.killedAtMs) / 1000L;
            timer = rem + "s";
        } else {
            long age = (now - wp.spawnedAtMs) / 1000L;
            timer = (age / 60) + ":" + String.format("%02d", age % 60);
        }
        ctx.drawTextWithShadow(tr, timer,
                panelW - HPAD - tr.getWidth(timer) - 2,
                ty + (SECT_H - tr.fontHeight) / 2,
                withAlpha(0x888888, a));
        ty += SECT_H;

        // Coord row
        String coords = "  " + wp.x + ", " + wp.y + ", " + wp.z;
        if (client.player != null) {
            Vec3d p = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
            double dist = Math.sqrt(wp.distanceSq(p.x, p.y, p.z));
            String d = dist >= 1000
                    ? String.format("%.1fkm", dist / 1000.0)
                    : String.format("%.0fm", dist);
            coords += "  (" + d + ")";
        }
        ctx.drawTextWithShadow(tr, coords, HPAD + 4, ty, withAlpha(COL_COORD, a));
        ty += lineH + VPAD;

        return ty;
    }

    private static int alpha(float o)               { return Math.max(0,Math.min(255,(int)(o*255))); }
    private static int withAlpha(int rgb, int alpha) {
        return ((Math.max(0,Math.min(255,alpha))&0xFF)<<24)|(rgb&0x00FFFFFF);
    }
}

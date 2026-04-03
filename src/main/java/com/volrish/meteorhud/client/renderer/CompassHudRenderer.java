package com.volrish.meteorhud.client.renderer;

import com.volrish.meteorhud.client.*;
import com.volrish.meteorhud.client.screen.MerchantArchiveScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.*;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * ============================================================
 *  COMPASS HUD — top-center bar
 * ============================================================
 *  Draws a horizontal compass bar with responsive direction arrows.
 *
 *  ARROWS:
 *    ▲  pointing upward = target is in that direction relative to you.
 *    Arrow shifts left/right as you rotate — when target is dead ahead
 *    the arrow is at the center of the bar.
 *    When target is behind you it disappears off the edge.
 *
 *  COLORS (from HudTheme):
 *    Meteor arrow    → HudTheme.COMPASS_METEOR   (orange-red)
 *    Merchant arrow  → HudTheme.COMPASS_MERCHANT (gold)
 *
 *  DISTANCE: shown above the arrow (e.g. "1.8km")
 *
 *  Minimized merchants (>5min old) only appear if tracked
 *  via Merchant Archive screen.
 *
 *  TO ADJUST BAR WIDTH: change BAR_W_MAX
 *  TO ADJUST BAR HEIGHT: change BAR_H
 *  TO CHANGE COLORS: edit HudTheme.java
 */
public class CompassHudRenderer {

    public static final String HUD_ID = HudPosition.COMPASS;

    private static final int BAR_H     = 24;
    private static final int BAR_W_MAX = 400;
    private static final int ARROW_H   = 8;   // arrow height in px
    private static final int ARROW_W   = 5;   // arrow base width in px

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (client.options.hudHidden) return;
        if (!ModConfig.getInstance().compassHudEnabled) return;
        renderInner(ctx, client);
    }

    public static void renderPreview(DrawContext ctx, MinecraftClient client) {
        renderInner(ctx, client);
    }

    private static void renderInner(DrawContext ctx, MinecraftClient client) {
        TextRenderer tr      = client.textRenderer;
        boolean      transp  = ModConfig.getInstance().transparentHud;
        int          screenW = client.getWindow().getScaledWidth();

        int barW = Math.min(BAR_W_MAX, screenW - 40);
        int barX = (screenW - barW) / 2;
        int barY = 4;

        // Background
        if (!transp) {
            ctx.fill(barX - 1, barY - 1, barX + barW + 1, barY + BAR_H + 1,
                    withAlpha(HudTheme.COMPASS_BORDER, 255));
            ctx.fill(barX, barY, barX + barW, barY + BAR_H,
                    withAlpha(HudTheme.COMPASS_BG, (int)(HudTheme.COMPASS_BG_ALPHA * 255)));
        }

        // Center tick
        int cx = barX + barW / 2;
        ctx.fill(cx - 1, barY, cx + 1, barY + BAR_H,
                withAlpha(HudTheme.COMPASS_CENTER, 255));

        // Cardinal letters
        drawLetters(ctx, tr, barX, barY, barW, client);

        float  yaw       = client.player.getYaw();
        Vec3d  playerPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());  // fixed for 1.21

        // Meteor arrows
        for (MeteorWaypoint wp : WaypointManager.getInstance().getAll()) {
            drawArrow(ctx, tr, barX, barY, barW, cx, playerPos, yaw,
                    wp.x, wp.y, wp.z, HudTheme.COMPASS_METEOR);
        }

        // Merchant arrows — tracked merchants always; others only if fresh (<5min)
        long now = System.currentTimeMillis();
        for (MerchantWaypoint wp : MerchantManager.getInstance().getAll()) {
            if (!wp.alive) continue;
            boolean tracked  = MerchantArchiveScreen.isTracked(wp.id);
            boolean fresh    = (now - wp.spawnedAtMs) < 5 * 60 * 1000L;
            if (!tracked && !fresh) continue;
            drawArrow(ctx, tr, barX, barY, barW, cx, playerPos, yaw,
                    wp.x, wp.y, wp.z, HudTheme.COMPASS_MERCHANT);
        }
    }

    /**
     * Draws a filled triangle arrow + distance label above it.
     * The arrow points UP — it represents the target's direction.
     */
    private static void drawArrow(DrawContext ctx, TextRenderer tr,
                                   int barX, int barY, int barW, int cx,
                                   Vec3d playerPos, float playerYaw,
                                   int tx, int ty, int tz, int color) {
        double dx = tx - playerPos.x;
        double dz = tz - playerPos.z;
        if (dx == 0 && dz == 0) return;

        // Relative yaw: -180 to +180, where 0 = directly ahead
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double rel       = ((targetYaw - playerYaw) % 360 + 360) % 360;
        if (rel > 180) rel -= 360;
        if (Math.abs(rel) > 90) return;  // behind player

        int offset  = (int)(rel / 90.0 * (barW / 2));
        int arrowCx = cx + offset;

        // Clip
        if (arrowCx < barX + ARROW_W || arrowCx > barX + barW - ARROW_W) return;

        // Draw upward-pointing triangle arrow
        int tipY  = barY + (BAR_H - ARROW_H) / 2;
        int baseY = tipY + ARROW_H;
        drawTriangleUp(ctx, arrowCx, tipY, ARROW_W, ARROW_H, color);

        // Distance label above bar
        double dist = Math.sqrt(dx*dx + dz*dz);
        String distStr = dist >= 1000
                ? String.format("%.1fkm", dist/1000.0)
                : String.format("%.0fm", dist);
        int textX = arrowCx - tr.getWidth(distStr) / 2;
        ctx.drawTextWithShadow(tr, distStr, textX, barY - tr.fontHeight - 1,
                withAlpha(color, 220));
    }

    /**
     * Draws a filled upward-pointing triangle.
     * Center X at cx, tip at (cx, tipY), base at tipY+height.
     */
    private static void drawTriangleUp(DrawContext ctx, int cx, int tipY,
                                        int halfBase, int height, int color) {
        int argb = withAlpha(color, 230);
        for (int row = 0; row < height; row++) {
            int half = (int)Math.round((double)halfBase * row / height);
            ctx.fill(cx - half, tipY + row, cx + half + 1, tipY + row + 1, argb);
        }
    }

    private static void drawLetters(DrawContext ctx, TextRenderer tr,
                                     int barX, int barY, int barW,
                                     MinecraftClient client) {
        float  yaw = client.player.getYaw();
        int    cx  = barX + barW / 2;
        int    ty  = barY + (BAR_H - tr.fontHeight) / 2;

        String[] labels = {"N","NE","E","SE","S","SW","W","NW"};
        float[]  angles = {180,135,90,45,0,-45,-90,-135};

        for (int i = 0; i < labels.length; i++) {
            double rel = ((angles[i] - yaw) % 360 + 360) % 360;
            if (rel > 180) rel -= 360;
            if (Math.abs(rel) > 90) continue;
            int lx  = cx + (int)(rel / 90.0 * (barW/2)) - tr.getWidth(labels[i]) / 2;
            int col = labels[i].length() == 1
                    ? HudTheme.COMPASS_CARDINAL
                    : HudTheme.COMPASS_INTER;
            ctx.drawTextWithShadow(tr, labels[i], lx, ty, withAlpha(col, 200));
        }
    }

    private static int withAlpha(int rgb, int a) {
        return ((Math.max(0,Math.min(255,a))&0xFF)<<24)|(rgb&0x00FFFFFF);
    }
}

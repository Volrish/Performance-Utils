package com.volrish.meteorhud.client.renderer;

import com.volrish.meteorhud.client.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class HudRenderer {

    private static final int HPAD   = 5;
    private static final int VPAD   = 4;
    private static final int INDENT = 14;

    private static final int COL_BG      = 0x0B111A;
    private static final int COL_SECT_BG = 0x111A2A;
    private static final int COL_BORDER  = 0x2D3A4F;
    private static final int COL_DIVIDER = 0x1E2D40;
    private static final int COL_WHITE   = 0xFFFFFF;
    private static final int COL_COORD   = 0x88EEFF;
    private static final int COL_DIST    = 0xFFDD88;
    private static final int COL_SUMMON  = 0xAAAAAA;
    private static final int COL_DANGER  = 0xFF4444;
    private static final int COL_WILD    = 0xFF8800;
    private static final int COL_SAFE    = 0x44FF77;
    private static final int COL_UNKNOWN = 0xAAAAAA;
    private static final int COL_FRIEND  = 0x44FF77;

    private static int lastPanelH = 60;
    private static int lastRenderedW = 220;  // actual rendered width (may differ from pos.panelWidth due to auto-fit)

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        if (client.options.hudHidden) return;
        if (client.getDebugHud().shouldShowDebugHud()) return;
        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.meteorHudEnabled) return;

        // Only render if there's something to show (no standby)
        List<MeteorNotification> entries = buildEntries();
        if (entries.isEmpty()) return;
        if (entries.size() == 1 && entries.get(0).state == MeteorNotification.State.STANDBY) return;

        renderWidget(ctx, client, cfg, entries);
        renderEdgeArrows(ctx, client);
    }

    public static void renderPreview(DrawContext ctx, MinecraftClient client) {
        ModConfig cfg = ModConfig.getInstance();
        List<MeteorNotification> entries = buildEntries();
        if (entries.isEmpty()) {
            entries = List.of(new MeteorNotification(
                    MeteorNotification.State.FALLING, MeteorLocation.UNKNOWN,
                    MeteorTrigger.NATURAL, 0, 64, 0, ""));
        }
        renderWidgetInner(ctx, client, cfg, entries);
    }

    public static int currentPanelWidth()                   { return lastRenderedW; }
    public static int currentPanelHeight(MinecraftClient c) { return lastPanelH; }

    private static void renderWidget(DrawContext ctx, MinecraftClient client,
                                     ModConfig cfg, List<MeteorNotification> entries) {
        renderWidgetInner(ctx, client, cfg, entries);
    }

    private static void renderWidgetInner(DrawContext ctx, MinecraftClient client,
                                          ModConfig cfg, List<MeteorNotification> entries) {
        TextRenderer tr     = client.textRenderer;
        int          lineH  = tr.fontHeight + 2;
        HudPosition  pos    = HudPosition.get(HudPosition.METEOR);
        int panelW = pos.effectiveWidth();
        int          a      = alpha(pos.opacity);

        // Auto-fit width based on widest content line
        int minW = Math.round(contentMinWidth(entries, client, cfg, tr) );
        panelW = Math.max(panelW, minW);

        int panelH = 0;
        for (int i = 0; i < entries.size(); i++) {
            panelH += sectionH(entries.get(i), client, cfg, lineH);
            if (i < entries.size() - 1) panelH += 5;
        }
        panelH += 2;
        lastPanelH    = panelH;
        lastRenderedW = panelW;

        int px = pos.x, py = pos.y;
        int accent = accentColor(entries.get(0));

        ctx.fill(px-3, py-3, px+panelW+3, py+panelH+3, withAlpha(0x000000, (int)(a*0.28f)));
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(px, py);
        // No matrix scale — panelW already incorporates scale for correct item rendering

        boolean transp = ModConfig.getInstance().transparentHud;
        if (!transp) ctx.fill(0, 0, panelW, panelH, withAlpha(COL_BG, (int)(a*0.82f)));
        if (!transp) ctx.fill(0, 0, panelW, 1,      withAlpha(accent, a));

        int ty = 1;
        for (int i = 0; i < entries.size(); i++) {
            ty = renderSection(ctx, tr, entries.get(i), client, cfg, ty, lineH, a, panelW);
            if (i < entries.size() - 1) {
                ctx.fill(HPAD+4, ty, panelW-HPAD-4, ty+1, withAlpha(COL_DIVIDER, (int)(a*0.7f)));
                ty += 5;
            }
        }
        ctx.getMatrices().popMatrix();
    }

    private static List<MeteorNotification> buildEntries() {
        List<MeteorNotification> result = new ArrayList<>();
        List<MeteorWaypoint> active = WaypointManager.getInstance().getAll()
                .stream().filter(wp -> wp.visible).toList();
        if (!active.isEmpty()) {
            for (MeteorWaypoint wp : active) {
                result.add(new MeteorNotification(MeteorNotification.State.FALLING,
                        wp.location, wp.trigger, wp.x, wp.y, wp.z, wp.summonerName));
            }
        } else {
            MeteorNotification cur = MeteorNotification.getCurrent();
            if (cur != null && cur.state == MeteorNotification.State.CRASHED) {
                result.add(cur);
            } else {
                result.add(new MeteorNotification(MeteorNotification.State.STANDBY,
                        MeteorLocation.UNKNOWN, MeteorTrigger.NATURAL, 0, 0, 0, ""));
            }
        }
        return result;
    }

    private static int sectionH(MeteorNotification n, MinecraftClient client,
                                 ModConfig cfg, int lineH) {
        int h = VPAD + 18; // header: icon 16px + padding
        if (n.state == MeteorNotification.State.STANDBY) return h + lineH + VPAD;
        h += lineH; // coord always
        h += lineH; // summoned always
        if (cfg.showStatusRow) h += lineH;
        if (client.player != null) h += lineH; // distance always
        return h + VPAD;
    }

    private static int renderSection(DrawContext ctx, TextRenderer tr,
                                     MeteorNotification notif, MinecraftClient client,
                                     ModConfig cfg, int startY, int lineH, int a, int panelW) {
        int accent = accentColor(notif);
        int SECT_H = 18;
        int ty = startY + VPAD;

        ctx.fill(1, startY, panelW-1, startY+VPAD+SECT_H, withAlpha(COL_SECT_BG, (int)(a*0.90f)));
        ctx.fill(3, startY, panelW-1, startY+VPAD+SECT_H, withAlpha(accent, (int)(a*0.10f)));

        // Fire charge icon at full 16px — looks good at normal size
        ctx.drawItem(Items.FIRE_CHARGE.getDefaultStack(), HPAD, ty);

        String title = switch (notif.state) {
            case STANDBY -> "Meteor HUD";
            case FALLING -> "Meteor [Falling]";
            case CRASHED -> "Meteor [Crashed]";
        };
        int titleColor = notif.state == MeteorNotification.State.CRASHED ? COL_UNKNOWN : COL_WHITE;
        ctx.drawTextWithShadow(tr, title, HPAD+18, ty+(SECT_H-tr.fontHeight)/2,
                withAlpha(titleColor, a));

        // Elapsed timer right-aligned
        String elapsed = null;
        if (notif.state == MeteorNotification.State.FALLING) {
            for (MeteorWaypoint wp : WaypointManager.getInstance().getAll()) {
                if (wp.x == notif.x && wp.z == notif.z) { elapsed = wp.elapsedFormatted(); break; }
            }
        } else if (notif.state == MeteorNotification.State.CRASHED && notif.frozenElapsed != null) {
            elapsed = notif.frozenElapsed;
        }
        if (elapsed != null) {
            ctx.drawTextWithShadow(tr, elapsed,
                    panelW-HPAD-tr.getWidth(elapsed)-2,
                    ty+(SECT_H-tr.fontHeight)/2, withAlpha(0x888888, a));
        }
        ty += SECT_H;

        if (notif.state == MeteorNotification.State.STANDBY) {
            ctx.drawTextWithShadow(tr, "Waiting for meteor...", INDENT, ty, withAlpha(COL_UNKNOWN, a));
            return ty + lineH + VPAD;
        }

        ctx.drawTextWithShadow(tr, notif.coordLine(), INDENT, ty, withAlpha(COL_COORD, a));
        ty += lineH;

        boolean isPlayer = notif.trigger == MeteorTrigger.PLAYER_SUMMON && !notif.summonerName.isEmpty();
        String summonLine; int summonColor;
        if (isPlayer) {
            String tag = RemoteDataSync.isFriend(notif.summonerName) ? "[Friend]" : "[Unknown]";
            summonLine  = "Summoned: " + notif.summonerName + " " + tag;
            summonColor = RemoteDataSync.isFriend(notif.summonerName) ? COL_FRIEND : COL_DANGER;
        } else {
            summonLine  = "Summoned: Natural";
            summonColor = COL_SUMMON;
        }
        int maxTextW = panelW - INDENT - HPAD - 2;
        ctx.drawTextWithShadow(tr, truncate(summonLine, tr, maxTextW), INDENT, ty, withAlpha(summonColor, a));
        ty += lineH;

        if (cfg.showStatusRow) {
            String status    = statusLabel(notif);
            String statusText = status;
            // Safe — show which zone
            if ("Safe".equals(status)) {
                String zoneName = RemoteDataSync.zoneName(notif.x, notif.z);
                if (zoneName != null) statusText += " [" + zoneName + "]";
            } else {
                // Wild / Danger — show distance in blocks to nearest mine center
                String nearLabel = RemoteDataSync.nearestZoneLabel(notif.x, notif.z);
                if (nearLabel != null) statusText += " " + nearLabel;
            }
            ctx.drawTextWithShadow(tr, truncate("Status: " + statusText, tr, maxTextW), INDENT, ty, withAlpha(accent, a));
            ty += lineH;
        }

        if (client.player != null) {
            Vec3d p = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
            double dist = Math.sqrt(Math.pow(notif.x-p.x,2)+Math.pow(notif.z-p.z,2));
            String distStr = dist >= 1000
                    ? String.format("%.1fkm", dist/1000.0) : String.format("%.0fm", dist);
            double yaw = Math.toRadians(client.player.getYaw());
            String dir = cardinalDir(notif.x-p.x, notif.z-p.z, yaw);
            ctx.drawTextWithShadow(tr, distStr+"  "+dir, INDENT, ty, withAlpha(COL_DIST, a));
            ty += lineH;
        }
        return ty + VPAD;
    }

    private static void renderEdgeArrows(DrawContext ctx, MinecraftClient client) {
        if (client.player == null) return;
        for (MeteorWaypoint wp : WaypointManager.getInstance().getAll()) {
            if (wp.visible) renderEdgeArrow(ctx, client, wp.x, wp.z, wp.color());
        }
    }

    private static void renderEdgeArrow(DrawContext ctx, MinecraftClient client,
                                         int mx, int mz, int color) {
        Vec3d p = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
        double dx = mx-p.x, dz = mz-p.z;
        double dist = Math.sqrt(dx*dx+dz*dz);
        double yaw = Math.toRadians(client.player.getYaw());
        double relFwd   = dx*(-Math.sin(yaw)) + dz*Math.cos(yaw);
        double relRight = dx*Math.cos(yaw)    + dz*Math.sin(yaw);
        boolean offScreen = relFwd < 0 || Math.abs(relRight) > Math.abs(relFwd)*1.2;
        if (!offScreen && dist < 200) return;

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        int mg = 28;
        double screenAngle = Math.atan2(relRight, relFwd);
        double sinA = Math.sin(screenAngle), cosA = Math.cos(screenAngle);
        double hW = sw/2.0-mg, hH = sh/2.0-mg;
        if (hW <= 0 || hH <= 0) return;
        double scale = (Math.abs(sinA)*hH > Math.abs(cosA)*hW)
                ? hW/Math.max(1e-6, Math.abs(sinA)) : hH/Math.max(1e-6, Math.abs(cosA));
        double ax = Math.max(mg, Math.min(sw-mg, sw/2.0+sinA*scale));
        double ay = Math.max(mg, Math.min(sh-mg, sh/2.0-cosA*scale));

        TextRenderer tr = client.textRenderer;
        String distStr = dist >= 1000 ? String.format("%.1fkm",dist/1000.0) : String.format("%.0fm",dist);
        ctx.drawTextWithShadow(tr, distStr, (int)ax-tr.getWidth(distStr)/2, (int)ay-18, 0xFFFFDD88);
        drawArrow(ctx, (int)ax, (int)ay, screenAngle, color);
    }

    private static void drawArrow(DrawContext ctx, int cx, int cy, double angle, int color) {
        float sin=(float)Math.sin(angle), cos=(float)Math.cos(angle);
        int len=10, wing=6;
        int tipX=cx+(int)(sin*len), tipY=cy+(int)(-cos*len);
        int bx=cx-(int)(sin*5), by=cy-(int)(-cos*5);
        int lx=bx+(int)(-cos*wing), ly=by+(int)(-sin*wing);
        int rx=bx+(int)(cos*wing),  ry=by+(int)(sin*wing);
        drawFilledTriangle(ctx, tipX, tipY, lx, ly, rx, ry, withAlpha(color, 220));
    }

    private static void drawFilledTriangle(DrawContext ctx,
            int x1,int y1, int x2,int y2, int x3,int y3, int color) {
        int minY=Math.min(Math.min(y1,y2),y3), maxY=Math.max(Math.max(y1,y2),y3);
        for (int y=minY; y<=maxY; y++) {
            int[] xs=scanline(y,x1,y1,x2,y2,x3,y3);
            if (xs[0]<=xs[1]) ctx.fill(xs[0],y,xs[1]+1,y+1,color);
        }
    }

    private static int[] scanline(int y, int x1,int y1, int x2,int y2, int x3,int y3) {
        int minX=Integer.MAX_VALUE, maxX=Integer.MIN_VALUE;
        int[][] e={{x1,y1,x2,y2},{x2,y2,x3,y3},{x3,y3,x1,y1}};
        for (int[] s:e) {
            if ((s[1]<=y&&y<s[3])||(s[3]<=y&&y<s[1])) {
                int x=s[0]+(y-s[1])*(s[2]-s[0])/(s[3]-s[1]);
                minX=Math.min(minX,x); maxX=Math.max(maxX,x);
            }
        }
        return new int[]{minX==Integer.MAX_VALUE?0:minX, maxX==Integer.MIN_VALUE?0:maxX};
    }

    private static int contentMinWidth(List<MeteorNotification> entries,
                                         MinecraftClient client, ModConfig cfg,
                                         TextRenderer tr) {
        int min = 160;
        for (MeteorNotification n : entries) {
            if (n.state == MeteorNotification.State.STANDBY) continue;
            min = Math.max(min, INDENT + tr.getWidth(n.coordLine()) + HPAD + 4);
        }
        return Math.min(min, 260); // hard cap — text truncates beyond this
    }

    /** Truncate text to fit within maxWidth pixels, appending "…" if needed. */
    private static String truncate(String text, TextRenderer tr, int maxW) {
        if (tr.getWidth(text) <= maxW) return text;
        String ellipsis = "...";
        while (text.length() > 0 && tr.getWidth(text + ellipsis) > maxW) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    private static String statusLabel(MeteorNotification n) {
        if (n.state==MeteorNotification.State.STANDBY) return "Standby";
        // If scoreboard says your Criminal Record is "Guarded", the area is safe
        // regardless of whether zones.json has it listed yet
        boolean guardedByRecord = ScoreboardReader.getInstance().isGuarded;
        boolean safe = n.location == MeteorLocation.GUARDED || guardedByRecord;
        if (n.trigger == MeteorTrigger.PLAYER_SUMMON) return safe ? "Safe" : "Danger";
        return safe ? "Safe" : "Wild";
    }

    private static int accentColor(MeteorNotification n) {
        if (n.state==MeteorNotification.State.STANDBY) return COL_UNKNOWN;
        if (n.state==MeteorNotification.State.CRASHED) return COL_UNKNOWN;
        boolean safe=n.location==MeteorLocation.GUARDED;
        if (n.trigger==MeteorTrigger.PLAYER_SUMMON) return safe?COL_WILD:COL_DANGER;
        return safe?COL_SAFE:COL_WILD;
    }

    private static String cardinalDir(double dx, double dz, double yaw) {
        double angle=Math.toDegrees(-(Math.atan2(dx,dz)+yaw))%360;
        if (angle<0) angle+=360;
        if (angle<22.5||angle>=337.5) return "Forward";
        if (angle<67.5)  return "Forward-Right";
        if (angle<112.5) return "Right";
        if (angle<157.5) return "Back-Right";
        if (angle<202.5) return "Behind";
        if (angle<247.5) return "Back-Left";
        if (angle<292.5) return "Left";
        return "Forward-Left";
    }

    private static int alpha(float o) { return Math.max(0,Math.min(255,(int)(o*255))); }
    private static int withAlpha(int rgb, int alpha) {
        return ((Math.max(0,Math.min(255,alpha))&0xFF)<<24)|(rgb&0x00FFFFFF);
    }
}

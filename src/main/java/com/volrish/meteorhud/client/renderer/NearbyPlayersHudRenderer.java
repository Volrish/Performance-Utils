package com.volrish.meteorhud.client.renderer;

import com.volrish.meteorhud.client.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Items;

import java.util.*;

/**
 * ============================================================
 *  NEARBY PLAYERS HUD
 * ============================================================
 *  Shows other real players within MAX_DIST blocks.
 *
 *  WHEN IT SHOWS:
 *    - Your Current Zone is NOT a safe zone (scoreboard)
 *    - Criminal Record is not Guarded
 *    - At least one player is within range
 *
 *  FILTERED OUT:
 *    - Players whose names start with "warden_" or "npc" (server bots)
 *    - Your own gang members (matched via scoreboard gang name)
 *    - Players in your friends.json
 *    - Players NOT in the server tab list (genuine NPCs have no tab entry)
 *
 *  FORMAT per row:   PlayerName   247m
 *  Players under 100 blocks highlight in red.
 *
 *  TO CHANGE MAX DISTANCE: edit MAX_DIST
 *  TO CHANGE MAX ROWS:     edit MAX_ROWS
 *  TO CHANGE COLORS:       edit HudTheme.java
 */
public class NearbyPlayersHudRenderer {

    public static final String HUD_ID = "nearby";

    private static final int MAX_DIST = 500;
    private static final int MAX_ROWS = 12;
    private static final int HPAD     = 6;
    private static final int VPAD     = 4;
    private static final int HEADER_H = 16;

    private static int lastPanelH = 60;

    record Entry(String name, int dist) {}

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (client.options.hudHidden) return;
        if (!ModConfig.getInstance().nearbyPlayersHudEnabled) return;

        // Hide in safe zones
        ScoreboardReader sr = ScoreboardReader.getInstance();
        if (sr.isGuarded) return;
        String zone = sr.currentZone.toLowerCase();
        if (zone.contains("safe") || zone.contains("spawn") || zone.contains("lobby")) return;

        List<Entry> nearby = collect(client, sr);
        if (nearby.isEmpty()) return;
        renderInner(ctx, client, nearby);
    }

    public static void renderPreview(DrawContext ctx, MinecraftClient client) {
        renderInner(ctx, client, List.of(
            new Entry("RaiderXXX", 47),
            new Entry("Hunter456", 215)
        ));
    }

    public static int currentPanelWidth(MinecraftClient c)  { return HudPosition.get(HUD_ID).effectiveWidth(); }
    public static int currentPanelHeight(MinecraftClient c) { return lastPanelH; }

    private static void renderInner(DrawContext ctx, MinecraftClient client, List<Entry> nearby) {
        TextRenderer tr     = client.textRenderer;
        int          lineH  = tr.fontHeight + 2;
        HudPosition  pos    = HudPosition.get(HUD_ID);
        int          panelW = pos.effectiveWidth();
        int          a      = alpha(pos.opacity);
        boolean      transp = ModConfig.getInstance().transparentHud;

        // Header + divider line + rows
        int panelH = VPAD + HEADER_H + 1 + nearby.size() * lineH + VPAD;
        lastPanelH = panelH;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(pos.x, pos.y);

        // Background
        if (!transp) {
            ctx.fill(0, 0, panelW, panelH,
                    withAlpha(HudTheme.BG, (int)(HudTheme.BG_ALPHA * a)));
            ctx.fill(0, 0, panelW, 1,
                    withAlpha(HudTheme.NEARBY_ACCENT, a));
            ctx.fill(0, panelH-1, panelW, panelH,
                    withAlpha(HudTheme.BORDER, a));
            ctx.fill(0, 0, 1, panelH,
                    withAlpha(HudTheme.BORDER, a));
            ctx.fill(panelW-1, 0, panelW, panelH,
                    withAlpha(HudTheme.BORDER, a));
        }

        // Header row
        ctx.drawItem(Items.IRON_SWORD.getDefaultStack(), HPAD, VPAD - 1);
        ctx.drawTextWithShadow(tr, "Nearby [" + nearby.size() + "]",
                HPAD + 20, VPAD + (HEADER_H - tr.fontHeight) / 2,
                withAlpha(HudTheme.NEARBY_HEADER, a));

        // Divider line between header and list
        int divY = VPAD + HEADER_H;
        ctx.fill(HPAD, divY, panelW - HPAD, divY + 1,
                withAlpha(HudTheme.BORDER, (int)(a * 0.6f)));

        // Player rows
        int ty = divY + 3;
        for (Entry e : nearby) {
            String distStr = e.dist + "m";
            int    nameCol = e.dist < 100 ? HudTheme.NEARBY_CLOSE : HudTheme.NEARBY_FAR;
            ctx.drawTextWithShadow(tr, e.name, HPAD + 2, ty,
                    withAlpha(nameCol, a));
            ctx.drawTextWithShadow(tr, distStr,
                    panelW - HPAD - tr.getWidth(distStr), ty,
                    withAlpha(HudTheme.TEXT_DIST, a));
            ty += lineH;
        }

        ctx.getMatrices().popMatrix();
    }

    // ---- Data collection ----

    private static List<Entry> collect(MinecraftClient client, ScoreboardReader sr) {
        String  myGang  = sr.gangName.toLowerCase();
        Set<String> friends = new HashSet<>(RemoteDataSync.getFriendNames());

        double px = client.player.getX(), py = client.player.getY(), pz = client.player.getZ();
        List<Entry> result = new ArrayList<>();

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) continue;

            String name   = player.getName().getString();          // fixed for 1.21
            String nameLo = name.toLowerCase();

            // Filter bots/NPCs: not in tab list OR has bot-like name pattern
            if (client.getNetworkHandler() == null ||
                    client.getNetworkHandler().getPlayerListEntry(player.getUuid()) == null) continue;

            if (nameLo.startsWith("warden_") || nameLo.startsWith("npc_")
                    || nameLo.startsWith("bot_")) continue;

            // Filter gang/friends
            if (!myGang.isEmpty() && isGangMember(player, myGang)) continue;
            if (friends.contains(nameLo)) continue;

            double dx = player.getX()-px, dy = player.getY()-py, dz = player.getZ()-pz;
            int dist = (int)Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist <= MAX_DIST) result.add(new Entry(name, dist));
        }

        result.sort(Comparator.comparingInt(e -> e.dist));
        if (result.size() > MAX_ROWS) result = result.subList(0, MAX_ROWS);
        return result;
    }

    private static boolean isGangMember(AbstractClientPlayerEntity p, String myGang) {
        try {
            return p.getDisplayName().getString()
                    .replaceAll("§[0-9a-fk-orA-FK-OR]","")
                    .toLowerCase().contains(myGang);
        } catch (Exception e) { return false; }
    }

    private static int alpha(float o) { return Math.max(0, Math.min(255, (int)(o*255))); }
    private static int withAlpha(int rgb, int a) {
        return ((Math.max(0,Math.min(255,a))&0xFF)<<24)|(rgb&0x00FFFFFF);
    }
}

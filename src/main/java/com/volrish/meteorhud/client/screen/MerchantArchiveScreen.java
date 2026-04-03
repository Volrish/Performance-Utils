package com.volrish.meteorhud.client.screen;

import com.volrish.meteorhud.client.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.*;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * ============================================================
 *  MERCHANT ARCHIVE SCREEN
 * ============================================================
 *  Client-side GUI listing all tracked merchants.
 *  Open via keybind M (configurable in HUD Settings → Keybinds).
 *
 *  TRACK / UNTRACK:
 *    - Tracking a merchant enables its compass arrow direction
 *    - Tracking restores the merchant to the full HUD (shows coords)
 *    - Only one merchant tracked at a time
 *    - Beacon beam uses shouldShowBeam() — only for tracked merchants
 *
 *  CARD LAYOUT per row:
 *    [icon]  TypeName   Killed/Old/Live    COORDS    [Track/Untrack]   15m 49s
 *
 *  TO CHANGE CARD COLORS: edit HudTheme.java
 */
public class MerchantArchiveScreen extends Screen {

    private static final long MINIMIZE_MS = 5 * 60 * 1000L;
    private static final int  ROW_H       = 36;
    private static final int  START_Y     = 54;
    private static final int  CARD_MX     = 20;  // card margin x

    /** ID of the currently tracked merchant. Null = none tracked. */
    private static String activeTrackId = null;

    private final Screen parent;
    private int scrollY = 0;

    public MerchantArchiveScreen(Screen parent) {
        super(Text.literal("Merchant Archive"));
        this.parent = parent;
    }

    /** Returns true if this merchant ID is currently being tracked. */
    public static boolean isTracked(String merchantId) {
        return merchantId != null && merchantId.equals(activeTrackId);
    }

    /** Whether to show the beam for this merchant. */
    public static boolean shouldShowBeam(MerchantWaypoint wp) {
        if (!wp.alive) return false;
        return isTracked(wp.id) && wp.beamUntilMs != -1;
    }

    @Override
    protected void init() {
        clearChildren();
        List<MerchantWaypoint> all = MerchantManager.getInstance().getAll();
        int cx = width / 2;

        for (int i = 0; i < all.size(); i++) {
            final MerchantWaypoint wp = all.get(i);
            int rowY = START_Y + i * ROW_H - scrollY;
            if (rowY + ROW_H < START_Y || rowY > height - 40) continue;
            boolean tracked = isTracked(wp.id);

            addDrawableChild(ButtonWidget.builder(
                    Text.literal(tracked ? "Untrack" : "Track"), btn -> {
                        if (isTracked(wp.id)) {
                            // Untrack
                            activeTrackId = null;
                            wp.beamUntilMs = -1;
                            wp.restoredFromArchive = false;
                        } else {
                            // Track — clear previous
                            clearAllTracking();
                            activeTrackId = wp.id;
                            wp.beamUntilMs = Long.MAX_VALUE;
                            wp.restoredFromArchive = true;
                        }
                        init();
                    }).dimensions(width - CARD_MX - 72, rowY + 8, 68, 18).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
                .dimensions(width / 2 - 40, height - 28, 80, 20).build());
    }

    private static void clearAllTracking() {
        for (MerchantWaypoint wp : MerchantManager.getInstance().getAll()) {
            wp.beamUntilMs = -1;
            wp.restoredFromArchive = false;
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xCC080E16);
        ctx.drawCenteredTextWithShadow(textRenderer, "Merchant Archive",
                width / 2, 14, 0xFFFFDD88);
        ctx.drawCenteredTextWithShadow(textRenderer,
                "Track a merchant to show compass arrow and beacon beam.",
                width / 2, 28, 0xFF555555);

        List<MerchantWaypoint> all = MerchantManager.getInstance().getAll();
        if (all.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    "No merchants tracked yet. They appear when chat detects a merchant spawn.",
                    width / 2, 90, 0xFF444444);
            super.render(ctx, mx, my, delta);
            return;
        }

        long now = System.currentTimeMillis();
        for (int i = 0; i < all.size(); i++) {
            MerchantWaypoint wp = all.get(i);
            int rowY = START_Y + i * ROW_H - scrollY;
            if (rowY + ROW_H < START_Y - 4 || rowY > height - 40) continue;

            boolean tracked = isTracked(wp.id);
            boolean killed  = !wp.alive;
            boolean old     = wp.alive && (now - wp.spawnedAtMs) > MINIMIZE_MS;

            // Card background — green-tinted if tracked
            int bgCol = tracked ? 0xBB0D2A1A : 0xBB0D1A26;
            ctx.fill(CARD_MX, rowY, width - CARD_MX, rowY + ROW_H - 2, bgCol);
            // Left accent bar
            if (tracked) ctx.fill(CARD_MX, rowY, CARD_MX + 2, rowY + ROW_H - 2, 0xFF44FF77);

            // Ore icon
            ctx.drawItem(new ItemStack(typeItem(wp.type)), CARD_MX + 6, rowY + 8);

            // Merchant type label
            int statusCol = killed ? 0xFF4444 : old ? 0xFFDD44 : 0x44FF77;
            String statusStr = killed ? "Killed" : old ? "Old" : "Live";
            ctx.drawTextWithShadow(textRenderer, wp.type.label + " Merchant",
                    CARD_MX + 26, rowY + 8, 0xFFEEEEEE);
            ctx.drawTextWithShadow(textRenderer, statusStr,
                    CARD_MX + 26 + textRenderer.getWidth(wp.type.label + " Merchant") + 6,
                    rowY + 8, statusCol);

            // Coordinates
            ctx.drawTextWithShadow(textRenderer,
                    wp.x + ", " + wp.y + ", " + wp.z,
                    CARD_MX + 26, rowY + 19, 0xFF88EEFF);

            // Timer (far right, above button)
            long ageSec = (now - wp.spawnedAtMs) / 1000;
            String timer = (ageSec / 60) + "m " + (ageSec % 60) + "s";
            ctx.drawTextWithShadow(textRenderer, timer,
                    width - CARD_MX - textRenderer.getWidth(timer) - 8,
                    rowY + 19, 0xFF555555);
        }

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int maxScroll = Math.max(0,
            MerchantManager.getInstance().getAll().size() * ROW_H - (height - START_Y - 44));
        scrollY = Math.max(0, Math.min(maxScroll, scrollY - (int)(dy * 14)));
        init();
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput k) {
        if (k.getKeycode() == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(k);
    }

    @Override
    public void close() { if (client != null) client.setScreen(parent); }

    private net.minecraft.item.Item typeItem(MerchantWaypoint.MerchantType t) {
        return switch (t) {
            case COAL -> Items.COAL; case IRON -> Items.IRON_INGOT;
            case LAPIS -> Items.LAPIS_LAZULI; case REDSTONE -> Items.REDSTONE;
            case GOLD -> Items.GOLD_INGOT; case DIAMOND -> Items.DIAMOND;
            case EMERALD -> Items.EMERALD;
        };
    }
}

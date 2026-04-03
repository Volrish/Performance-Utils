package com.volrish.meteorhud.client;

import com.volrish.meteorhud.client.renderer.*;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD Layout Editor.
 *
 * DRAG BODY   — move the panel
 * DRAG CORNER — resize (bottom-right 10×10 handle, yellow when hovered)
 *
 * Scale works by changing pos.scale which multiplies pos.panelWidth via
 * effectiveWidth(). All layout math in renderers uses effectiveWidth() so
 * everything (text, icons, fills) scales proportionally.
 */
public class HudLayoutScreen extends Screen {

    private final Screen parent;
    private String activeHud = HudPosition.METEOR;

    private boolean moveDragging   = false;
    private int     moveDragOffX, moveDragOffY;
    private boolean resizeDragging = false;
    private int     resizeStartMouseX;
    private float   resizeStartScale;

    private ButtonWidget scaleLabel;
    private ButtonWidget opacityLabel;

    private static final int HANDLE = 12;

    private static final List<String> HUD_IDS = List.of(
        HudPosition.METEOR, HudPosition.MERCHANT,
        HudPosition.ABSORBER, HudPosition.NEARBY
        // COMPASS is top-center fixed — not in layout editor
    );

    public HudLayoutScreen(Screen parent, String hudId) {
        super(Text.literal("HUD Layout Editor"));
        this.parent    = parent;
        this.activeHud = hudId;
    }
    public HudLayoutScreen(Screen parent) { this(parent, HudPosition.METEOR); }
    public HudLayoutScreen()              { this(null,   HudPosition.METEOR); }

    @Override
    protected void init() {
        int bW = 22, labW = 110, gap = 2, cx = width / 2, rowY = height - 50;

        // Scale
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), btn -> adjustScale(-HudPosition.SCALE_STEP))
                .dimensions(cx - bW - gap - labW/2 - 70, rowY, bW, 20).build());
        scaleLabel = addDrawableChild(ButtonWidget.builder(Text.literal(scaleText()), btn -> {})
                .dimensions(cx - labW/2 - 70, rowY, labW, 20).build());
        scaleLabel.active = false;
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> adjustScale(HudPosition.SCALE_STEP))
                .dimensions(cx + labW/2 - 70 + gap, rowY, bW, 20).build());

        // Opacity
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), btn -> adjustOpacity(-0.1f))
                .dimensions(cx - bW - gap - labW/2 + 70, rowY, bW, 20).build());
        opacityLabel = addDrawableChild(ButtonWidget.builder(Text.literal(opacityText()), btn -> {})
                .dimensions(cx - labW/2 + 70, rowY, labW, 20).build());
        opacityLabel.active = false;
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> adjustOpacity(0.1f))
                .dimensions(cx + labW/2 + 70 + gap, rowY, bW, 20).build());

        int btnY = height - 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset Selected"),
                btn -> { HudPosition.get(activeHud).reset(); refreshLabels(); })
                .dimensions(cx - 120, btnY, 112, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
                .dimensions(cx + 8, btnY, 112, 18).build());
    }

    private void adjustScale(float delta) {
        HudPosition pos = HudPosition.get(activeHud);
        pos.scale = Math.max(HudPosition.SCALE_MIN,
                Math.min(HudPosition.SCALE_MAX,
                Math.round((pos.scale + delta) * 10) / 10.0f));
        HudPosition.saveAll(); refreshLabels();
    }

    private void adjustOpacity(float delta) {
        HudPosition pos = HudPosition.get(activeHud);
        pos.opacity = Math.max(0.1f, Math.min(1.0f,
                Math.round((pos.opacity + delta) * 10) / 10.0f));
        HudPosition.saveAll(); refreshLabels();
    }

    private void refreshLabels() {
        if (scaleLabel   != null) scaleLabel.setMessage(Text.literal(scaleText()));
        if (opacityLabel != null) opacityLabel.setMessage(Text.literal(opacityText()));
    }

    private String scaleText()   { return "Scale: " + String.format("%.1fx", HudPosition.get(activeHud).scale); }
    private String opacityText() { return "Opacity: " + Math.round(HudPosition.get(activeHud).opacity * 100) + "%"; }

    @Override
    public boolean mouseClicked(Click click, boolean dbl) {
        if (super.mouseClicked(click, dbl)) return true;
        List<String> rev = new ArrayList<>(HUD_IDS);
        java.util.Collections.reverse(rev);
        for (String id : rev) {
            HudPosition pos = HudPosition.get(id);
            int pw = panelW(id), ph = panelH(id);

            // Resize handle — bottom-right corner
            if (isWithin(click.x(), click.y(), pos.x + pw - HANDLE, pos.y + ph - HANDLE, HANDLE, HANDLE)) {
                activeHud         = id;
                resizeDragging    = true;
                resizeStartMouseX = (int) click.x();
                resizeStartScale  = pos.scale;
                refreshLabels();
                return true;
            }

            // Body drag
            if (isWithin(click.x(), click.y(), pos.x, pos.y, pw, ph)) {
                activeHud    = id;
                moveDragging = true;
                moveDragOffX = (int) click.x() - pos.x;
                moveDragOffY = (int) click.y() - pos.y;
                refreshLabels();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double dX, double dY) {
        if (resizeDragging) {
            HudPosition pos = HudPosition.get(activeHud);
            // Map horizontal drag to scale: 100px drag = 1.0 scale change
            float newScale = resizeStartScale + (float)(click.x() - resizeStartMouseX) / 100f;
            pos.scale = Math.max(HudPosition.SCALE_MIN, Math.min(HudPosition.SCALE_MAX,
                    Math.round(newScale * 10) / 10.0f));
            HudPosition.saveAll();
            refreshLabels();
            return true;
        }
        if (moveDragging) {
            HudPosition pos = HudPosition.get(activeHud);
            int pw = panelW(activeHud), ph = panelH(activeHud);
            pos.set(clamp((int) Math.round(click.x() - moveDragOffX), 0, Math.max(0, width - pw)),
                    clamp((int) Math.round(click.y() - moveDragOffY), 0, Math.max(0, height - ph)));
            return true;
        }
        return super.mouseDragged(click, dX, dY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (resizeDragging || moveDragging) {
            HudPosition.saveAll(); resizeDragging = false; moveDragging = false; return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput k) {
        if (k.getKeycode() == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(k);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xBF101723);
        ctx.fill(0, 0, width, 32, 0x6B294264);

        HudRenderer.renderPreview(ctx, client);
        MerchantHudRenderer.renderPreview(ctx, client);
        AbsorberHudRenderer.renderPreview(ctx, client);
        NearbyPlayersHudRenderer.renderPreview(ctx, client);

        for (String id : HUD_IDS) {
            HudPosition pos = HudPosition.get(id);
            int pw = panelW(id), ph = panelH(id);
            boolean active = id.equals(activeHud);
            int col = active ? 0xFFFFFFFF : 0x55FFFFFF;
            ctx.fill(pos.x-1, pos.y-1, pos.x+pw+1, pos.y,         col);
            ctx.fill(pos.x-1, pos.y+ph, pos.x+pw+1, pos.y+ph+1,   col);
            ctx.fill(pos.x-1, pos.y-1, pos.x,        pos.y+ph+1,  col);
            ctx.fill(pos.x+pw, pos.y-1, pos.x+pw+1,  pos.y+ph+1,  col);

            // Resize handle — yellow when hovered
            boolean hover = isWithin(mx, my, pos.x+pw-HANDLE, pos.y+ph-HANDLE, HANDLE, HANDLE);
            ctx.fill(pos.x+pw-HANDLE, pos.y+ph-HANDLE, pos.x+pw, pos.y+ph,
                    hover ? 0xFFFFDD44 : (active ? 0xFF4488AA : 0x334488AA));
        }

        String mode = resizeDragging ? "resizing (drag right = bigger)"
                    : moveDragging   ? "moving"
                    : "drag body = move | drag yellow corner = scale";
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(hudLabel(activeHud) + "  [" + mode + "]"), width/2, 38, 0xFFD7E6FC);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("HUD Layout Editor"), width/2, 11, 0xFFFFFFFF);

        int rowY = height - 50;
        ctx.drawCenteredTextWithShadow(textRenderer, "Scale",   width/2 - 70, rowY - 12, 0xFFCCCCCC);
        ctx.drawCenteredTextWithShadow(textRenderer, "Opacity", width/2 + 70, rowY - 12, 0xFFCCCCCC);

        super.render(ctx, mx, my, delta);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void close() { HudPosition.saveAll(); if (client != null) client.setScreen(parent); }

    private int panelW(String id) {
        return switch (id) {
            case HudPosition.MERCHANT -> MerchantHudRenderer.currentPanelWidth(client);
            case HudPosition.ABSORBER -> AbsorberHudRenderer.currentPanelWidth(client);
            case HudPosition.NEARBY   -> NearbyPlayersHudRenderer.currentPanelWidth(client);
            default                   -> HudRenderer.currentPanelWidth();
        };
    }
    private int panelH(String id) {
        return switch (id) {
            case HudPosition.MERCHANT -> MerchantHudRenderer.currentPanelHeight(client);
            case HudPosition.ABSORBER -> AbsorberHudRenderer.currentPanelHeight(client);
            case HudPosition.NEARBY   -> NearbyPlayersHudRenderer.currentPanelHeight(client);
            default                   -> HudRenderer.currentPanelHeight(client);
        };
    }
    private static String hudLabel(String id) {
        return switch (id) {
            case HudPosition.MERCHANT -> "Merchant HUD";
            case HudPosition.ABSORBER -> "Utils HUD";
            case HudPosition.NEARBY   -> "Nearby Players HUD";
            default                   -> "Meteor HUD";
        };
    }
    private static boolean isWithin(double x, double y, int ax, int ay, int aw, int ah) {
        return x >= ax && y >= ay && x < ax+aw && y < ay+ah;
    }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}

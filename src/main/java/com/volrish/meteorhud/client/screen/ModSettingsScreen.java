package com.volrish.meteorhud.client.screen;

import com.volrish.meteorhud.client.HudLayoutScreen;
import com.volrish.meteorhud.client.HudPosition;
import com.volrish.meteorhud.client.ModConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD Settings screen.
 * Access: Options → player head button (left of Done).
 *
 * Cards are scrollable. Bottom row: Move/Resize HUDs | Keybinds | □ Transparent | Done
 */
public class ModSettingsScreen extends Screen {

    private static final int CARD_W   = 230;
    private static final int CARD_H   = 74;
    private static final int CARD_GAP = 6;
    private static final int HEADER_H = 50;
    private static final int FOOTER_H = 38;

    private final Screen         parent;
    private final List<FeatureCard> cards = new ArrayList<>();

    private int  scrollY  = 0;
    private int  contentH = 0;

    private FeatureCard           activePopup    = null;
    private boolean               showDisplaySub = false;
    private final List<ButtonWidget> popupBtns   = new ArrayList<>();

    // Transparent toggle button — kept as a field so we can update its label
    private ButtonWidget transparentBtn;

    public ModSettingsScreen(Screen parent) {
        super(Text.literal("HUD Settings"));
        this.parent = parent;
    }

    // ---------------------------------------------------------------- init

    @Override
    public void init() {
        ModConfig cfg = ModConfig.getInstance();
        cards.clear();
        scrollY = 0;
        activePopup = null;
        showDisplaySub = false;
        popupBtns.clear();

        // ---- HUD modules ----
        cards.add(card(Items.FIRE_CHARGE,        "Meteor HUD",        "Meteor alerts with coords, status and distance.",   cfg.meteorHudEnabled,          v -> { cfg.meteorHudEnabled          = v; cfg.save(); }));
        cards.add(card(Items.BEACON,             "Beam Renderer",     "Colored beacon beam at each active meteor.",        cfg.beamEnabled,               v -> { cfg.beamEnabled               = v; cfg.save(); }));
        cards.add(card(Items.WRITABLE_BOOK,      "Chat Detection",    "Reads chat for meteor and merchant events.",        cfg.chatParseEnabled,          v -> { cfg.chatParseEnabled          = v; cfg.save(); }));
        cards.add(card(Items.MAP,                "Zone Sync",         "Fetches safe zones from GitHub on login.",          cfg.zoneSyncEnabled,           v -> { cfg.zoneSyncEnabled           = v; cfg.save(); }));
        cards.add(card(Items.VILLAGER_SPAWN_EGG, "Merchant HUD",      "Ore merchant tracker. Expands near mines.",         cfg.merchantHudEnabled,        v -> { cfg.merchantHudEnabled        = v; cfg.save(); }));
        cards.add(card(Items.BUNDLE,             "Utils HUD",         "Satchel fill % and absorber count.",               cfg.absorberHudEnabled,        v -> { cfg.absorberHudEnabled        = v; cfg.save(); }));
        cards.add(card(Items.IRON_SWORD,         "Nearby Players",    "Players within 500 blocks. Hides in safe zones.",  cfg.nearbyPlayersHudEnabled,   v -> { cfg.nearbyPlayersHudEnabled   = v; cfg.save(); }));
        cards.add(card(Items.COMPASS,            "Compass HUD",       "Top-center compass with meteor and merchant arrows.", cfg.compassHudEnabled,       v -> { cfg.compassHudEnabled         = v; cfg.save(); }));
        cards.add(card(Items.GOLD_INGOT,         "Item Overlays",     "Shows value labels on Charge Orbs, Dust, Money Notes, etc.", cfg.itemValueOverlayEnabled, v -> { cfg.itemValueOverlayEnabled = v; cfg.save(); }));

        // ---- Auto mods — details only (toggled in-game via keybind) ----
        cards.add(detailsOnly(Items.SPONGE,      "Auto Absorber",  "Auto-uses absorber at 100% energy.  Key: K / J (manual)"));
        cards.add(detailsOnly(Items.NETHER_STAR, "Auto Combine",   "Auto-combines Cosmic Energy items.  Key: O"));
        cards.add(detailsOnly(Items.GOLD_NUGGET, "Auto Sell",      "Auto /sell all when inventory full.  Key: L"));

        int cols = cols();
        contentH = (int)Math.ceil(cards.size() / (double)cols) * (CARD_H + CARD_GAP) - CARD_GAP;

        buildCardButtons();
        buildBottomButtons();
    }

    private FeatureCard card(net.minecraft.item.Item icon, String title, String desc,
                              boolean enabled, java.util.function.Consumer<Boolean> toggle) {
        return new FeatureCard(new ItemStack(icon), title, desc, enabled, toggle,
                () -> openPopup(title));
    }

    private FeatureCard detailsOnly(net.minecraft.item.Item icon, String title, String desc) {
        return new FeatureCard(new ItemStack(icon), title, desc, false, v -> {},
                () -> openPopup(title));
    }

    private int cols() { return Math.max(1, (width - 20) / (CARD_W + CARD_GAP)); }

    // ---------------------------------------------------------------- layout

    private void buildCardButtons() {
        int cols   = cols();
        int totalW = cols * CARD_W + (cols - 1) * CARD_GAP;
        int startX = (width - totalW) / 2;

        for (int i = 0; i < cards.size(); i++) {
            FeatureCard fc = cards.get(i);
            fc.x = startX + (i % cols) * (CARD_W + CARD_GAP);
            fc.y = HEADER_H + (i / cols) * (CARD_H + CARD_GAP);

            boolean isAuto = fc.title.startsWith("Auto ");
            final FeatureCard ffc = fc;

            int btnRow = fc.y + CARD_H - 18;

            if (!isAuto) {
                fc.toggleBtn = addDrawableChild(ButtonWidget.builder(
                        Text.literal(fc.enabled ? "ON" : "OFF"), btn -> {
                            ffc.enabled = !ffc.enabled;
                            ffc.onToggle.accept(ffc.enabled);
                            btn.setMessage(Text.literal(ffc.enabled ? "ON" : "OFF"));
                        }).dimensions(fc.x + CARD_W - 104, btnRow, 46, 14).build());
            }
            addDrawableChild(ButtonWidget.builder(Text.literal("Details"),
                    btn -> ffc.onDetails.run())
                    .dimensions(fc.x + CARD_W - 54, btnRow, 50, 14).build());
        }
    }

    private void buildBottomButtons() {
        int cy = height - 24, cx = width / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("Move / Resize HUDs"),
                btn -> client.setScreen(new HudLayoutScreen(this, HudPosition.METEOR)))
                .dimensions(cx - 166, cy, 138, 18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Keybinds"),
                btn -> client.setScreen(new KeybindScreen(this)))
                .dimensions(cx - 22, cy, 72, 18).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
                .dimensions(cx + 54, cy, 52, 18).build());

        // Transparent toggle — small checkbox in bottom-right corner
        ModConfig cfg2 = ModConfig.getInstance();
        transparentBtn = addDrawableChild(ButtonWidget.builder(
                Text.literal(transparentLabel(cfg2.transparentHud)), btn -> {
                    cfg2.transparentHud = !cfg2.transparentHud;
                    cfg2.save();
                    btn.setMessage(Text.literal(transparentLabel(cfg2.transparentHud)));
                }).dimensions(width - 100, height - 22, 96, 16).build());
    }

    private static String transparentLabel(boolean on) {
        return on ? "▣ Transparent" : "□ Transparent";
    }

    // ---------------------------------------------------------------- scroll

    private int maxScroll() {
        return Math.max(0, contentH - (height - HEADER_H - FOOTER_H));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scrollY = clamp(scrollY - (int)(dy * 18), 0, maxScroll());
        return true;
    }

    // ---------------------------------------------------------------- popup

    private void openPopup(String title) {
        activePopup    = cards.stream().filter(c -> c.title.equals(title)).findFirst().orElse(null);
        showDisplaySub = false;
        rebuildPopupButtons();
    }

    private void closePopup() {
        activePopup = null; showDisplaySub = false;
        for (ButtonWidget b : popupBtns) remove(b);
        popupBtns.clear();
    }

    private void rebuildPopupButtons() {
        for (ButtonWidget b : popupBtns) remove(b);
        popupBtns.clear();
        if (activePopup == null) return;

        int pw = 360, ph = 160;
        int px = (width - pw) / 2, py = (height - ph) / 2;
        int btnY = py + ph - 26;

        if (activePopup.title.equals("Meteor HUD") && !showDisplaySub) {
            popupBtns.add(addDrawableChild(ButtonWidget.builder(
                    Text.literal("Display Settings"), btn -> {
                        showDisplaySub = true; rebuildPopupButtons();
                    }).dimensions(px + pw/2 - 142, btnY, 128, 18).build()));
            popupBtns.add(addDrawableChild(ButtonWidget.builder(
                    Text.literal("Move HUD"),
                    btn -> client.setScreen(new HudLayoutScreen(this)))
                    .dimensions(px + pw/2 + 14, btnY, 128, 18).build()));

        } else if (activePopup.title.equals("Meteor HUD") && showDisplaySub) {
            ModConfig cfg = ModConfig.getInstance();
            popupBtns.add(addDrawableChild(ButtonWidget.builder(
                    Text.literal("Status Row: " + onOff(cfg.showStatusRow)), btn -> {
                        cfg.showStatusRow = !cfg.showStatusRow; cfg.save();
                        btn.setMessage(Text.literal("Status Row: " + onOff(cfg.showStatusRow)));
                    }).dimensions(px + pw/2 - 80, py + 72, 160, 18).build()));
            popupBtns.add(addDrawableChild(ButtonWidget.builder(
                    Text.literal("Back"), btn -> {
                        showDisplaySub = false; rebuildPopupButtons();
                    }).dimensions(px + pw/2 - 40, btnY, 80, 18).build()));

        } else {
            popupBtns.add(addDrawableChild(ButtonWidget.builder(
                    Text.literal("Close"), btn -> closePopup())
                    .dimensions(px + pw/2 - 40, btnY, 80, 18).build()));
        }
    }

    // ---------------------------------------------------------------- render

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xCC080E16);
        ctx.drawCenteredTextWithShadow(textRenderer, "HUD Settings", width/2, 12, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                "Toggle and configure each module.", width/2, 26, 0xFF555555);

        int clipT = HEADER_H, clipB = height - FOOTER_H;
        int cols  = cols();

        for (int i = 0; i < cards.size(); i++) {
            FeatureCard fc = cards.get(i);
            int screenY = fc.y - scrollY;
            if (screenY + CARD_H <= clipT || screenY >= clipB) continue;
            drawCard(ctx, fc, mx, my, screenY);

            // Sync button Y every frame
            int btnRow = screenY + CARD_H - 18;
            boolean vis = screenY + CARD_H > clipT && screenY < clipB;
            if (fc.toggleBtn != null) { fc.toggleBtn.setY(btnRow); fc.toggleBtn.visible = vis; }
        }

        // Scroll indicator
        if (maxScroll() > 0) {
            int viewH  = clipB - clipT;
            int thumbH = Math.max(14, viewH * viewH / contentH);
            int thumbY = clipT + (int)((long)scrollY * (viewH - thumbH) / maxScroll());
            ctx.fill(width-5, clipT, width-2, clipB, 0xFF1A2A3A);
            ctx.fill(width-5, thumbY, width-2, thumbY + thumbH, 0xFF4488AA);
        }

        ctx.fill(0, clipT-1, width, clipT, 0xFF1A2A3A);
        ctx.fill(0, clipB,   width, clipB+1, 0xFF1A2A3A);

        super.render(ctx, mx, my, delta);
        if (activePopup != null) drawPopup(ctx);
    }

    private void drawCard(DrawContext ctx, FeatureCard fc, int mx, int my, int sy) {
        boolean hov = mx >= fc.x && mx < fc.x + CARD_W && my >= sy && my < sy + CARD_H;
        int     accent = fc.enabled ? 0xFF44AA66 : 0xFF334455;

        ctx.fill(fc.x, sy, fc.x + CARD_W, sy + CARD_H, hov ? 0xBB1A2A3A : 0xBB0D1A26);
        ctx.fill(fc.x, sy, fc.x + CARD_W, sy + 2, accent);

        // Icon background + icon
        ctx.fill(fc.x + 8, sy + 8, fc.x + 26, sy + 26, 0xFF1A2A3A);
        ctx.drawItem(fc.icon, fc.x + 10, sy + 8);

        // Title
        ctx.drawTextWithShadow(textRenderer, fc.title, fc.x + 34, sy + 9, 0xFFEEEEEE);

        // Description — word wrap
        String[] words = fc.description.split(" ");
        StringBuilder ln = new StringBuilder();
        int dy = sy + 21, maxW = CARD_W - 38;
        for (String w : words) {
            String t = ln.length() == 0 ? w : ln + " " + w;
            if (textRenderer.getWidth(t) > maxW && ln.length() > 0) {
                ctx.drawTextWithShadow(textRenderer, ln.toString(), fc.x + 34, dy, 0xFF555555);
                dy += 9; ln = new StringBuilder(w);
            } else ln = new StringBuilder(t);
        }
        if (ln.length() > 0)
            ctx.drawTextWithShadow(textRenderer, ln.toString(), fc.x + 34, dy, 0xFF555555);
    }

    private void drawPopup(DrawContext ctx) {
        int pw = 360, ph = 160;
        int px = (width - pw)/2, py = (height - ph)/2;
        ctx.fill(0, 0, width, height, 0x88000000);
        ctx.fill(px, py, px+pw, py+ph, 0xFF0D1A26);
        ctx.fill(px, py, px+pw, py+2, 0xFF2A6A8A);
        ctx.fill(px, py, px+1, py+ph, 0xFF2A5A7A);
        ctx.fill(px+pw-1, py, px+pw, py+ph, 0xFF2A5A7A);
        ctx.fill(px, py+ph-1, px+pw, py+ph, 0xFF2A5A7A);
        ctx.drawItem(activePopup.icon, px+12, py+12);
        ctx.drawTextWithShadow(textRenderer, activePopup.title, px+34, py+16, 0xFFFFFFFF);
        if (activePopup.title.equals("Meteor HUD") && showDisplaySub) {
            ctx.drawCenteredTextWithShadow(textRenderer, "Display Settings", px+pw/2, py+46, 0xFFCCCCCC);
        } else {
            ctx.drawTextWithShadow(textRenderer, activePopup.description, px+12, py+38, 0xFF777777);
        }
        ctx.drawCenteredTextWithShadow(textRenderer, "ESC or click outside to close",
                px+pw/2, py+ph-12, 0xFF333333);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(Click click, boolean dbl) {
        if (activePopup != null) {
            int pw = 360, ph = 160, px = (width-pw)/2, py = (height-ph)/2;
            if (click.x() < px || click.x() > px+pw || click.y() < py || click.y() > py+ph) {
                closePopup(); return true;
            }
        }
        return super.mouseClicked(click, dbl);
    }

    @Override
    public boolean keyPressed(KeyInput k) {
        if (k.getKeycode() == GLFW.GLFW_KEY_ESCAPE) {
            if (showDisplaySub) { showDisplaySub = false; rebuildPopupButtons(); return true; }
            if (activePopup != null) { closePopup(); return true; }
        }
        return super.keyPressed(k);
    }

    @Override public void close() { client.setScreen(parent); }

    private static String onOff(boolean v)                    { return v ? "ON" : "OFF"; }
    private static int    clamp(int v, int lo, int hi)        { return Math.max(lo, Math.min(hi, v)); }
}

package com.volrish.meteorhud.client.screen;

import com.volrish.meteorhud.client.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  KEYBIND EDITOR SCREEN
 * ============================================================
 *  Opened from the mod settings screen.
 *  Click "Change" next to any keybind, then press the key you want.
 *  Press Escape to cancel. Press Delete/Backspace to clear a bind (-1 = disabled).
 *
 *  Keybinds are saved to config/meteorhud/config.json.
 *  They do NOT appear in Minecraft's Options > Controls.
 */
public class KeybindScreen extends Screen {

    private final Screen parent;
    private int  listeningIndex = -1; // which row is waiting for a key press

    // Each row: label, getter (returns current key code), setter
    record KeybindRow(String label, java.util.function.IntSupplier getter,
                      java.util.function.IntConsumer setter) {}

    private final List<KeybindRow> rows = new ArrayList<>();

    public KeybindScreen(Screen parent) {
        super(Text.literal("Keybinds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig cfg = ModConfig.getInstance();
        rows.clear();

        // ---- Define all keybinds here ----
        // To add a new keybind: add a KeybindRow with label, getter, setter
        rows.add(new KeybindRow("Absorber Toggle",  () -> cfg.keyAbsorberToggle,  v -> { cfg.keyAbsorberToggle  = v; cfg.save(); }));
        rows.add(new KeybindRow("Absorber Manual",  () -> cfg.keyAbsorberManual,  v -> { cfg.keyAbsorberManual  = v; cfg.save(); }));
        rows.add(new KeybindRow("Combine Toggle",   () -> cfg.keyCombineToggle,   v -> { cfg.keyCombineToggle   = v; cfg.save(); }));
        rows.add(new KeybindRow("Sell Toggle",      () -> cfg.keySellToggle,      v -> { cfg.keySellToggle      = v; cfg.save(); }));

        // Build Change/Clear buttons for each row
        int startY = 60;
        int rowH   = 22;
        for (int i = 0; i < rows.size(); i++) {
            final int idx = i;
            int y = startY + i * rowH;

            addDrawableChild(ButtonWidget.builder(Text.literal("Change"),
                    btn -> listeningIndex = idx)
                    .dimensions(width / 2 + 60, y, 60, 18).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Clear"),
                    btn -> { rows.get(idx).setter().accept(-1); })
                    .dimensions(width / 2 + 126, y, 50, 18).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"),
                btn -> close())
                .dimensions(width / 2 - 50, height - 32, 100, 20).build());
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (listeningIndex >= 0) {
            int code = keyInput.getKeycode();
            if (code == GLFW.GLFW_KEY_ESCAPE) {
                listeningIndex = -1; // cancel
            } else if (code == GLFW.GLFW_KEY_DELETE || code == GLFW.GLFW_KEY_BACKSPACE) {
                rows.get(listeningIndex).setter().accept(-1); // clear
                listeningIndex = -1;
            } else {
                rows.get(listeningIndex).setter().accept(code);
                listeningIndex = -1;
            }
            return true;
        }
        if (keyInput.getKeycode() == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(keyInput);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xBF101723);
        ctx.drawCenteredTextWithShadow(textRenderer, "Keybinds", width / 2, 18, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                "Click Change then press any key. Clear = disabled.",
                width / 2, 34, 0xFF888888);

        int startY = 60;
        int rowH   = 22;
        for (int i = 0; i < rows.size(); i++) {
            int y   = startY + i * rowH;
            int key = rows.get(i).getter().getAsInt();

            // Label
            ctx.drawTextWithShadow(textRenderer, rows.get(i).label(),
                    width / 2 - 180, y + 4, 0xFFCCCCCC);

            // Current key name
            String keyName = listeningIndex == i ? "[ Press a key... ]"
                    : key < 0 ? "[ Disabled ]"
                    : keyName(key);
            int keyColor = listeningIndex == i ? 0xFFFFDD44
                    : key < 0 ? 0xFF666666 : 0xFFFFFFFF;

            ctx.fill(width / 2 - 10, y + 1, width / 2 + 58, y + rowH - 1, 0xFF1A2A3A);
            ctx.drawTextWithShadow(textRenderer, keyName, width / 2 - 6, y + 5, keyColor);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    /** Convert GLFW key code to a readable name */
    private static String keyName(int code) {
        // Letters A-Z
        if (code >= 65 && code <= 90) return String.valueOf((char)code);
        // Numbers 0-9
        if (code >= 48 && code <= 57) return String.valueOf((char)code);
        return switch (code) {
            case GLFW.GLFW_KEY_SPACE        -> "Space";
            case GLFW.GLFW_KEY_LEFT_SHIFT   -> "L.Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT  -> "R.Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "L.Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT     -> "L.Alt";
            case GLFW.GLFW_KEY_TAB          -> "Tab";
            case GLFW.GLFW_KEY_CAPS_LOCK    -> "CapsLock";
            case GLFW.GLFW_KEY_F1           -> "F1";
            case GLFW.GLFW_KEY_F2           -> "F2";
            case GLFW.GLFW_KEY_F3           -> "F3";
            case GLFW.GLFW_KEY_F4           -> "F4";
            case GLFW.GLFW_KEY_F5           -> "F5";
            default -> "Key " + code;
        };
    }
}

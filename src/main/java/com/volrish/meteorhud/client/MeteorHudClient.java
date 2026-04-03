package com.volrish.meteorhud.client;

import com.volrish.meteorhud.client.chat.ChatMeteorListener;
import com.volrish.meteorhud.client.command.MeteorCommand;
import com.volrish.meteorhud.client.command.ItemInfoCommand;
import com.volrish.meteorhud.client.renderer.BeamRenderer;
import com.volrish.meteorhud.client.renderer.HudRenderer;
import com.volrish.meteorhud.client.renderer.MerchantHudRenderer;
import com.volrish.meteorhud.client.renderer.AbsorberHudRenderer;
import com.volrish.meteorhud.client.ScoreboardReader;
import com.volrish.meteorhud.client.renderer.NearbyPlayersHudRenderer;
import com.volrish.meteorhud.client.renderer.CompassHudRenderer;
import com.volrish.meteorhud.client.renderer.ItemValueOverlayRenderer;
import com.volrish.meteorhud.client.screen.MerchantArchiveScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import com.volrish.meteorhud.client.auto.AutoManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;
import com.volrish.meteorhud.client.MerchantManager;
import com.volrish.meteorhud.client.screen.ModSettingsScreen;
import com.volrish.meteorhud.network.MeteorCrashPayload;
import com.volrish.meteorhud.network.MeteorSpawnPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.PlayerSkinDrawer;

public class MeteorHudClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        // World-space beacon beams
        WorldRenderEvents.END_MAIN.register(BeamRenderer::render);

        // HUD overlay
        HudRenderCallback.EVENT.register(HudRenderer::render);
        HudRenderCallback.EVENT.register(MerchantHudRenderer::render);
        HudRenderCallback.EVENT.register(AbsorberHudRenderer::render);
        HudRenderCallback.EVENT.register(NearbyPlayersHudRenderer::render);
        HudRenderCallback.EVENT.register(CompassHudRenderer::render);

        // ---- AUTO MOD KEYBINDS (hidden — not in Options > Controls) ----
        final boolean[] prevDown = new boolean[5];
        final int[] outpostArr = {0};

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean screenOpen = client.currentScreen != null;

            ModConfig cfg = ModConfig.getInstance();
            AutoManager am = AutoManager.getInstance();

            long win = client.getWindow().getHandle();

            boolean[] down = new boolean[]{
                    cfg.keyAbsorberToggle >= 0 && GLFW.glfwGetKey(win, cfg.keyAbsorberToggle) == GLFW.GLFW_PRESS,
                    cfg.keyAbsorberManual >= 0 && GLFW.glfwGetKey(win, cfg.keyAbsorberManual) == GLFW.GLFW_PRESS,
                    cfg.keyCombineToggle  >= 0 && GLFW.glfwGetKey(win, cfg.keyCombineToggle)  == GLFW.GLFW_PRESS,
                    cfg.keySellToggle        >= 0 && GLFW.glfwGetKey(win, cfg.keySellToggle)        == GLFW.GLFW_PRESS,
                    cfg.keyMerchantArchive >= 0 && GLFW.glfwGetKey(win, cfg.keyMerchantArchive) == GLFW.GLFW_PRESS,
            };

            if (!screenOpen) {
                if (down[0] && !prevDown[0]) am.absorber.toggle(client);
                if (down[1] && !prevDown[1]) am.absorber.manualAbsorb(client);
                if (down[2] && !prevDown[2]) am.combine.toggle(client);
                if (down[3] && !prevDown[3]) am.sell.toggle(client);
                if (down[4] && !prevDown[4] && client.currentScreen == null)
                    client.setScreen(new com.volrish.meteorhud.client.screen.MerchantArchiveScreen(null));
            }

            System.arraycopy(down, 0, prevDown, 0, down.length);

            if (cfg.autoAbsorberEnabled)  am.absorber.tick(client);
            if (cfg.autoCombineEnabled)   am.combine.tick(client);
            if (cfg.autoSellEnabled)      am.sell.tick(client);

            outpostArr[0]++;
            if (outpostArr[0] >= 10) { outpostArr[0] = 0; ScoreboardReader.getInstance().tick(client); }
        });

        ChatMeteorListener.register();
        MeteorCommand.register();
        ItemInfoCommand.register();

        // Inject 20x20 player head button into Options screen (your Volrish skin, no empty button)
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof net.minecraft.client.gui.screen.option.OptionsScreen) {
                Screen finalScreen = screen;
                int btnX = screen.width / 2 - 124;
                int btnY = screen.height - 28;

                ButtonWidget headButton = ButtonWidget.builder(Text.empty(), btn ->
                                client.setScreen(new ModSettingsScreen(finalScreen)))
                        .dimensions(btnX, btnY, 20, 20)
                        .build();

                Screens.getButtons(screen).add(headButton);

                // Draw YOUR Volrish skin (face + hat layer) — clean 20x20 head
                ScreenEvents.afterRender(screen).register((s, drawContext, mouseX, mouseY, tickDelta) -> {
                    if (!(s instanceof net.minecraft.client.gui.screen.option.OptionsScreen)) return;

                    int bx = s.width / 2 - 124;
                    int by = s.height - 28;

                    try {
                        MinecraftClient mc = MinecraftClient.getInstance();
                        if (mc.player != null) {
                            PlayerSkinDrawer.draw(drawContext, mc.player.getSkin(), bx + 2, by + 2, 16);
                        }
                        // No else needed — player is almost never null here
                    } catch (Exception ignored) {}
                });
            }
        });

        // Item value overlays
        ScreenEvents.AFTER_INIT.register((c, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterRender(screen).register((s, drawContext, mouseX, mouseY, tickDelta) -> {
                if (!ModConfig.getInstance().itemValueOverlayEnabled) return;
                if (!(s instanceof HandledScreen<?> hs)) return;
                var tr = MinecraftClient.getInstance().textRenderer;

                int hsX = 0, hsY = 0;
                for (Class<?> cls = hs.getClass(); cls != null; cls = cls.getSuperclass()) {
                    try { var f = cls.getDeclaredField("x"); f.setAccessible(true); hsX = (int)f.get(hs); break; } catch (Exception ignored) {}
                }
                for (Class<?> cls = hs.getClass(); cls != null; cls = cls.getSuperclass()) {
                    try { var f = cls.getDeclaredField("y"); f.setAccessible(true); hsY = (int)f.get(hs); break; } catch (Exception ignored) {}
                }

                for (var slot : hs.getScreenHandler().slots) {
                    if (slot.getStack().isEmpty()) continue;
                    int sx = hsX + slot.x, sy = hsY + slot.y;

                    var overlay = ItemValueOverlayRenderer.getOverlay(slot.getStack());
                    if (overlay == null) continue;

                    int col = 0xFF000000 | (overlay.color() & 0xFFFFFF);

                    if (overlay.drawBox()) {
                        // Tier indicator: 4×4 colored box at bottom-right of slot
                        drawContext.fill(sx + 12, sy + 12, sx + 16, sy + 16, col);
                        drawContext.fill(sx + 13, sy + 13, sx + 15, sy + 15, 0x88000000);
                    } else if (overlay.text() != null) {
                        // Value text at bottom-right
                        String txt = overlay.text();
                        drawContext.drawTextWithShadow(tr, txt,
                            sx + 16 - tr.getWidth(txt),
                            sy + 16 - tr.fontHeight, col);
                    }
                }
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client2) -> {
            if (client2.world != null) {
                String dim = client2.world.getRegistryKey().getValue().getPath();
                RemoteDataSync.setPlanet(dim);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client2) -> {
            WaypointManager.getInstance().clearAll();
            MeteorNotification.clear();
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            RemoteDataSync.fetchAsync();
            MerchantManager.getInstance().clear();
        });

        ClientPlayNetworking.registerGlobalReceiver(MeteorSpawnPayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> {
                    MeteorLocation loc  = MeteorLocation.fromOrdinal(payload.locationOrdinal());
                    MeteorTrigger  trig = MeteorTrigger.fromOrdinal(payload.triggerOrdinal());

                    if (loc == MeteorLocation.UNKNOWN) {
                        loc = RemoteDataSync.resolveZone(payload.x(), payload.z());
                    }

                    MeteorWaypoint wp = new MeteorWaypoint(
                            payload.meteorId(), loc, trig,
                            payload.x(), payload.y(), payload.z(),
                            payload.summonerName());

                    WaypointManager.getInstance().add(wp);

                    MeteorNotification.set(new MeteorNotification(
                            MeteorNotification.State.FALLING, loc, trig,
                            payload.x(), payload.y(), payload.z(),
                            payload.summonerName()));
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(MeteorCrashPayload.ID, (payload, ctx) ->
                ctx.client().execute(() -> {
                    WaypointManager.getInstance().remove(payload.meteorId());
                    MeteorNotification.markCrashed(null);
                })
        );
    }
}
package com.volrish.meteorhud.client.auto;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AUTO ABSORBER
 * Fires when cosmic energy hits 100%.
 * Delay: 500ms – 2000ms after energy full.
 * Toggle: K   Manual: J
 */
public class AutoAbsorber {

    private static final int  DELAY_MIN_MS    = 500;
    private static final int  DELAY_MAX_MS    = 2000;
    private static final int  CHECK_TICKS     = 20;   // check lore once/second

    private boolean autoMode        = false;
    private boolean isProcessing    = false;
    private boolean armed           = true;
    private boolean waitingToAbsorb = false;
    private long    absorbAt        = 0;
    private int     tickCounter     = 0;

    private static final Random  RANDOM = new Random();
    private static final Pattern FRAC   = Pattern.compile("\\(([\\d,]+)\\s*/\\s*([\\d,]+)\\)");

    public boolean isEnabled() { return autoMode; }

    public void toggle(MinecraftClient client) {
        autoMode        = !autoMode;
        armed           = true;
        waitingToAbsorb = false;
        if (client.player != null)
            client.player.sendMessage(
                Text.literal("[AutoAbsorber] " + (autoMode ? "ON" : "OFF"))
                    .formatted(autoMode ? Formatting.GREEN : Formatting.YELLOW), true);
    }

    public void manualAbsorb(MinecraftClient client) {
        if (!isProcessing) tryAbsorb(client, true);
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || !autoMode || isProcessing) return;

        if (waitingToAbsorb && System.currentTimeMillis() >= absorbAt) {
            waitingToAbsorb = false;
            tryAbsorb(client, false);
            return;
        }

        tickCounter++;
        if (tickCounter < CHECK_TICKS) return;
        tickCounter = 0;

        ItemStack pk = findCosmicPickaxe(client.player);
        if (pk == null) return;

        boolean full = isEnergyFull(pk);
        if (!armed) { if (!full) armed = true; return; }

        if (full && !waitingToAbsorb) {
            int delay = DELAY_MIN_MS + RANDOM.nextInt(DELAY_MAX_MS - DELAY_MIN_MS + 1);
            absorbAt        = System.currentTimeMillis() + delay;
            waitingToAbsorb = true;
        }
    }

    private boolean isEnergyFull(ItemStack pk) {
        try {
            var lore = pk.get(DataComponentTypes.LORE);
            if (lore == null) return false;
            for (var line : lore.lines()) {
                Matcher m = FRAC.matcher(line.getString()
                        .replaceAll("§[0-9a-fk-orA-FK-OR]","").trim());
                if (m.find()) {
                    long cur = Long.parseLong(m.group(1).replace(",",""));
                    long max = Long.parseLong(m.group(2).replace(",",""));
                    return max > 0 && cur >= max;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private ItemStack findCosmicPickaxe(PlayerEntity player) {
        if (isCosmicPickaxe(player.getMainHandStack())) return player.getMainHandStack();
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (isCosmicPickaxe(s)) return s;
        }
        return null;
    }

    private boolean isCosmicPickaxe(ItemStack stack) {
        if (stack.isEmpty()) return false;
        try {
            var lore = stack.get(DataComponentTypes.LORE);
            if (lore != null)
                for (var line : lore.lines())
                    if (line.getString().replaceAll("§[0-9a-fk-orA-FK-OR]","")
                            .toLowerCase().contains("cosmic energy")) return true;
        } catch (Exception ignored) {}
        return false;
    }

    private void tryAbsorb(MinecraftClient client, boolean manual) {
        if (isProcessing || client.player == null || client.interactionManager == null) return;
        isProcessing = true;
        try {
            PlayerEntity player = client.player;
            int absorberSlot = -1;
            for (int i = 0; i < player.getInventory().size(); i++)
                if (isAbsorber(player.getInventory().getStack(i))) { absorberSlot = i; break; }
            if (absorberSlot == -1) {
                player.sendMessage(Text.literal("[AutoAbsorber] No absorber — off!")
                    .formatted(Formatting.RED), true);
                autoMode = false; return;
            }
            int pickaxeSlot = -1;
            if (isCosmicPickaxe(player.getMainHandStack()))
                pickaxeSlot = player.getInventory().getSelectedSlot();
            else
                for (int i = 0; i < player.getInventory().size(); i++)
                    if (isCosmicPickaxe(player.getInventory().getStack(i))) { pickaxeSlot = i; break; }
            if (pickaxeSlot == -1) return;

            int syncId = player.currentScreenHandler.syncId;
            int abs    = toScreen(absorberSlot), pk = toScreen(pickaxeSlot);
            client.interactionManager.clickSlot(syncId, abs, 0, SlotActionType.PICKUP, player);
            client.interactionManager.clickSlot(syncId, pk,  0, SlotActionType.PICKUP, player);
            client.interactionManager.clickSlot(syncId, abs, 0, SlotActionType.PICKUP, player);

            ItemStack cursor = player.currentScreenHandler.getCursorStack();
            if (!cursor.isEmpty()) {
                int empty = findEmpty(player);
                if (empty >= 0) client.interactionManager.clickSlot(syncId, toScreen(empty), 0, SlotActionType.PICKUP, player);
                else            client.interactionManager.clickSlot(syncId, -999, 0, SlotActionType.PICKUP, player);
            }
            armed = false;
            if (manual) player.sendMessage(Text.literal("[AutoAbsorber] Absorbed!").formatted(Formatting.GREEN), true);
        } finally { isProcessing = false; }
    }

    private boolean isAbsorber(ItemStack s) {
        if (s.isEmpty()) return false;
        String n = s.getName().getString().replaceAll("§[0-9a-fk-orA-FK-OR]","").trim().toLowerCase();
        return n.equals("absorber") || s.getItem().toString().toLowerCase().contains("absorber");
    }
    private int findEmpty(PlayerEntity p) {
        for (int i = 9;  i < 36; i++) if (p.getInventory().getStack(i).isEmpty()) return i;
        for (int i = 0;  i < 9;  i++) if (p.getInventory().getStack(i).isEmpty()) return i;
        return -1;
    }
    private int toScreen(int s) { return s < 9 ? s + 36 : s; }
}

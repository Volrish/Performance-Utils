package com.volrish.meteorhud.client.auto;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AUTO COMBINE
 * Combines Cosmic Energy items 2 at a time.
 * Delay between each combine: 2–3 seconds.
 * Toggle: O
 */
public class AutoCombine {

    private static final int JITTER_MIN_MS = 2000;
    private static final int JITTER_MAX_MS = 3000;

    private boolean enabled     = false;
    private long    nextCombine = 0;

    private static final Random RANDOM = new Random();

    public boolean isEnabled() { return enabled; }

    public void toggle(MinecraftClient client) {
        enabled = !enabled;
        if (client.player != null)
            client.player.sendMessage(
                Text.literal("[AutoCombine] " + (enabled ? "ON" : "OFF"))
                    .formatted(enabled ? Formatting.GREEN : Formatting.GRAY), true);
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null || !enabled) return;
        if (System.currentTimeMillis() < nextCombine) return;
        if (client.currentScreen != null) return;

        List<Integer> slots = findEnergySlots(client);
        if (slots.size() < 2) return;

        int syncId = client.player.currentScreenHandler.syncId;
        int s1 = toScreen(slots.get(0)), s2 = toScreen(slots.get(1));
        client.interactionManager.clickSlot(syncId, s1, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(syncId, s2, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(syncId, s1, 0, SlotActionType.PICKUP, client.player);

        // Schedule next combine 2–3 seconds later
        nextCombine = System.currentTimeMillis() + JITTER_MIN_MS
                      + RANDOM.nextInt(JITTER_MAX_MS - JITTER_MIN_MS + 1);
    }

    private List<Integer> findEnergySlots(MinecraftClient client) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            ItemStack s = client.player.getInventory().getStack(i);
            if (s.isEmpty()) continue;
            String id = s.getItem().toString().toLowerCase();
            String nm = s.getName().getString().toLowerCase();
            if (id.contains("cosmic_energy") || id.contains("energy_gem")
                || nm.contains("cosmic energy") || nm.contains("energy gem")
                || nm.contains("energy orb")    || nm.contains("energy shard")) {
                slots.add(i);
                if (slots.size() >= 2) break;
            }
        }
        return slots;
    }

    private int toScreen(int i) { return i < 9 ? i + 36 : i; }
}

package com.volrish.meteorhud.client.auto;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;

/**
 * AUTO SELL
 * Conditions: ALL 36 slots occupied AND ≥5 ore stacks present.
 * Delay: 1–4 seconds before issuing /sell all.
 * Inventory is checked every tick (not timed) — no FPS impact since
 * it's just iterating 36 ItemStack references.
 * Toggle: L
 */
public class AutoSell {

    private static final int    MIN_ORE_STACKS = 5;
    private static final int    JITTER_MIN_MS  = 1000;
    private static final int    JITTER_MAX_MS  = 4000;

    // TO ADD NEW ORE TYPES: add the keyword (lowercase) to this list
    private static final String[] ORE_KEYWORDS = {
        "coal", "iron", "gold", "diamond", "emerald", "lapis", "redstone",
        "copper", "quartz", "netherite", "ruby", "sapphire", "amethyst",
        "topaz", "cosmic", "ore"
    };

    private boolean enabled       = false;
    private boolean hasSold       = false;
    private boolean waitingToSell = false;
    private long    sellAt        = 0;

    private static final Random RANDOM = new Random();

    public boolean isEnabled() { return enabled; }

    public void toggle(MinecraftClient client) {
        enabled = !enabled; hasSold = false; waitingToSell = false;
        if (client.player != null)
            client.player.sendMessage(
                Text.literal("[AutoSell] " + (enabled ? "ON" : "OFF"))
                    .formatted(enabled ? Formatting.GREEN : Formatting.GRAY), true);
    }

    public void tick(MinecraftClient client) {
        if (client.player == null || !enabled) { hasSold = false; waitingToSell = false; return; }

        // Check conditions every tick — fast array scan, no FPS impact
        boolean full = isInventoryFull(client);
        int     ores = full ? countOreStacks(client) : 0;  // skip ore count if not full
        boolean met  = full && ores >= MIN_ORE_STACKS;

        if (!met) { hasSold = false; waitingToSell = false; return; }
        if (hasSold) return;

        if (!waitingToSell) {
            // Schedule with 1–4 second random delay
            sellAt        = System.currentTimeMillis() + JITTER_MIN_MS
                            + RANDOM.nextInt(JITTER_MAX_MS - JITTER_MIN_MS);
            waitingToSell = true;
        }

        if (System.currentTimeMillis() >= sellAt) {
            client.player.networkHandler.sendChatCommand("sell all");
            hasSold       = true;
            waitingToSell = false;
            client.player.sendMessage(
                Text.literal("[AutoSell] /sell all triggered!").formatted(Formatting.YELLOW), true);
        }
    }

    private boolean isInventoryFull(MinecraftClient client) {
        for (int i = 0; i < 36; i++)
            if (client.player.getInventory().getStack(i).isEmpty()) return false;
        return true;
    }

    private int countOreStacks(MinecraftClient client) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            String id   = stack.getItem().toString().toLowerCase();
            String name = stack.getName().getString().toLowerCase();
            for (String kw : ORE_KEYWORDS)
                if (id.contains(kw) || name.contains(kw)) { count++; break; }
        }
        return count;
    }
}

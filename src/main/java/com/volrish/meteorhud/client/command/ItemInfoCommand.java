package com.volrish.meteorhud.client.command;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.component.type.CustomModelDataComponent;

/**
 * /iteminfo
 *
 * Hold any item and run this command.
 * Prints to chat:
 *   Item:  minecraft:paper
 *   Name:  Simple Clue Scroll
 *   CustomModelData: 1042
 *   Lore line 1
 *   Lore line 2 ...
 *
 * Use the CustomModelData value in your resource pack override.
 */
public class ItemInfoCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(
                ClientCommandManager.literal("iteminfo")
                    .executes(ctx -> {
                        ItemStack stack = ctx.getSource().getPlayer().getMainHandStack();

                        if (stack.isEmpty()) {
                            ctx.getSource().sendFeedback(
                                Text.literal("[ItemInfo] Hold an item first.")
                                    .formatted(Formatting.RED));
                            return 0;
                        }

                        // Item ID
                        String itemId = stack.getItem().toString();
                        ctx.getSource().sendFeedback(
                            Text.literal("=== ItemInfo ===").formatted(Formatting.GOLD));
                        ctx.getSource().sendFeedback(
                            Text.literal("Item: " + itemId).formatted(Formatting.YELLOW));

                        // Display name
                        String name = stack.getName().getString()
                            .replaceAll("§[0-9a-fk-orA-FK-OR]", "");
                        ctx.getSource().sendFeedback(
                            Text.literal("Name: " + name).formatted(Formatting.WHITE));

                        // Custom model data — check both old NBT path and new component
                        int cmd = getCustomModelData(stack);
                        if (cmd != 0) {
                            ctx.getSource().sendFeedback(
                                Text.literal("CustomModelData: " + cmd)
                                    .formatted(Formatting.AQUA));
                        } else {
                            ctx.getSource().sendFeedback(
                                Text.literal("CustomModelData: none").formatted(Formatting.GRAY));
                        }

                        // Lore lines
                        var lore = stack.get(DataComponentTypes.LORE);
                        if (lore != null && !lore.lines().isEmpty()) {
                            ctx.getSource().sendFeedback(
                                Text.literal("Lore:").formatted(Formatting.GRAY));
                            for (var line : lore.lines()) {
                                String raw = line.getString()
                                    .replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
                                if (!raw.isEmpty())
                                    ctx.getSource().sendFeedback(
                                        Text.literal("  " + raw).formatted(Formatting.DARK_GRAY));
                            }
                        }

                        // Stack count
                        ctx.getSource().sendFeedback(
                            Text.literal("Count: " + stack.getCount()).formatted(Formatting.GRAY));

                        return 1;
                    })
            )
        );
    }

    /**
     * Reads CustomModelData from the item stack.
     * Checks both the 1.21+ component API and old NBT for compatibility.
     */
    private static int getCustomModelData(ItemStack stack) {
        // NBT fallback (works perfectly on Cosmic Prisons / 1.21.1 servers)
        try {
            NbtComponent nbtComp = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (nbtComp != null) {
                NbtCompound nbt = nbtComp.copyNbt();
                if (nbt.contains("CustomModelData")) {
                    return nbt.getInt("CustomModelData").orElse(0);
                }
            }
        } catch (Exception ignored) {}

        return 0;
    }
}

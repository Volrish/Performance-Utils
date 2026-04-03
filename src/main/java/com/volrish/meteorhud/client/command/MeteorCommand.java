package com.volrish.meteorhud.client.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.volrish.meteorhud.client.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

import java.util.List;

/**
 * /meteor list                     — show active waypoints with status and distance
 * /meteor clear                    — remove all active waypoints
 * /meteor setloc <id> <safe|wild>  — manually tag a [?] waypoint
 * /meteor sync                     — re-fetch zones + friends from GitHub
 */
public class MeteorCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(
                ClientCommandManager.literal("meteor")

                    .then(ClientCommandManager.literal("list")
                        .executes(ctx -> {
                            List<MeteorWaypoint> all = WaypointManager.getInstance().getAll();
                            if (all.isEmpty()) {
                                ctx.getSource().sendFeedback(Text.literal("[MeteorHUD] No active meteors."));
                                return 0;
                            }
                            all.forEach(wp -> ctx.getSource().sendFeedback(Text.literal(
                                    "  " + wp.id + " [" + wp.status() + "] @ "
                                    + wp.x + ", " + wp.y + ", " + wp.z)));
                            return 1;
                        })
                    )

                    .then(ClientCommandManager.literal("clear")
                        .executes(ctx -> {
                            WaypointManager.getInstance().clearAll();
                            ctx.getSource().sendFeedback(Text.literal("[MeteorHUD] Cleared all meteors."));
                            return 1;
                        })
                    )

                    .then(ClientCommandManager.literal("setloc")
                        .then(ClientCommandManager.argument("id", StringArgumentType.word())
                        .then(ClientCommandManager.argument("loc", StringArgumentType.word())
                            .executes(ctx -> {
                                String id     = StringArgumentType.getString(ctx, "id");
                                String locStr = StringArgumentType.getString(ctx, "loc");
                                MeteorLocation newLoc = switch (locStr.toLowerCase()) {
                                    case "safe", "guarded" -> MeteorLocation.GUARDED;
                                    case "wild"            -> MeteorLocation.WILD;
                                    default                -> MeteorLocation.UNKNOWN;
                                };
                                MeteorWaypoint wp = WaypointManager.getInstance().get(id).orElse(null);
                                if (wp == null) {
                                    ctx.getSource().sendFeedback(Text.literal(
                                            "[MeteorHUD] Not found: " + id + " — use /meteor list"));
                                    return 0;
                                }
                                wp.location = newLoc;
                                WaypointManager.getInstance().save();
                                ctx.getSource().sendFeedback(Text.literal(
                                        "[MeteorHUD] " + id + " → [" + wp.status() + "]"));
                                return 1;
                            })
                        ))
                    )

                    .then(ClientCommandManager.literal("sync")
                        .executes(ctx -> {
                            RemoteDataSync.fetchAsync();
                            ctx.getSource().sendFeedback(Text.literal(
                                    "[MeteorHUD] Syncing zones and friends from GitHub..."));
                            return 1;
                        })
                    )
            )
        );
    }
}

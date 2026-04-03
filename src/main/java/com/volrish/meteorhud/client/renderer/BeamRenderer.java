package com.volrish.meteorhud.client.renderer;

import com.volrish.meteorhud.client.screen.MerchantArchiveScreen;

import com.volrish.meteorhud.client.*;
import com.volrish.meteorhud.client.RemoteDataSync;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class BeamRenderer {

    private static final int    BEAM_HEIGHT      = 2048;
    private static final float  INNER_RADIUS     = 0.35f;
    private static final float  OUTER_RADIUS     = 0.75f;
    private static final double MAX_DIST_SQ      = 2048.0 * 2048.0;
    private static final double MERCHANT_HIDE_SQ = 10.0 * 10.0;  // hide beam within 10 blocks

    public static void render(WorldRenderContext ctx) {
        if (!ModConfig.getInstance().beamEnabled) return;
        if (ctx == null || ctx.matrices() == null || ctx.commandQueue() == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.gameRenderer == null || client.gameRenderer.getCamera() == null) return;

        Vec3d cam = client.gameRenderer.getCamera().getCameraPos();
        float tickProgress = client.getRenderTickCounter() != null
                ? client.getRenderTickCounter().getTickProgress(false) : 0f;
        float rotation = (float)(System.currentTimeMillis() % 2000) / 2000f * 360f;

        // Meteor beams
        for (MeteorWaypoint wp : WaypointManager.getInstance().getAll()) {
            if (!wp.visible) continue;
            renderBeam(ctx, cam, tickProgress, rotation, wp.x, wp.y, wp.z, wp.color());
        }

        // Merchant beams — only when player is inside any mine zone
        if (client.player != null) {
            int ppx = (int)client.player.getX(), ppz = (int)client.player.getZ();
            boolean inMine = RemoteDataSync.isInAnyZone(ppx, ppz);
            if (inMine) {
                Vec3d player = new Vec3d(
                        client.player.getX(), client.player.getY(), client.player.getZ());
                for (MerchantWaypoint wp : MerchantManager.getInstance().getAll()) {
                    // Only show beam if user has clicked "Track" in Merchant Archive
                    if (!MerchantArchiveScreen.shouldShowBeam(wp)) continue;
                    double playerDistSq = wp.distanceSq(player.x, player.y, player.z);
                    if (playerDistSq <= MERCHANT_HIDE_SQ) continue;
                    renderBeam(ctx, cam, tickProgress, rotation, wp.x, wp.y, wp.z, wp.color());
                }
            }
        }
    }

    private static void renderBeam(WorldRenderContext ctx, Vec3d cam,
                                   float tickProgress, float rotation,
                                   int x, int y, int z, int color) {
        double dx = x - cam.x, dy = y - cam.y, dz = z - cam.z;
        if (dx*dx + dy*dy + dz*dz > MAX_DIST_SQ) return;

        ctx.matrices().push();
        ctx.matrices().translate(x - cam.x, -cam.y, z - cam.z);

        BeaconBlockEntityRenderer.renderBeam(
                ctx.matrices(),
                ctx.commandQueue(),
                BeaconBlockEntityRenderer.BEAM_TEXTURE,
                tickProgress,
                rotation,
                0,
                BEAM_HEIGHT,
                color,
                INNER_RADIUS,
                OUTER_RADIUS
        );

        ctx.matrices().pop();
    }
}

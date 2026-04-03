package com.volrish.meteorhud.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server -> Client: a meteor has crashed (landed).
 *
 * The client will:
 *   1. Remove the waypoint matching meteorId.
 *   2. Remove the beam.
 *   3. Remove the Xaero waypoint.
 *   4. Show the crash HUD notification with the final coordinates.
 *
 * Server usage example:
 *
 *   ServerPlayNetworking.send(player, new MeteorCrashPayload(
 *       "meteor_001",       // must match the ID from MeteorSpawnPayload
 *       1474, 97, -502      // final crash coordinates
 *   ));
 */
public record MeteorCrashPayload(
        String meteorId,
        int x, int y, int z
) implements CustomPayload {

    public static final CustomPayload.Id<MeteorCrashPayload> ID =
            new CustomPayload.Id<>(Identifier.of("meteorhud", "crash"));

    public static final PacketCodec<PacketByteBuf, MeteorCrashPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,  MeteorCrashPayload::meteorId,
            PacketCodecs.VAR_INT, MeteorCrashPayload::x,
            PacketCodecs.VAR_INT, MeteorCrashPayload::y,
            PacketCodecs.VAR_INT, MeteorCrashPayload::z,
            MeteorCrashPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}

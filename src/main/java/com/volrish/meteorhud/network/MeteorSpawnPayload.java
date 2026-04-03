package com.volrish.meteorhud.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server -> Client: a meteor has started falling.
 *
 * locationOrdinal: 0 = WILD (orange), 1 = GUARDED (green)
 * triggerOrdinal:  0 = NATURAL,       1 = PLAYER_SUMMON
 *
 * Examples:
 *
 *   // Wild natural meteor:
 *   new MeteorSpawnPayload("m_001", 0, 0, -602, 93, -390, "")
 *
 *   // Guarded natural meteor (mine zone):
 *   new MeteorSpawnPayload("m_002", 1, 0, 1474, 97, -502, "")
 *
 *   // Wild player-summoned meteor:
 *   new MeteorSpawnPayload("m_003", 0, 1, 1677, 114, 717, "--zInfamoussh")
 *
 *   // Guarded player-summoned meteor:
 *   new MeteorSpawnPayload("m_004", 1, 1, 200, 64, 300, "--zInfamoussh")
 */
public record MeteorSpawnPayload(
        String meteorId,
        int locationOrdinal,   // MeteorLocation.ordinal()
        int triggerOrdinal,    // MeteorTrigger.ordinal()
        int x, int y, int z,
        String summonerName    // empty string for NATURAL
) implements CustomPayload {

    public static final CustomPayload.Id<MeteorSpawnPayload> ID =
            new CustomPayload.Id<>(Identifier.of("meteorhud", "spawn"));

    public static final PacketCodec<PacketByteBuf, MeteorSpawnPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,  MeteorSpawnPayload::meteorId,
            PacketCodecs.VAR_INT, MeteorSpawnPayload::locationOrdinal,
            PacketCodecs.VAR_INT, MeteorSpawnPayload::triggerOrdinal,
            PacketCodecs.VAR_INT, MeteorSpawnPayload::x,
            PacketCodecs.VAR_INT, MeteorSpawnPayload::y,
            PacketCodecs.VAR_INT, MeteorSpawnPayload::z,
            PacketCodecs.STRING,  MeteorSpawnPayload::summonerName,
            MeteorSpawnPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}

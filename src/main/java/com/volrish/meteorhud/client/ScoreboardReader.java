package com.volrish.meteorhud.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  SCOREBOARD READER
 * ============================================================
 *  Reads the sidebar scoreboard that Cosmic Prisons always
 *  sends to the client, even when /toggles hides it visually.
 *
 *  Parsed fields (updated every 10 ticks from MeteorHudClient):
 *    criminalRecord  — "Neutral", "Guarded 4", etc.
 *    isGuarded       — true when criminalRecord contains "Guarded"
 *    currentZone     — "Gold", "Chain", "Diamond Mine", etc.
 *    gangName        — your gang name, or ""
 *    balance         — "$2,374,508.19" raw string
 *
 *  HOW IT WORKS:
 *  The server sends the scoreboard sidebar as a list of Team
 *  objects where prefix+suffix form each visible line.
 *  We strip colour codes and pattern-match the lines.
 *
 *  TO ADD A NEW FIELD: add a pattern match in parseLine().
 */
public class ScoreboardReader {

    private static final ScoreboardReader INSTANCE = new ScoreboardReader();
    public static ScoreboardReader getInstance() { return INSTANCE; }

    // ---- Parsed state ----
    public String  criminalRecord  = "";
    public boolean isGuarded       = false;
    public String  currentZone     = "";
    public String  gangName        = "";
    public String  truceGang       = "";
    public String  balance         = "";

    // Reading state machine
    private String lastHeader = "";

    private ScoreboardReader() {}

    /** Called every 10 ticks from MeteorHudClient tick handler. */
    public void tick(MinecraftClient client) {
        if (client.world == null) return;
        Scoreboard sb = client.world.getScoreboard();
        ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (obj == null) { clear(); return; }

        List<String> lines = extractLines(sb);
        parseLines(lines);
    }

    private List<String> extractLines(Scoreboard sb) {
        List<String> out = new ArrayList<>();
        for (Team team : sb.getTeams()) {
            String prefix = team.getPrefix() != null ? team.getPrefix().getString() : "";
            String suffix = team.getSuffix() != null ? team.getSuffix().getString() : "";
            String line   = strip(prefix + suffix).trim();
            if (!line.isEmpty()) out.add(line);
        }
        return out;
    }

    private void parseLines(List<String> lines) {
        lastHeader = "";
        for (String line : lines) {
            String lo = line.toLowerCase();

            // Section headers — next line is the value
            if (lo.contains("criminal record")) { lastHeader = "criminal"; continue; }
            if (lo.contains("current zone"))    { lastHeader = "zone";     continue; }
            if (lo.contains("truce"))           { lastHeader = "truce";    continue; }
            if (lo.contains("gang"))            { lastHeader = "gang";     continue; }
            if (lo.contains("balance"))         { lastHeader = "balance";  continue; }

            // Value lines follow headers
            switch (lastHeader) {
                case "criminal" -> {
                    criminalRecord = line.trim();
                    isGuarded = lo.contains("guard");
                    lastHeader = "";
                }
                case "zone" -> {
                    currentZone = line.trim();
                    lastHeader = "";
                }
                case "gang" -> {
                    gangName = line.trim();
                    lastHeader = "";
                }
                case "truce" -> {
                    truceGang = line.trim();
                    lastHeader = "";
                }
                case "balance" -> {
                    balance = line.trim();
                    lastHeader = "";
                }
            }
        }
    }

    private void clear() {
        criminalRecord = ""; isGuarded = false;
        currentZone = ""; gangName = ""; balance = "";
    }

    /** Strip Minecraft colour codes and non-ASCII characters. */
    public static String strip(String s) {
        return s.replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                .replaceAll("[^\\x20-\\x7E]", "").trim();
    }
}

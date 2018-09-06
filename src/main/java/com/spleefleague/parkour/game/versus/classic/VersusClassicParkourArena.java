/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game.versus.classic;

import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.DBSave;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.gameapi.events.BattleStartEvent;
import com.spleefleague.gameapi.queue.GameQueue;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.Arena;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.player.ParkourPlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import org.bukkit.ChatColor;

/**
 *
 * @author jonas
 */
public class VersusClassicParkourArena extends Arena<VersusClassicParkourBattle> {
    
    private Map<String, Long> highscores = new HashMap<>();
    
    @Override
    public boolean hasLeaderboard() {
        return true;
    }
    
    @Override
    public void setHighscore(String player, long score) {
        highscores.put(player, score);
        EntityBuilder.save(this, Parkour.getInstance().getPluginDB().getCollection("Arenas"));
    }
    
    @Override
    public List<String> getLeaderboard() {
        List<String> leaderboard = new ArrayList<>();
        List<String> hs = new ArrayList<>();
        highscores.keySet().forEach(k -> hs.add(k));
        Collections.sort(hs, (a, b) -> {
            return highscores.get(a) > highscores.get(b) ? 1 : -1;
        });
        int i = 0;
        for (String s : hs) {
                i++;
                leaderboard.add(Parkour.fillColor + i + ": "
                        + Parkour.playerColor + s
                        + Parkour.fillColor + ": "
                        + Parkour.pointColor + String.format("%.2f", highscores.get(s) / 20.f)
                        + Parkour.fillColor + " seconds");
        }
        return leaderboard;
    }
    
    @DBLoad(fieldName = "highscores")
    private void setHighscores(Document highscores) {
        highscores.forEach((name, score) -> {
            this.highscores.put(name, (long) score);
        });
    }
    
    @DBSave(fieldName = "highscores")
    private Document getHighscores() {
        Document hsdoc = new Document();
        for (Map.Entry<String, Long> hs : highscores.entrySet()) {
            hsdoc.append(hs.getKey(), hs.getValue());
        }
        return hsdoc;
    }

    @Override
    public VersusClassicParkourBattle startBattle(List<ParkourPlayer> players, BattleStartEvent.StartReason reason) {
        if (!isOccupied()) { //Shouldn't be necessary
            VersusClassicParkourBattle battle = new VersusClassicParkourBattle(this, players);
            battle.start(reason);
            return battle;
        }
        return null;
    }

    @Override
    public Function<SLPlayer, List<String>> getDynamicDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add("");
            description.add(ChatColor.GRAY + "Map Multiplier: " 
                    + ChatColor.GOLD + "x" + String.format("%.2f", this.getMapMultiplier()));
            description.add(ChatColor.GRAY + "Personal Record: " 
                    + ChatColor.GOLD + sjp.getVersusClassicScoreFormatted(this.getName()));
            return description;
        };
    }
    
    private static final Map<String, VersusClassicParkourArena> arenas = new HashMap<>();
    
    public static Collection<VersusClassicParkourArena> getAll() {
        return arenas.values();
    }
    
    public static VersusClassicParkourArena byName(String arena) {
        return arenas.get(arena.toLowerCase());
    }
    
    public static void loadArena(Document document) {
        Class<? extends VersusClassicParkourArena> c = VersusClassicParkourArena.class;
        if (document.containsKey("arenaClass")) {
            try {
                c = (Class<? extends VersusClassicParkourArena>) Class.forName(document.getString("arenaClass"));
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Arena.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        VersusClassicParkourArena arena = EntityBuilder.load(document, c);
        if(arenas.containsKey(arena.getName().toLowerCase())) {
            Arena.recursiveCopy(arena, byName(arena.getName()), Arena.class);
        }
        else {
            Parkour.getInstance().getBattleManager(ParkourMode.CLASSIC).registerArena(arena);
            arenas.put(arena.getName().toLowerCase(), arena);
        }
    }
}

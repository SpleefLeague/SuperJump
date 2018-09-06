/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game.conquest;

import com.mongodb.client.MongoCursor;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.entitybuilder.DBEntity;
import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.DBLoadable;
import com.spleefleague.entitybuilder.DBSave;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.gameapi.events.BattleStartEvent;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.Arena;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.player.ParkourPlayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bson.Document;
import org.bukkit.ChatColor;

/**
 *
 * @author jonas
 */
public class ConquestParkourArena extends Arena<ConquestParkourBattle> {
    
    @DBLoad(fieldName = "conquestPack")
    private String pack;
    @DBLoad(fieldName = "starTimes")
    private List<Long> starTimes = new ArrayList<>();
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
    public ConquestParkourBattle startBattle(List<ParkourPlayer> players, BattleStartEvent.StartReason reason) {
        if (!isOccupied()) { //Shouldn't be necessary
            ConquestParkourBattle battle = new ConquestParkourBattle(this, players);
            battle.start(reason);
            return battle;
        }
        return null;
    }
    
    private static Map<String, ConquestParkourArena> arenas = new HashMap<>();
    private static Map<String, Pack> packs;
    
    public static void initPacks() {
        packs = new HashMap<>();
        String packName = "";
        int packDiff = 0;
        int menuLoc = 0;
        MongoCursor<Document> dbc = Parkour.getInstance().getPluginDB()
                .getCollection("ConquestPacks")
                .find()
                .iterator();
        while (dbc.hasNext()) {
            Document packDoc = dbc.next();
            try {
                
            } catch(Exception e) {
                Parkour.getInstance().log("Error loading pack: " + packDoc.get("name"));
            }
            Pack pack = EntityBuilder.load(packDoc, Pack.class);
            pack.init();
            for(ConquestParkourArena arena : arenas.values()) {
                if(arena.getPack().equals(pack.getName())) {
                    pack.addArena(arena.getName());
                }
            }
            packs.put(pack.getName(), pack);
        }
    }

    @Override
    public Function<SLPlayer, List<String>> getDynamicDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add("");
            description.add(ChatColor.GRAY + "Stars Earned: " + sjp.getConquestStarsFormatted(this));
            description.add(ChatColor.GRAY + "Personal Record: " 
                    + ChatColor.GOLD + sjp.getConquestScoreFormatted(this.getName()));
            return description;
        };
    }
    
    public int getStars(long time) {
        if(time <= starTimes.get(0)) {
            return 3;
        }
        else if(time <= starTimes.get(1)) {
            return 2;
        }
        else if(time > 0) {
            return 1;
        }
        return 0;
    }
    
    public static Collection<ConquestParkourArena> getAll() {
        return arenas.values();
    }
    
    public static Collection<Pack> getAllPacks() {
        return packs.values();
    }
    
    public static ConquestParkourArena byName(String arena) {
        return arenas.get(arena.toLowerCase());
    }
    
    public String getPack() {
        return pack;
    }
    
    public static void loadArena(Document document) {
        Class<? extends ConquestParkourArena> c = ConquestParkourArena.class;
        if (document.containsKey("arenaClass")) {
            try {
                c = (Class<? extends ConquestParkourArena>) Class.forName(document.getString("arenaClass"));
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ConquestParkourArena.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        ConquestParkourArena arena = EntityBuilder.load(document, c);
        if(arenas.containsKey(arena.getName().toLowerCase())) {
            ConquestParkourArena.recursiveCopy(arena, byName(arena.getName()), ConquestParkourArena.class);
        }
        else {
            Parkour.getInstance().getBattleManager(ParkourMode.CONQUEST).registerArena(arena);
            arenas.put(arena.getName().toLowerCase(), arena);
        }
    }
    
    public class Pack extends DBEntity implements DBLoadable {
        
        @DBLoad(fieldName="name")
        private String name;
        @DBLoad(fieldName="difficulty")
        private int difficulty;
        @DBLoad(fieldName="menuLoc")
        private int menuLoc;
        @DBLoad(fieldName="default")
        private boolean isDefault = false;
        private List<String> arenas;
        
        public void init() {
            arenas = new ArrayList<>();
        }
        
        public boolean isDefault() {
            return isDefault;
        }
        
        public String getName() {
            return name;
        }
        
        public int getDifficulty() {
            return difficulty;
        }
        
        public int getMenuLoc() {
            return menuLoc;
        }
        
        public void addArena(String arena) {
            arenas.add(arena);
        }
        
        public List<String> getArenas() {
            return arenas;
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.player;

import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatChannel;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.DBLoadable;
import com.spleefleague.entitybuilder.DBSave;
import com.spleefleague.entitybuilder.DBSaveable;
import com.spleefleague.entitybuilder.TypeConverter;
import com.spleefleague.gameapi.player.RatedPlayer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.ParkourBattle;
import com.spleefleague.parkour.game.Arena;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.variable.SimpleDate;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bson.Document;
import org.bukkit.ChatColor;

/**
 *
 * @author Jonas, NickM13
 */
public class ParkourPlayer extends RatedPlayer<ParkourMode> {
    
    private boolean ingame, frozen, requestingEndgame;
    private Set<Arena> visitedArenas;
    private ParkourMode parkourMode;
    
    // New elo system
    @DBSave(fieldName = "SJPoints")
    @DBLoad(fieldName = "SJPoints")
    private int points;
    @DBSave(fieldName = "SJParagon")
    @DBLoad(fieldName = "SJParagon")
    private int paragon;
    
    // Currency
    @DBSave(fieldName = "SJParagonCoins")
    @DBLoad(fieldName = "SJParagonCoins")
    private int paragonCoins;
    
    private int endlessStartDate; // When starting a match, stores the date for indexing
    private Map<Integer, EndlessStats> endlessStatsMap;
    private int endlessLevelRecord;
    
    public ParkourPlayer() {
        super(ParkourMode.class, Parkour.getInstance().getPluginDB().getCollection("Players"));
        this.visitedArenas = new HashSet<>();
        this.parkourMode = ParkourMode.NONE;
        this.endlessStatsMap = new HashMap<>();
        this.endlessStartDate = -1;
        this.endlessLevelRecord = 0;
        setDefaults();
    }
    
    public void addPoints(int points, String info) {
        this.points += points;
        ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()), Parkour.getInstance().getChatPrefix()
                + ChatColor.GREEN + " You gained "
                + ChatColor.GOLD + points
                + ChatColor.GREEN + (points == 1 ? " point " : " points ")
                + info);
        while(this.points >= 5000) {
            this.points -= 5000;
            this.paragon++;
            this.paragonCoins++;
            ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(),
                      ChatColor.RED + " " + this.getName()
                    + ChatColor.GREEN + " is now Paragon "
                    + ChatColor.WHITE + this.paragon
                    + ChatColor.GREEN + "!",
                    Parkour.getInstance().getEndMessageChannel());
        }
    }
    
    public int getTotalPoints() {
        return (this.points + this.paragon * 5000);
    }
    
    public int getPoints() {
        return (this.points);
    }
    
    public int getParagonLevel() {
        return (this.paragon);
    }
    
    public float getEndlessMultiplier() {
        return Math.floorDiv(Math.min(getEndlessLevel(), 100), 20) * 0.05f;
    }
    
    public float getPointMultiplier() {
        return getEndlessMultiplier();
    }
    
    public void setParkourMode(ParkourMode mode) {
        parkourMode = mode;
    }
    
    public ParkourMode getParkourMode() {
        return parkourMode;
    }
    
    // Check if stats are for today
    public boolean validateEndless() {
        SimpleDate today = new SimpleDate();
        boolean exists = endlessStatsMap.containsKey(today.asInt());
        if(!exists)
            endlessStatsMap.put(today.asInt(), new EndlessStats());
        if(!exists || endlessStartDate != today.asInt()) {
            endlessStartDate = today.asInt();
            return false;
        }
        return true;
    }
    
    public int getEndlessLevel() {
        return(endlessStartDate != -1 ? endlessStatsMap.get(endlessStartDate).level : 0);
    }
    
    public int getEndlessLevelRecord() {
        return endlessLevelRecord;
    }
    
    public void setEndlessLevel(int level) {
        if(level > endlessLevelRecord)
            endlessLevelRecord = level;
        endlessStatsMap.get(endlessStartDate).level = level;
    }
    
    public void addEndlessLevel(int levels) {
        float m = getEndlessMultiplier();
        setEndlessLevel(endlessStatsMap.get(endlessStartDate).level + levels);
        if(getEndlessLevel() == 10) {
            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer())
                    , Parkour.getInstance().getChatPrefix()
                    + ChatColor.GREEN + " You gained "
                    + ChatColor.GOLD + "25"
                    + ChatColor.GREEN + " points for reaching level 10!");
        }
        if(getEndlessMultiplier() > m) {
            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer())
                    , Parkour.getInstance().getChatPrefix()
                    + ChatColor.GREEN + " Your "
                    + ChatColor.WHITE + "Endless"
                    + ChatColor.GREEN + " multiplier is now at "
                    + ChatColor.GOLD + "x" + getEndlessMultiplier());
        }
    }
    
    public int getTotalEndlessFalls() {
        int falls = 0;
        for(Map.Entry<Integer, EndlessStats> stats : endlessStatsMap.entrySet()) {
            falls += stats.getValue().falls;
        }
        return falls;
    }
    
    public int getEndlessFalls() {
        return endlessStatsMap.get(endlessStartDate).falls;
    }
    
    public void addEndlessFalls(int falls) {
        endlessStatsMap.get(endlessStartDate).falls += falls;
    }
    
    public String getEndlessTimeFormatted() {
        long time = 0;
        for(Map.Entry<Integer, EndlessStats> stats : endlessStatsMap.entrySet()) {
            time += stats.getValue().timePlayed;
        }
        return DurationFormatUtils.formatDuration((time), "H:mm:ss", true);
    }
    
    public long getEndlessTime() {
        return endlessStatsMap.get(endlessStartDate).timePlayed;
    }
    
    public void addEndlessTime(float time) {
        endlessStatsMap.get(endlessStartDate).timePlayed += time;
    }
    
    @DBLoad(fieldName = "endless")
    private void loadEndlessStats(List<Document> endlessStats) {
        Document docDate;
        int year, month, day;
        SimpleDate date;
        int level, falls;
        long timePlayed;
        for(Document doc : endlessStats) {
            try {
                docDate = doc.get("date", Document.class);
                year = docDate.get("year", Integer.class);
                month = docDate.get("month", Integer.class);
                day = docDate.get("day", Integer.class);
                date = new SimpleDate(year, month, day);
                level = doc.get("level", Integer.class);
                timePlayed = doc.get("timePlayed", Long.class);
                falls = doc.get("falls", Integer.class);
                endlessStatsMap.put(date.asInt(), new EndlessStats(date, level, falls, timePlayed));
                if(level > endlessLevelRecord)
                    endlessLevelRecord = level;
            } catch(Exception e) {
                System.out.println("Failed to load endless data:\n" + doc);
                e.printStackTrace();
            }
        }
    }
    
    @DBSave(fieldName = "endless")
    private List<Document> saveEndlessStats() {
        List<Document> documents = new ArrayList<>();
        Document docDate;
        for(Map.Entry<Integer, EndlessStats> stats : endlessStatsMap.entrySet()) {
            docDate = new Document("year", stats.getValue().date.year).append("month", stats.getValue().date.month).append("day", stats.getValue().date.day);
            documents.add(new Document("date", docDate).append("level", stats.getValue().level).append("timePlayed", stats.getValue().timePlayed).append("falls", stats.getValue().falls));
        }
        return documents;
    }

    @DBSave(fieldName = "visitedArenas")
    private List<Document> saveVisitedArenas() {
        List<Document> arenaNames = new ArrayList<>();
        for (Arena arena : visitedArenas) {
            arenaNames.add(new Document("name", arena.getName()).append("mode", arena.getParkourMode().name()));
        }
        return arenaNames;
    }

    @DBLoad(fieldName = "visitedArenas")
    private void loadVisitedArenas(List<Document> arenas) {
        for (Document arenaDoc : arenas) {
            try {
                ParkourMode mode = ParkourMode.valueOf(arenaDoc.get("mode", String.class));
                Arena arena = Arena.byName(arenaDoc.get("name", String.class), mode);
                if (arena != null) {
                    visitedArenas.add(arena);
                }
            } catch(IllegalArgumentException e) {}
        }
    }

    public Set<Arena> getVisitedArenas() {
        return visitedArenas;
    }

    public void setRequestingEndgame(boolean requestingEndgame) {
        this.requestingEndgame = requestingEndgame;
    }

    public boolean isRequestingEndgame() {
        return requestingEndgame;
    }

    public void setIngame(boolean ingame) {
        this.ingame = ingame;
    }

    public boolean isIngame() {
        return ingame;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public ParkourBattle<?> getCurrentBattle() {
        if(this.parkourMode == ParkourMode.NONE) return null;
        return (ParkourBattle<?>) Parkour.getInstance().getBattleManager(this.parkourMode).getBattle(this);
    }
    
    @Override
    public void setDefaults() {
        super.setDefaults();
        this.frozen = false;
        this.ingame = false;
    }
    
    public class EndlessStats {
        public SimpleDate date;
        public int level;
        public int falls;
        public long timePlayed; // Kept in millis
        
        public EndlessStats() {
            date = new SimpleDate();
            level = 1;
            falls = 0;
            timePlayed = 0;
        }
        
        public EndlessStats(SimpleDate date, int level, int falls, long timePlayed) {
            this.date = date;
            this.level = level;
            this.falls = falls;
            this.timePlayed = timePlayed;
        }
    }
}

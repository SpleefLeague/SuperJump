/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.player;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.player.Rank;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.entitybuilder.DBEntity;
import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.DBLoadable;
import com.spleefleague.entitybuilder.DBSave;
import com.spleefleague.entitybuilder.DBSaveable;
import com.spleefleague.gameapi.player.RatedPlayer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.ParkourBattle;
import com.spleefleague.parkour.game.Arena;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.game.conquest.ConquestParkourArena;
import com.spleefleague.parkour.game.versus.classic.VersusClassicParkourArena;
import com.spleefleague.parkour.variable.SimpleDate;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 *
 * @author Jonas, NickM13
 */
public class ParkourPlayer extends RatedPlayer<ParkourMode> {
    
    private boolean ingame, frozen, requestingEndgame;
    private Set<Arena> visitedArenas;
    private ParkourMode parkourMode;
    private List<Arena> lastQueueArenas; // For requeueing
    private Player spectatorTarget;
    private int spectatorLoading = 0;
    private BukkitTask clockRequeue;
    
    // New elo system
    @DBSave(fieldName = "SJPoints")
    @DBLoad(fieldName = "SJPoints")
    private int points;
    @DBSave(fieldName = "SJParagon")
    @DBLoad(fieldName = "SJParagon")
    private int paragon;
    //@DBSave(fieldName = "SJTempMultiplier")
    //@DBLoad(fieldName = "SJTempMultiplier")
    //private TempMultiplier tempMultiplier;
    //@DBSave(fieldName = "SJPermMultiplier")
    //@DBLoad(fieldName = "SJPermMultiplier")
    //private long permMultiplier;
    
    // Currency
    @DBSave(fieldName = "SJParagonCoins")
    @DBLoad(fieldName = "SJParagonCoins")
    private int paragonPoints;
    
    // Conquest
    private Set<String> conquestPacksAvailable;
    private Map<String, Long> conquestScores;
    
    // Endless
    private int endlessStartDate; // When starting a match, stores the date for indexing
    private Map<Integer, EndlessStats> endlessStatsMap;
    private int endlessLevelRecord;
    
    // Versus
    private int versusWins;
    private int versusLosses;
    private Map<String, Long> versusScores;
    private Set<Arena> versusRandomBlacklist;
    private boolean randomQueued; // Awards bonus multiplier
    private boolean movedLastMatch;
    
    public ParkourPlayer() {
        super(ParkourMode.class, Parkour.getInstance().getPluginDB().getCollection("Players"));
        this.visitedArenas = new HashSet<>();
        this.parkourMode = ParkourMode.NONE;
        this.conquestScores = new HashMap<>();
        this.conquestPacksAvailable = new HashSet<>();
        this.endlessStatsMap = new HashMap<>();
        this.endlessStartDate = -1;
        this.endlessLevelRecord = 0;
        this.versusWins = this.versusLosses = 0;
        this.versusScores = new HashMap<>();
        this.versusRandomBlacklist = new HashSet<>();
        this.lastQueueArenas = new ArrayList<>();
        setDefaults();
    }
    
    public void setMovedInMatch(boolean state) {
        movedLastMatch = state;
    }
    
    public boolean getMovedLastMatch() {
        return movedLastMatch;
    }
    
    public void setSpectatorLoading(boolean loading) {
        spectatorLoading += loading ? 1 : -1;
    }
    
    public boolean isSpectatorLoading() {
        return spectatorLoading > 0;
    }
    
    public void setParkourSpectatorTarget(Player sjp) {
        spectatorTarget = sjp;
    }
    
    public Player getParkourSpectatorTarget() {
        return spectatorTarget;
    }
    
    public void focusParkourSpectatorTarget() {
        if (!getGameMode().equals(GameMode.SPECTATOR)) return;
        if (spectatorTarget == null) {
            if (getSpectatorTarget() != null) {
                spectatorLoading = 1;
                setSpectatorTarget(null);
            }
        }
        else if (!isSpectatorLoading()) {
            new BukkitRunnable() {
                int runtime = 0;
                @Override
                public void run() {
                    Player target = spectatorTarget;
                    if (target == null) {
                        spectatorLoading = 1;
                        setSpectatorTarget(null);
                        this.cancel();
                    }
                    else {
                        teleport(target);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                spectatorLoading = 1;
                                setSpectatorTarget(target);
                                if (getPlayer().getSpectatorTarget() != null) {
                                    this.cancel();
                                }
                                else {
                                    runtime++;
                                    if (runtime > 100) {
                                        this.cancel();
                                    }
                                }
                            }
                        }.runTaskLater(Parkour.getInstance(), 2);
                    }
                }
            }.runTaskTimer(Parkour.getInstance(), 1, 3);
        }
    }
    
    public void nextParkourSpectatorTarget() {
        if (spectatorTarget != null) {
            ParkourBattle battle = Parkour.getInstance().getPlayerManager().get(spectatorTarget).getCurrentBattle();
            int count = battle.getActivePlayers().size();
            if(count > 1) {
                for(int i = 0; i < count; i++) {
                    if (spectatorTarget.getName().equals(((ParkourPlayer) battle.getActivePlayers().get(i)).getName())) {
                        if (i < count - 1) {
                            setParkourSpectatorTarget(((ParkourPlayer) battle.getActivePlayers().get(i + 1)).getPlayer());
                            focusParkourSpectatorTarget();
                        }
                        else {
                            setParkourSpectatorTarget(((ParkourPlayer) battle.getActivePlayers().get(0)).getPlayer());
                            focusParkourSpectatorTarget();
                        }
                        break;
                    }
                }
            }
        }
    }
    
    public void addPoints(int points) {
        addPoints(points, "");
    }
    
    public void addPoints(int points, String info) {
        this.points += points;
        ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()), Parkour.getInstance().getChatPrefix()
                + Parkour.fillColor + " You gained "
                + Parkour.pointColor + points
                + Parkour.fillColor + (points == 1 ? " point" : " points")
                + info
                + Parkour.fillColor + "!");
        while(this.points >= 5000) {
            if (this.paragon < 30) {
                this.points -= 5000;
                this.paragon++;
                ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(),
                          Parkour.playerColor + this.getName()
                        + Parkour.fillColor + " is now Paragon "
                        + Parkour.pointColor + this.paragon
                        + Parkour.fillColor + "!",
                        Parkour.getInstance().getEndMessageChannel());
            }
        }
    }
    
    public void addParagonPoints(int points, String info) {
        this.paragonPoints++;
            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()), Parkour.getInstance().getChatPrefix()
                    + Parkour.fillColor + " You gained "
                    + Parkour.pointColor + points
                    + Parkour.fillColor + " Paragon " + (points == 1 ? "Point" : "Points")
                    + info
                    + Parkour.fillColor + "!");
    }
    
    public boolean buyWithParagonPoints(int points) {
        if(this.paragonPoints >= points) {
            this.paragonPoints -= points;
            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()), Parkour.getInstance().getChatPrefix()
                    + Parkour.fillColor + " You spent "
                    + Parkour.pointColor + points
                    + Parkour.fillColor + " Paragon Points, "
                    + Parkour.pointColor + this.paragonPoints
                    + Parkour.fillColor + " remaining.");
            SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()).setCoins(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()).getCoins() + 50);
            return true;
        }
        else {
            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()), Parkour.getInstance().getChatPrefix()
                    + Parkour.fillColor + " You don't have enough Paragon Points. "
                    + Parkour.pointColor + this.paragonPoints
                    + Parkour.fillColor + " out of "
                    + Parkour.pointColor + points);
            return false;
        }
    }
    
    public void removeParagonPoints(int points) {
        this.paragonPoints -= points;
    }
    
    public int getTotalPoints() {
        return (this.points + this.paragon * 5000);
    }
    
    public int getPoints() {
        return this.points;
    }
    
    public int getParagonLevel() {
        return this.paragon;
    }
    
    public int getParagonPoints() {
        return this.paragonPoints;
    }
    
    public void addVersusWin() {
        this.versusWins++;
    }
    
    public void addVersusLoss() {
        this.versusLosses++;
    }
    
    public int getVersusWins() {
        return this.versusWins;
    }
    
    public int getVersusLosses() {
        return this.versusLosses;
    }
    
    public long getVersusClassicScore(String arena) {
        if(versusScores.get(arena) == null) {
            return 0;
        }
        return versusScores.get(arena);
    }
    
    public String getVersusClassicScoreFormatted(String arena) {
        if(versusScores.get(arena) == null) {
            return "N/A";
        }
        return String.format("%.2f", versusScores.get(arena) / 20.f) + Parkour.fillColor + " seconds";
    }
    
    public void checkVersusClassicScore(VersusClassicParkourArena arena, long time) {
        if(!versusScores.containsKey(arena.getName())) {
            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()), Parkour.getInstance().getChatPrefix()
                    + Parkour.fillColor + " You set a new record of "
                    + ChatColor.YELLOW + String.format("%.2f", time / 20.f)
                    + Parkour.fillColor + " seconds!");
            versusScores.put(arena.getName(), time);
            arena.setHighscore(getName(), time);
        }
        else {
            if(versusScores.get(arena.getName()) > time) {
                ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()), Parkour.getInstance().getChatPrefix()
                        + Parkour.fillColor + " You set a new record of "
                        + ChatColor.YELLOW + String.format("%.2f", time / 20.f)
                        + Parkour.fillColor + " seconds!");
                ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()), Parkour.getInstance().getChatPrefix()
                        + Parkour.fillColor + " You beat your previous record of "
                        + ChatColor.YELLOW + String.format("%.2f", versusScores.get(arena.getName()) / 20.f)
                        + Parkour.fillColor + " seconds.");
                versusScores.put(arena.getName(), time);
                arena.setHighscore(getName(), time);
            }
        }
    }
    
    public long getConquestScore(String arena) {
        if(conquestScores.get(arena) == null) {
            return 0;
        }
        return conquestScores.get(arena);
    }
    
    public String getConquestScoreFormatted(String arena) {
        if(conquestScores.get(arena) == null) {
            return "N/A";
        }
        return String.format("%.2f", conquestScores.get(arena) / 20.f) + ChatColor.GRAY + " seconds";
    }
    
    public String getConquestStarsFormatted(ConquestParkourArena arena) {
        String stars = "";
        if(conquestScores.containsKey(arena.getName())) {
            for(int i = 0; i < 3; i++) {
                if(i < arena.getStars(conquestScores.get(arena.getName()))) {
                    stars += ChatColor.GOLD + "✫";
                }
                else {
                    stars += ChatColor.GRAY + "✫";
                }
            }
        }
        else {
            stars = ChatColor.GRAY + "✫✫✫";
        }
        return stars;
    }
    
    public void checkConquestScore(ConquestParkourArena arena, long time) {
        int earned = 0;
        ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()), Parkour.getInstance().getChatPrefix()
                + Parkour.fillColor + " You have completed "
                + Parkour.arenaColor + arena.getName()
                + Parkour.fillColor + " in "
                + Parkour.pointColor + String.format("%.2f", time / 20.f)
                + Parkour.fillColor + " seconds!");
        if(!conquestScores.containsKey(arena.getName())) {
            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()), Parkour.getInstance().getChatPrefix()
                    + Parkour.fillColor + " You set a new personal record!");
            earned = arena.getStars(time);
            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()), Parkour.getInstance().getChatPrefix()
                    + Parkour.fillColor + " You earned "
                    + Parkour.pointColor + earned
                    + Parkour.fillColor + " star" + (arena.getStars(time) != 1 ? "s" : "") + ".");
            conquestScores.put(arena.getName(), time);
            arena.setHighscore(getName(), time);
        }
        else {
            if(conquestScores.get(arena.getName()) > time) {
                ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()), Parkour.getInstance().getChatPrefix()
                        + Parkour.fillColor + " You beat your previous record of "
                        + Parkour.pointColor + String.format("%.2f", conquestScores.get(arena.getName()) / 20.f)
                        + Parkour.fillColor + " seconds.");
                earned = arena.getStars(time) - arena.getStars(conquestScores.get(arena.getName()));
                if(earned != 0) {
                    ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer()), Parkour.getInstance().getChatPrefix()
                            + Parkour.fillColor + " You earned "
                            + Parkour.pointColor + earned
                            + Parkour.fillColor + " star" + (earned != 1 ? "s" : "") + "!");
                }
                conquestScores.put(arena.getName(), time);
                arena.setHighscore(getName(), time);
            }
        }
        if(earned != 0) {
            this.addPoints(earned * 20, Parkour.fillColor + " for earning " + Parkour.pointColor + earned + Parkour.fillColor + " star" + (earned != 1 ? "s" : ""));
        }
    }
    
    public float getEndlessMultiplier() {
        return (Math.floorDiv(Math.min(getEndlessLevel(), 100), 20) * 0.05f);
    }
    
    public float getParagonMultiplier() {
        return (paragon * 0.1f);
    }
    
    public float getRankMultiplier() {
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer());
        Rank rank = slp.getRank();
        if(slp.isDonor() || slp.isDonorPlus() || slp.isDonorPlusPlus()
                || rank.getName().equals("VIP")) {
            return 0.2f;
        }
        return 0.0f;
    }
    
    public float getRandomQueueMultiplier() {
        return (randomQueued ? 0.4f : 0.0f);
    }
    
    public float getPointMultiplier() {
        return 1.0f + (getEndlessMultiplier() + getParagonMultiplier() + getRankMultiplier() + getRandomQueueMultiplier());
    }
    
    public void setParkourMode(ParkourMode mode) {
        parkourMode = mode;
    }
    
    public ParkourMode getParkourMode() {
        return parkourMode;
    }
    
    public void setRandomQueued(boolean random) {
        randomQueued = random;
    }
    
    public boolean isRandomQueued() {
        return randomQueued;
    }
    
    public int getConquestStarsPack(ConquestParkourArena.Pack pack) {
        if(pack == null || pack.getArenas() == null) return 0;
        int stars = 0;
        for(String arena : pack.getArenas()) {
            if(conquestScores.containsKey(arena)) {
                stars += ConquestParkourArena.byName(arena).getStars(conquestScores.get(arena));
            }
        }
        return stars;
    }
    
    // Returns number of arenas that have been played
    public int getConquestPlayedPack(ConquestParkourArena.Pack pack) {
        if(pack == null || pack.getArenas() == null) return 0;
        int played = 0;
        for(String arena : pack.getArenas()) {
            if(conquestScores.containsKey(arena) && ConquestParkourArena.byName(arena).getStars(conquestScores.get(arena)) != 0) {
                played++;
            }
        }
        return played;
    }
    
    public int getConquestStarsTotal() {
        int stars = 0;
        for(Map.Entry<String, Long> arena : this.conquestScores.entrySet()) {
            if (ConquestParkourArena.byName(arena.getKey()) != null) {
                stars += ConquestParkourArena.byName(arena.getKey()).getStars(arena.getValue());
            }
        }
        return stars;
    }
    
    public void addConquestPack(ConquestParkourArena.Pack pack) {
        conquestPacksAvailable.add(pack.getName());
    }
    
    public boolean hasConquestPack(ConquestParkourArena.Pack pack) {
        return (pack.isDefault() || conquestPacksAvailable.contains(pack.getName()));
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
        int date = new SimpleDate().asInt();
        return(endlessStatsMap.containsKey(date) ? endlessStatsMap.get(date).level : 0);
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
                    + Parkour.fillColor + " You gained "
                    + Parkour.pointColor + "25"
                    + Parkour.fillColor + " points for reaching level 10!");
        }
        if(getEndlessMultiplier() > m) {
            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(this.getPlayer())
                    , Parkour.getInstance().getChatPrefix()
                    + Parkour.fillColor + " Your "
                    + Parkour.arenaColor + "Endless"
                    + Parkour.fillColor + " multiplier is now at "
                    + Parkour.pointColor + "x" + getEndlessMultiplier());
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
    
    public int getEndlessLevelsTotal() {
        int totalLevels = 0;
        for(Map.Entry<Integer, EndlessStats> entry : endlessStatsMap.entrySet()) {
            totalLevels += entry.getValue().level - 1;
        }
        return totalLevels;
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
    
    public String getOpponentFormatted() {
        String opponents = "";
        if (isIngame()) {
            ParkourBattle battle = getCurrentBattle();
            for (ParkourPlayer sjp : (List<ParkourPlayer>) battle.getActivePlayers()) {
                if (sjp != this) {
                    if (opponents.length() > 1) {
                        opponents += Parkour.fillColor + ", ";
                    }
                    opponents += Parkour.playerColor + sjp.getName();
                }
            }
        }
        return opponents;
    }
    
    public void clearLastArenas() {
        this.lastQueueArenas.clear();
    }
    
    public void addLastArena(Arena arena) {
        this.lastQueueArenas.add(arena);
    }
    
    public List<Arena> getLastArenas() {
        return this.lastQueueArenas;
    }
    
    public void addLastArenas(List<Arena> arenas) {
        arenas.forEach(arena -> this.lastQueueArenas.add(arena));
    }
    
    private void requeue() {
        if (getParkourMode() != ParkourMode.REQUEUE) return;
        Parkour.getInstance().dequeue(this);
        if (isRandomQueued()) {
            Parkour.getInstance().queuePlayerRandom(this);
        }
        else {
            for (Arena arena : lastQueueArenas) {
                Parkour.getInstance().queuePlayer(Parkour.getInstance().getPlayerManager().get(getName()), arena, true);
            }
        }
    }
    
    public void startRequeue() {
        if (getParkourMode() == ParkourMode.NONE) {
            setParkourMode(ParkourMode.REQUEUE);
            if (clockRequeue != null) {
                clockRequeue.cancel();
            }
            clockRequeue = new BukkitRunnable() {
                @Override
                public void run() {
                    requeue();
                }
            }.runTaskLater(Parkour.getInstance(), 100);
        }
    }
    
    public void stopRequeue() {
        if (getParkourMode() == ParkourMode.REQUEUE) {
            setParkourMode(ParkourMode.NONE);
            clockRequeue.cancel();
        }
    }
    
    @DBLoad(fieldName = "conquest")
    private void loadConquestStats(Document conquestStats) {
        conquestStats.forEach((arena, time) -> {
            conquestScores.put(arena, (long) time);
        });
    }
    
    @DBSave(fieldName = "conquest")
    private Document saveConquestStats() {
        Document doc = new Document();
        conquestScores.forEach((arena, time) -> {
            doc.append(arena, time);
        });
        return doc;
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
    
    @DBLoad(fieldName = "versus")
    private void loadVersusStats(Document versusStats) {
        versusWins = versusStats.getInteger("wins");
        versusLosses = versusStats.getInteger("losses");
        versusStats.remove("wins");
        versusStats.remove("losses");
        versusStats.forEach((arena, time) -> {
            versusScores.put(arena, (long) time);
        });
    }
    
    @DBSave(fieldName = "versus")
    private Document saveVersusStats() {
        Document doc = new Document();
        doc.append("wins", getVersusWins()).append("losses", getVersusLosses());
        versusScores.forEach((arena, time) -> {
            doc.append(arena, time);
        });
        return doc;
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
    
    @DBSave(fieldName = "versusRandomBlaclist")
    private List<Document> saveVersusRandomBlacklist() {
        List<Document> arenaNames = new ArrayList<>();
        for (Arena arena : versusRandomBlacklist) {
            arenaNames.add(new Document("name", arena.getName()).append("mode", arena.getParkourMode().name()));
        }
        return arenaNames;
    }
    
    @DBLoad(fieldName = "versusRandomBlacklist")
    private void loadVersusRandomBlacklist(List<Document> blacklist) {
        for (Document arenaDoc : blacklist) {
            try {
                ParkourMode mode = ParkourMode.valueOf(arenaDoc.get("mode", String.class));
                Arena arena = Arena.byName(arenaDoc.get("name", String.class), mode);
                if (arena != null) {
                    versusRandomBlacklist.add(arena);
                }
            } catch(IllegalArgumentException e) {}
        }
    }
    
    public Set<Arena> getVersusRandomBlacklist() {
        return versusRandomBlacklist;
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
    
    // Resets player database stats
    public void resetDB() {
        MongoCollection<Document> col = Parkour.getInstance().getPluginDB().getCollection("Players");
        col.deleteMany(new BasicDBObject().append("username", this.getName()));
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

class TempMultiplier extends DBEntity implements DBLoadable, DBSaveable {
    
    @DBSave(fieldName = "multiplier")
    int multiplier;
    @DBSave(fieldName = "expireTime")
    long expireTime;

    public TempMultiplier(int multiplier, long expireTime) {
        this.multiplier = multiplier;
        this.expireTime = expireTime;
    }
}

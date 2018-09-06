/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game.memory;

import com.google.common.collect.Lists;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.chat.Theme;
import com.spleefleague.core.player.PlayerState;
import com.spleefleague.core.player.Rank;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.gameapi.events.BattleEndEvent;
import com.spleefleague.gameapi.events.BattleStartEvent.StartReason;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.ParkourBattle;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.player.ParkourPlayer;
import com.spleefleague.parkour.variable.SimpleDate;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

/**
 *
 * @author jonas
 */
public class MemoryParkourBattle extends ParkourBattle<MemoryParkourArena>{

    private int levelStart;
    private int ticksPassedTotal;
    private long periodResetStart; // Stores period reset at start, then subtracts ticks passed
    
    protected MemoryParkourBattle(MemoryParkourArena arena, List<ParkourPlayer> players) {
        super(arena, players);
    }
    
    @Override
    protected void applyRatingChange(ParkourPlayer winner) {
        
    }

    @Override
    protected void addToBattleManager() {
        Parkour.getInstance().getBattleManager(ParkourMode.ENDLESS).add(this);
    }

    @Override
    protected void removeFromBattleManager() {
        Parkour.getInstance().getBattleManager(ParkourMode.ENDLESS).remove(this);
    }
    
    public int getLevelStart() {
        return levelStart;
    }
    
    public int getLevel() {
        if (players.size() > 0 && players.get(0) != null) {
            return players.get(0).getEndlessLevel();
        }
        else {
            return 0;
        }
    }
    
    @Override
    protected void updateScoreboardTime() {
        if (scoreboard == null) return;
        scoreboard.getObjectives().forEach(o->{o.unregister();});
        String sessionTime = DurationFormatUtils.formatDuration(ticksPassed * 50, "s", true) + " s";
        String resetTime = DurationFormatUtils.formatDuration(SimpleDate.getNextDayMillis() - (1000 * 60 * 5), "H:mm:ss", true);
        Objective sidebar = scoreboard.registerNewObjective("sidebar", "dummy");
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
        sidebar.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Endless");
        sidebar.getScore(ChatColor.BOLD + "Today").setScore(10);
        String post = "";
        int place = MemoryParkourArena.getLeaderboardPlacement(players.get(0));
        if (place % 100 >= 10 && place % 100 <= 19) {
            post = "th";
        } else {
            switch(place % 10) {
                case 1: post = "st"; break;
                case 2: post = "nd"; break;
                case 3: post = "rd"; break;
                default: post = "th"; break;
            }
        }
        sidebar.getScore(" Personal: " + ChatColor.GOLD + players.get(0).getEndlessLevel() 
                + ChatColor.GRAY + " [" + place + post + "]").setScore(9);
        sidebar.getScore(" Server: " + ChatColor.GOLD + arena.getHighscoreToday().getTopPlayer().score + ChatColor.GRAY + " [" 
                + arena.getHighscoreToday().getTopPlayer().player + ChatColor.GRAY + "] ").setScore(8);
        sidebar.getScore(ChatColor.BOLD + " ").setScore(6);
        sidebar.getScore(ChatColor.BOLD + "Record").setScore(5);
        sidebar.getScore(" Personal: " + ChatColor.GOLD + players.get(0).getEndlessLevelRecord()).setScore(4);
        sidebar.getScore(" Server: " + ChatColor.GOLD + arena.getHighscoreAlltime().score + ChatColor.GRAY + " [" 
                + arena.getHighscoreAlltime().player + ChatColor.GRAY + "]").setScore(3);
        sidebar.getScore(ChatColor.BOLD + "  ").setScore(2);
        sidebar.getScore(ChatColor.BOLD + "Time").setScore(1);
        sidebar.getScore(" Reset In: " + ChatColor.GOLD + resetTime).setScore(0);
        
        /*
        Objective playerlist = scoreboard.registerNewObjective("playerlist", "dummy");
        playerlist.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        playerlist.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "--=[ENDLESS]=--");
        SpleefLeague.getInstance().getPlayerManager().getAll().forEach(slp -> {
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getPlayer());
            if(sjp != null) {
                playerlist.getScore(sjp.getName()).setScore(sjp.getEndlessLevel());
            }
            players.get(0).hidePlayer(slp.getPlayer());
        });
        */
        
        //applyScoreboard();
    }
    
    @Override
    protected void updateExperienceScore() {
        getActivePlayers().forEach(slp -> {
            int pointsGained = 0;
            if (getLevel() > 10 && getLevel() > getLevelStart()) {
                pointsGained += getLevel() - Math.max(getLevelStart(), 10);
            }
            if (getLevel() >= 10 && getLevelStart() < 10) {
                pointsGained += 25;
            }
            slp.setLevel(pointsGained);
            slp.setExp(slp.getEndlessMultiplier() / 0.25f);
        });
    }
    
    @Override
    public void start(StartReason reason) {
        super.start(reason);
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(players.get(0));
        if (players.get(0).getEndlessTime() == 0) {
            ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                    + Parkour.arenaColor + " Endless"
                    + Parkour.fillColor + " has reset since last time you played.");
        }
        levelStart = players.get(0).getEndlessLevel();
        periodResetStart = TimeUnit.HOURS.toMillis(Calendar.getInstance(SimpleDate.getTimeZone()).get(Calendar.HOUR))
                + TimeUnit.MINUTES.toMillis(Calendar.getInstance(SimpleDate.getTimeZone()).get(Calendar.MINUTE))
                + TimeUnit.SECONDS.toMillis(Calendar.getInstance(SimpleDate.getTimeZone()).get(Calendar.SECOND));
        periodResetStart = (TimeUnit.DAYS.toMillis(1) - periodResetStart) / 50;
        MemoryParkourArena.initPlayer(players.get(0));
    }
    
    @Override
    public void announceStart() {
        ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(players.get(0)), Parkour.getInstance().getChatPrefix()
                + Parkour.fillColor + " You have started playing "
                + Parkour.arenaColor + "Endless"
                + Parkour.fillColor + "!");
    }
    
    @Override
    public void createScoreboard() {
        updateScoreboardTime();
        ParkourPlayer sjp = players.get(0);
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer());
        slp.addChatChannel(cc);
        slp.setState(PlayerState.INGAME);
        sjp.setIngame(true);
        sjp.setFrozen(true);
        ParkourBattle.PlayerData pdata = new ParkourBattle.PlayerData(sjp, arena.getSpawns()[0], arena.getGoals()[0]);
        this.data.putIfAbsent(sjp, pdata);
    }
    
    @Override
    public void end(ParkourPlayer winner, BattleEndEvent.EndReason reason) {
        ParkourPlayer sjp = players.get(0);
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(players.get(0));
        sjp.addEndlessFalls(data.get(sjp).getFalls());
        data.get(sjp).setFalls(0);
        float levelTime = Math.floorDiv((System.currentTimeMillis() - timeLastLap), 10) / 100.f;
        sjp.addEndlessTime(System.currentTimeMillis() - timeLastLap);
        ticksPassedTotal += ticksPassed;
        ticksPassed = 0;
        timeLastLap = System.currentTimeMillis();
        if (reason == BattleEndEvent.EndReason.NORMAL) {
            if (levelTime < arena.getBanTime() && !slp.getRank().hasPermission(Rank.DEVELOPER)) {
                slp.performBan(null,
                        "Console",
                        "Cheating on " + this.arena.getName());
            }
            String completeMessage;
            if (levelTime < 30)      completeMessage = "" + ChatColor.GREEN;
            else if (levelTime < 60) completeMessage = "" + ChatColor.YELLOW;
            else                     completeMessage = "" + ChatColor.RED;
            completeMessage += "" + String.format("%.2f", levelTime) + " Seconds";
            ChatManager.sendTitle(ChatColor.GREEN + "Completed In", completeMessage, 5, 20, 5, cc);
            sjp.addEndlessLevel(1);
            Location spawn = arena.getSpawns()[0];
            sjp.teleport(new Location(spawn.getWorld(),
                    spawn.getX(), spawn.getY(), spawn.getZ(),
                    winner.getLocation().getYaw(), winner.getLocation().getPitch()));
            focusSpectators();
            arena = arena.nextLevel(arena, this);
            this.setGoal(sjp, arena.getGoals()[0]);
            arena.checkHighscore(sjp);
            updateScoreboardTime();
            updateExperienceScore();
        }
        else {
            if (reason == BattleEndEvent.EndReason.CANCEL) {
                ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(), Theme.INCOGNITO.buildTheme(false) + "The battle has been cancelled by a moderator.", cc);
            } else if (reason == BattleEndEvent.EndReason.ENDGAME) {
                ChatManager.sendMessagePlayer(slp, Theme.SUCCESS.buildTheme(false) 
                        + Parkour.getInstance().getChatPrefix()
                        + Parkour.fillColor + " You played "
                        + Parkour.arenaColor + "Endless"
                        + Parkour.fillColor + " for "
                        + Parkour.pointColor + DurationFormatUtils.formatDuration((long) (Math.max(ticksPassedTotal, 0) * 50), "H:mm:ss", true)
                        + Parkour.fillColor + ", beating "
                        + Parkour.pointColor + (getLevel() - getLevelStart())
                        + Parkour.fillColor + " levels!");
            }
            int pointsGained = 0;
            int coinsGained = getLevel() - getLevelStart();
            String info = "";
            if (getLevel() > 10 && getLevel() > getLevelStart()) {
                pointsGained += getLevel() - Math.max(getLevelStart(), 10);
            }
            if (getLevel() >= 10 && getLevelStart() < 10) {
                info +=   Parkour.fillColor + " ("
                        + Parkour.pointColor + "25"
                        + Parkour.fillColor + " points for reaching "
                        + Parkour.pointColor + "10"
                        + Parkour.fillColor + ")";
                pointsGained += 25;
            }
            if (pointsGained > 0) {
                getPlayers().get(0).addPoints(pointsGained, info);
            }
            if (coinsGained > 0) {
                SpleefLeague.getInstance().getPlayerManager().get(getPlayers().get(0).getPlayer()).addCoins(coinsGained, "");
            }
            try {
                EntityBuilder.save(sjp, Parkour.getInstance().getPluginDB().getCollection("Players"));
            } catch(Exception e) {
                Parkour.LOG.log(Level.WARNING, "Could not save ParkourPlayer!");
                Document doc = EntityBuilder.serialize(sjp).get("$set", Document.class);
                Parkour.LOG.log(Level.WARNING, doc.toJson());
                e.printStackTrace();
            }
            Lists.newArrayList(super.getSpectators()).forEach(super::resetPlayer);
            Lists.newArrayList(super.getActivePlayers()).forEach(super::resetPlayer);
            Bukkit.getPluginManager().callEvent(new BattleEndEvent(this, reason));
            cleanup();
            this.removeFromBattleManager();
        }
    }
}

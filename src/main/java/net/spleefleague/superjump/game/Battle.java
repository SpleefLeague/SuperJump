/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import net.spleefleague.core.SpleefLeague;
import net.spleefleague.core.chat.ChatManager;
import net.spleefleague.core.chat.Theme;
import net.spleefleague.core.player.PlayerState;
import net.spleefleague.core.utils.Area;
import net.spleefleague.superjump.SuperJump;
import net.spleefleague.superjump.player.SJPlayer;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 *
 * @author Jonas
 */
public class Battle {
    
    private final Arena arena;
    private final List<SJPlayer> players; //MIGHT CONTAIN PLAYERS WHICH LEFT THE GAME. USE getActivePlayers() FOR ACTIVE PLAYERS INSTEAD
    private final HashMap<SJPlayer, PlayerData> data;
    private int ticksPassed = 0;
    private BukkitRunnable clock;
    private Scoreboard scoreboard;
    private boolean inCountdown;
    
    protected Battle(Arena arena, List<SJPlayer> players) {
        this.arena = arena;
        this.players = players;
        this.data = new HashMap<>();
    }
    
    public Arena getArena() {
        return arena;
    }
    
    public Collection<SJPlayer> getPlayers() {
        return players;
    }
    
    public Location getSpawn(SJPlayer sjp) {
        return data.get(sjp).getSpawn();
    }
    
    public Area getGoal(SJPlayer sjp) {
        return data.get(sjp).getGoal();
    }
    
    public void removePlayer(SJPlayer sjp) {
        resetPlayer(sjp);
        ArrayList<SJPlayer> activePlayers = getActivePlayers();
        if(activePlayers.size() == 1) {
            end(players.get(0));
        }
        else if(activePlayers.size() > 1) {   
            for(SJPlayer pl : activePlayers) {
                pl.getPlayer().sendMessage(SuperJump.getInstance().getPrefix() + " " + Theme.ERROR.buildTheme(false) + sjp.getName() + " has left the game!");
            }
        }
    }
    
    public ArrayList<SJPlayer> getActivePlayers() {
        ArrayList<SJPlayer> active = new ArrayList<>();
        for(SJPlayer sjp : players) {
            if(sjp.isIngame()) {
                active.add(sjp);
            }
        }
        return active;
    }
    
    public void start() {
        SuperJump.getInstance().getBattleManager().add(this);
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("rounds", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.GRAY + "0:0:0 | " + ChatColor.RED + "Times Fallen:");
        for(int i = 0; i < players.size(); i++) {
            SJPlayer sjp = players.get(i);
            sjp.setIngame(true);
            sjp.setFrozen(true);
            sjp.getPlayer().teleport(arena.getSpawns()[i]);
            this.data.put(sjp, new PlayerData(sjp, arena.getSpawns()[i], arena.getGoals()[i % arena.getGoals().length]));
            sjp.getPlayer().setScoreboard(scoreboard);
            scoreboard.getObjective("rounds").getScore(sjp.getName()).setScore(data.get(sjp).getFalls());
            SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer()).setState(PlayerState.INGAME);
        }
        startCountdown();
    }
    
    private void updateScoreboardTime() {
        if (scoreboard == null) {
            return;
        }
        Objective objective = scoreboard.getObjective("rounds");
        if (objective != null) {
            String s = DurationFormatUtils.formatDuration(ticksPassed * 50, "H:m:s", true);
            objective.setDisplayName(ChatColor.GRAY.toString() + s + " | " + ChatColor.RED + "Times Fallen:");
        }
    }
    
    private void startCountdown() {
        inCountdown = true;
        BukkitRunnable br = new BukkitRunnable() {
            private int secondsLeft = 3;
            @Override
            public void run() {
                if(secondsLeft > 0) {
                    for(SJPlayer sjp : getActivePlayers()) {
                        sjp.getPlayer().sendMessage(SuperJump.getInstance().getChatPrefix() + " " + secondsLeft + "...");
                    }
                    secondsLeft--;
                }
                else {
                    for(SJPlayer sjp : getActivePlayers()) {
                        sjp.getPlayer().sendMessage(SuperJump.getInstance().getChatPrefix() + " GO!");
                        sjp.setFrozen(false);
                    }
                    startClock();
                    inCountdown = false;
                    super.cancel();
                }
            }
            
            private void startClock() {
                clock = new BukkitRunnable() {
                    @Override
                    public void run() {
                        ticksPassed++;
                        updateScoreboardTime();
                    }
                };
                clock.runTaskTimer(SuperJump.getInstance(), 0, 1);
            }
        };
        br.runTaskTimer(SuperJump.getInstance(), 20, 20);
    }
    
    public void cancel() {
        for (SJPlayer sjp : getActivePlayers()) {
            sjp.getPlayer().sendMessage(SuperJump.getInstance().getPrefix() + " " + Theme.INCOGNITO.buildTheme(false) + "Your battle has been cancelled by a moderator.");
            if(sjp.isIngame())
                resetPlayer(sjp);
        }
    }
    
    public void end(SJPlayer winner) {
        end(winner, arena.isRated());
    }
    
    public void end(SJPlayer winner, boolean rated) {
        clock.cancel();
        if(rated) {
            applyRatingChange(winner);
        }
        for(SJPlayer sjp : getActivePlayers()) {
            resetPlayer(sjp);
        }
        SuperJump.getInstance().getBattleManager().remove(this);
    }
    
    private void resetPlayer(SJPlayer sjp) {
        sjp.getPlayer().teleport(SpleefLeague.DEFAULT_WORLD.getSpawnLocation());
        sjp.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        sjp.setIngame(false);
        sjp.setFrozen(false);
        SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer()).setState(PlayerState.IDLE);
    }
    
    private void applyRatingChange(SJPlayer winner) {
        int winnerPoints = 0;
        final int MIN_RATING = 1, MAX_RATING = 20;
        String playerList = "";
        for(SJPlayer sjp : players) {
            if(sjp != winner) {
                float elo = (float) (1f / (1f + Math.pow(2f, ((sjp.getRating() - winner.getRating()) / 400f))));
                int rating = (int) Math.round(MAX_RATING * (1f - elo));
                if (rating < MIN_RATING) {
                    rating = MIN_RATING;
                }
                winnerPoints += rating;
                sjp.setRating(sjp.getRating() - rating);
                playerList += ChatColor.RED + sjp.getName() + ChatColor.WHITE + " (" + sjp.getRating() + ")" + ChatColor.GREEN + " gets " + ChatColor.GRAY + -rating + ChatColor.WHITE + " points. ";
            }
        }
        winner.setRating(winner.getRating() + winnerPoints);
        playerList += ChatColor.RED + winner.getName() + ChatColor.WHITE + " (" + winner.getRating() + ")" + ChatColor.GREEN + " gets " + ChatColor.GRAY + winnerPoints + ChatColor.WHITE + " points. ";
        ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), ChatColor.GREEN + "Game in arena " + ChatColor.WHITE + arena.getName() + ChatColor.GREEN + " is over. " + playerList, "GAME_MESSAGE_JUMP");
    }

    public void onArenaLeave(SJPlayer sjp) {
        if(inCountdown) {
            sjp.getPlayer().teleport(data.get(sjp).getSpawn());
        }
        data.get(sjp).increaseFalls();
        sjp.getPlayer().teleport(data.get(sjp).getSpawn());
        scoreboard.getObjective("rounds").getScore(sjp.getName()).setScore(data.get(sjp).getFalls()); 
    }
    
    private static class PlayerData {
        
        private int falls;
        private final Location spawn;
        private final SJPlayer sjp;
        private final Area goal;
        
        public PlayerData(SJPlayer sjp, Location spawn, Area goal) {
            this.sjp = sjp;
            this.spawn = spawn;
            this.falls = 0;
            this.goal = goal;
        }
        
        public Location getSpawn() {
            return spawn;
        }
        
        public Area getGoal() {
            return goal;
        }
        
        public int getFalls() {
            return falls;
        }
        
        public void increaseFalls() {
            this.falls++;
        }
        
        public SJPlayer getPlayer() {
            return sjp;
        }
    }
}

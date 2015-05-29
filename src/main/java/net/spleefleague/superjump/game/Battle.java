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
import net.spleefleague.core.chat.ChatChannel;
import net.spleefleague.core.chat.ChatManager;
import net.spleefleague.core.chat.Theme;
import net.spleefleague.core.io.EntityBuilder;
import net.spleefleague.core.player.PlayerState;
import net.spleefleague.core.player.Rank;
import net.spleefleague.core.player.SLPlayer;
import net.spleefleague.core.utils.Area;
import net.spleefleague.superjump.SuperJump;
import net.spleefleague.superjump.player.SJPlayer;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
    private final List<SJPlayer> spectators;
    private final ChatChannel cc;
    private int ticksPassed = 0;
    private BukkitRunnable clock;
    private Scoreboard scoreboard;
    private boolean inCountdown;
    private boolean isOver;
    
    protected Battle(Arena arena, List<SJPlayer> players) {
        this.arena = arena;
        this.players = players;
        this.spectators = new ArrayList<>();
        this.data = new HashMap<>();    
        this.cc = new ChatChannel("GAMECHANNEL" + this.hashCode(), "GAMECHANNEL" + this.hashCode(), Rank.DEFAULT, false, true);
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
    
    public void addSpectator(SJPlayer sp) {
        spectators.add(sp);
        Location spawn = arena.getSpectatorSpawn();
        if(spawn == null) {
            int i = 0;
            for(SJPlayer s : getActivePlayers()) {
                i++;
                if(spawn == null) {
                    spawn = s.getPlayer().getLocation();
                }
                else {
                    spawn = spawn.add(s.getPlayer().getLocation());
                }
                spawn.add(0, 1, 0);
            }
            spawn.multiply(1.0 / (double)i);
        }
        sp.getPlayer().teleport(spawn);
        sp.getPlayer().setScoreboard(scoreboard);
        sp.getPlayer().setAllowFlight(true);
        SpleefLeague.getInstance().getPlayerManager().get(sp.getPlayer()).setState(PlayerState.SPECTATING);
    }
    
    public boolean isSpectating(SJPlayer sjp) {
        return spectators.contains(sjp);
    }
    
    public void removeSpectator(SJPlayer sp) {
        resetPlayer(sp);
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
    
    public boolean isOver() {
        return isOver;
    }
    
    public void start() {
        SuperJump.getInstance().getBattleManager().add(this);
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("rounds", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.GRAY + "0:0:0 | " + ChatColor.RED + "Times Fallen:");
        for(int i = 0; i < players.size(); i++) {
            SJPlayer sjp = players.get(i);
            Player p = sjp.getPlayer();
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            sjp.setIngame(true);
            sjp.setFrozen(true);
            this.data.put(sjp, new PlayerData(sjp, arena.getSpawns()[i], arena.getGoals()[i % arena.getGoals().length]));
            p.setGameMode(GameMode.ADVENTURE);
            p.setFlying(false);
            p.setAllowFlight(false);
            p.teleport(arena.getSpawns()[i]);
            p.getInventory().clear();
            p.setScoreboard(scoreboard);
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
                    ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), secondsLeft + "...", cc.getName());
                    secondsLeft--;
                } else {
                    ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), "GO!", cc.getName());
                    for (SJPlayer sjp : getActivePlayers()) {
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
        isOver = true;
        saveGameHistory(null);
        ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), Theme.INCOGNITO.buildTheme(false) + "The battle has been cancelled by a moderator.", cc.getName());
        for (SJPlayer sjp : getActivePlayers()) {
            resetPlayer(sjp);
        }
        for(SJPlayer sjp : spectators) {
            resetPlayer(sjp);
        }
        cleanup();
    }
    
    private void cleanup() {
        clock.cancel();
        SuperJump.getInstance().getBattleManager().remove(this);
        ChatManager.unregisterChannel(cc);
    }
    
    public void end(SJPlayer winner) {
        end(winner, arena.isRated());
    }
    
    public void end(SJPlayer winner, boolean rated) {
        isOver = true;
        clock.cancel();
        saveGameHistory(winner);
        if(rated) {
            applyRatingChange(winner);
        }
        for(SJPlayer sjp : getActivePlayers()) {
            resetPlayer(sjp);
        }
        SuperJump.getInstance().getBattleManager().remove(this);
    }
    
    private void saveGameHistory(SJPlayer winner) {
        GameHistory gh = new GameHistory(this, winner);
        EntityBuilder.save(gh, SuperJump.getInstance().getPluginDB().getCollection("GameHistory"));
    }
    
    private void resetPlayer(SJPlayer sp) {
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sp.getPlayer());
        if(spectators.contains(sp)) {
            spectators.remove(sp);
        }
        else {
            sp.setIngame(false);
            sp.setFrozen(false);
            data.get(sp).restoreOldData();
        }
        sp.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());    
        sp.getPlayer().teleport(SpleefLeague.DEFAULT_WORLD.getSpawnLocation());
        slp.removeChatChannel(cc.getName());   
        slp.setState(PlayerState.IDLE);
    }
    
    private void applyRatingChange(SJPlayer winner) {
        int winnerPoints = 0;
        final int MIN_RATING = 1, MAX_RATING = 40;
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
    
    protected PlayerData getData(SJPlayer sjp) {
        return this.data.get(sjp);
    }
    
    protected int getDuration() {
        return ticksPassed;
    }
    
    protected static class PlayerData {
        
        private int falls;
        private final Location spawn;
        private final SJPlayer sjp;
        private final Area goal;
        private final GameMode oldGamemode;
        private final boolean oldFlying, oldAllowFlight;
        private final ItemStack[] oldInventory;
        
        public PlayerData(SJPlayer sjp, Location spawn, Area goal) {
            this.sjp = sjp;
            this.spawn = spawn;
            this.falls = 0;
            this.goal = goal;
            Player p = sjp.getPlayer();
            oldGamemode = p.getGameMode();
            oldFlying = p.isFlying();
            oldAllowFlight = p.getAllowFlight();
            oldInventory = p.getInventory().getContents();
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
        
        public void restoreOldData() {
            Player p = sjp.getPlayer();
            p.setGameMode(oldGamemode);
            p.setFlying(oldFlying);
            p.setAllowFlight(oldAllowFlight);
            p.getInventory().setContents(oldInventory);
        }
    }
}

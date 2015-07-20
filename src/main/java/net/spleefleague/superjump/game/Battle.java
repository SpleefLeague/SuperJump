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
import net.spleefleague.core.plugin.GamePlugin;
import net.spleefleague.core.utils.Area;
import net.spleefleague.core.utils.RuntimeCompiler;
import net.spleefleague.superjump.SuperJump;
import net.spleefleague.superjump.game.signs.GameSign;
import net.spleefleague.superjump.player.SJPlayer;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
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
            sp.getPlayer().teleport(spawn);
        }
        sp.getPlayer().setScoreboard(scoreboard);
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sp.getPlayer());
        slp.setState(PlayerState.SPECTATING);
        slp.addChatChannel(cc.getName());
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
            end(activePlayers.get(0));
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
        if(arena.getStartDebugger() != null) {
            RuntimeCompiler.debugFromHastebin(arena.getStartDebugger());
        }
        arena.setOccupied(true);
        GameSign.updateGameSigns(arena);
        ChatManager.registerChannel(cc);
        SuperJump.getInstance().getBattleManager().add(this);
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("rounds", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.GRAY + "0:0:0 | " + ChatColor.RED + "Times Fallen:");
        String playerNames = "";
        for(int i = 0; i < players.size(); i++) {
            SJPlayer sjp = players.get(i);
            if(i == 0) {
                playerNames = sjp.getName();
            }
            else if(i == players.size() - 1) {
                playerNames += " and " + sjp.getName();
            }
            else {
                playerNames += ", " + sjp.getName();
            }
            SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer());
            Player p = sjp.getPlayer();
            GamePlugin.dequeueGlobal(p);
            GamePlugin.unspectateGlobal(p);
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            sjp.setIngame(true);
            sjp.setFrozen(true);
            this.data.put(sjp, new PlayerData(sjp, arena.getSpawns()[i], arena.getGoals()[i % arena.getGoals().length]));
            p.setGameMode(GameMode.ADVENTURE);
            p.setFlying(false);
            p.setAllowFlight(false);
            for(PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            for(SJPlayer sjp1 : players) {
                if(sjp != sjp1) {
                    p.showPlayer(sjp1.getPlayer());
                }
            }
            slp.addChatChannel(cc.getName());
            p.eject();
            p.teleport(arena.getSpawns()[i]);
            p.getInventory().clear();
            p.setScoreboard(scoreboard);
            scoreboard.getObjective("rounds").getScore(sjp.getName()).setScore(data.get(sjp).getFalls());
            slp.setState(PlayerState.INGAME);
        }
        ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), Theme.SUCCESS.buildTheme(false) + "Beginning match on " + ChatColor.WHITE + arena.getName() + ChatColor.GREEN + " between " + ChatColor.RED + playerNames + "!", "GAME_MESSAGE_JUMP_START");
        startCountdown();
    }
    
    private void updateScoreboardTime() {
        if (scoreboard == null) {
            return;
        }
        Objective objective = scoreboard.getObjective("rounds");
        if (objective != null) {
            String s = DurationFormatUtils.formatDuration(ticksPassed * 50, "HH:mm:ss", true);
            objective.setDisplayName(ChatColor.GRAY.toString() + s + " | " + ChatColor.RED + "Score:");
        }
    }
    
    public void startCountdown() {
        inCountdown = true;
        for(SJPlayer sjp : getActivePlayers()) {
            Location spawn = this.data.get(sjp).getSpawn();
            createSpawnCage(spawn);
            sjp.setFrozen(true);
            sjp.getPlayer().setFireTicks(0);
            sjp.getPlayer().teleport(this.data.get(sjp).getSpawn());
        }
        BukkitRunnable br = new BukkitRunnable() {
            private int secondsLeft = 3;

            @Override
            public void run() {
                if (secondsLeft > 0) {
                    ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), secondsLeft + "...", cc.getName());
                    secondsLeft--;
                } else {
                    ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), "GO!", cc.getName());
                    for (SJPlayer sp : getActivePlayers()) {
                        sp.setFrozen(false);
                    }
                    onDone();
                    super.cancel();
                }
            }
            
            public void onDone() {
                for(SJPlayer sp : getActivePlayers()) {
                    removeSpawnCage(data.get(sp).getSpawn());
                    sp.setFrozen(false);
                }
                inCountdown = false;
            }
        };
        br.runTaskTimer(SuperJump.getInstance(), 20, 20);
        startClock();
    }
    
    private void createSpawnCage(Location s) {
        modifySpawnCage(s, Material.GLASS);
    }

    private void removeSpawnCage(Location s) {
        modifySpawnCage(s, Material.AIR);
    }

    private void modifySpawnCage(Location s, Material type) {
        World w = s.getWorld();
        for (int x = s.getBlockX() - 1; x <= s.getBlockX() + 1; x++) {
            for (int z = s.getBlockZ() - 1; z <= s.getBlockZ() + 1; z++) {
                if (x == s.getBlockX() && z == s.getBlockZ()) {
                    w.getBlockAt(x, s.getBlockY(), z).setType(Material.AIR); //Just in case
                    w.getBlockAt(x, s.getBlockY() + 1, z).setType(Material.AIR);
                    w.getBlockAt(x, s.getBlockY() + 2, z).setType(type);
                } 
                else {
                    for (int y = s.getBlockY(); y <= s.getBlockY() + 2; y++) {
                        w.getBlockAt(x, y, z).setType(type);
                    }
                }
            }
        }
    }
    
    private void startClock() {
        clock = new BukkitRunnable() {
            @Override
            public void run() {
                if(!inCountdown) {
                    ticksPassed++;
                }
                updateScoreboardTime();
            }
        };
        clock.runTaskTimer(SuperJump.getInstance(), 0, 1);
    }
    
    public void cancel() {
        cancel(true);
    }
    
    public void cancel(boolean moderator) {
        isOver = true;
        saveGameHistory(null);
        if(moderator) {
            ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), Theme.INCOGNITO.buildTheme(false) + "The battle has been cancelled by a moderator.", cc.getName());
        }
        for (SJPlayer sjp : getActivePlayers()) {
            resetPlayer(sjp);
        }
        for(SJPlayer sjp : new ArrayList<>(spectators)) {
            resetPlayer(sjp);
        }
        cleanup();
    }
    
    private void cleanup() {
        isOver = true;
        clock.cancel();
        arena.setOccupied(false);
        SuperJump.getInstance().getBattleManager().remove(this);
        ChatManager.unregisterChannel(cc);
        GameSign.updateGameSigns();
        if(arena.getEndDebugger() != null) {
            RuntimeCompiler.debugFromHastebin(arena.getEndDebugger());
        }
    }
    
    public void end(SJPlayer winner) {
        end(winner, arena.isRated());
    }
    
    public void end(SJPlayer winner, boolean rated) {
        saveGameHistory(winner);
        if(rated) {
            applyRatingChange(winner);
        }
        for(SJPlayer sjp : getActivePlayers()) {
            resetPlayer(sjp);
        }
        for(SJPlayer sp : new ArrayList<>(spectators)) {
            resetPlayer(sp);
        }
        cleanup();
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
            removeSpawnCage(this.getData(sp).getSpawn());
            sp.setIngame(false);
            sp.setFrozen(false);
            sp.setRequestingEndgame(false);
            data.get(sp).restoreOldData();
        }
        sp.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());    
        sp.getPlayer().teleport(SpleefLeague.getInstance().getSpawnLocation());
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
        ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), ChatColor.GREEN + "Game in arena " + ChatColor.WHITE + arena.getName() + ChatColor.GREEN + " is over. " + playerList, "GAME_MESSAGE_JUMP_END");
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
        private final ItemStack[] oldInventory;
        
        public PlayerData(SJPlayer sjp, Location spawn, Area goal) {
            this.sjp = sjp;
            this.spawn = spawn;
            this.falls = 0;
            this.goal = goal;
            Player p = sjp.getPlayer();
            oldGamemode = p.getGameMode();
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
            p.setFlying(false);
            p.getInventory().setContents(oldInventory);
        }
    }
}

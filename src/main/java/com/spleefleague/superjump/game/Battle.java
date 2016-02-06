/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.game;

import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatChannel;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.chat.Theme;
import com.spleefleague.core.io.EntityBuilder;
import com.spleefleague.core.listeners.FakeBlockHandler;
import com.spleefleague.core.player.GeneralPlayer;
import com.spleefleague.core.player.PlayerState;
import com.spleefleague.core.player.Rank;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.plugin.GamePlugin;
import com.spleefleague.core.utils.Area;
import com.spleefleague.core.utils.FakeArea;
import com.spleefleague.core.utils.FakeBlock;
import com.spleefleague.core.utils.RuntimeCompiler;
import com.spleefleague.superjump.SuperJump;
import com.spleefleague.superjump.game.signs.GameSign;
import com.spleefleague.superjump.player.SJPlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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
public class Battle implements com.spleefleague.core.queue.Battle<Arena, SJPlayer> {
    
    private final Arena arena;
    private final FakeArea fakeBlocks;
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
        this.fakeBlocks = new FakeArea();
        this.cc = ChatChannel.createTemporaryChannel("GAMECHANNEL" + this.hashCode(), null, Rank.DEFAULT, false, false);
    }
    
    @Override
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
        Location spawn = arena.getSpectatorSpawn();
        if(spawn != null) {
            sp.teleport(spawn);
        }
        sp.setScoreboard(scoreboard);
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sp.getPlayer());
        FakeBlockHandler.addArea(fakeBlocks, sp.getPlayer());
        slp.setState(PlayerState.SPECTATING);
        slp.addChatChannel(cc);
        for(SJPlayer sjp : getActivePlayers()) {
            sjp.showPlayer(sp.getPlayer());
            sp.showPlayer(sjp.getPlayer());
        }
        for(SJPlayer sjp : spectators) {
            sjp.showPlayer(sp.getPlayer());
            sp.showPlayer(sjp.getPlayer());
        }
        spectators.add(sp);
        hidePlayers(sp);
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
                pl.sendMessage(SuperJump.getInstance().getPrefix() + " " + Theme.ERROR.buildTheme(false) + sjp.getName() + " has left the game!");
            }
        }
    }
    
    @Override
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
        arena.registerGameStart();
        GameSign.updateGameSigns(arena);
        ChatManager.registerChannel(cc);
        SuperJump.getInstance().getBattleManager().add(this);
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("rounds", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.GRAY + "0:0:0 | " + ChatColor.RED + "Times Fallen:");
        String playerNames = "";
        FakeBlockHandler.addArea(fakeBlocks, GeneralPlayer.toBukkitPlayer(players.toArray(new SJPlayer[players.size()])));
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
            slp.addChatChannel(cc);
            slp.setState(PlayerState.INGAME);
            Player p = sjp.getPlayer();
            GamePlugin.dequeueGlobal(p);
            GamePlugin.unspectateGlobal(p);
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            sjp.setIngame(true);
            sjp.setFrozen(true);
            PlayerData pdata = new PlayerData(sjp, arena.getSpawns()[i], arena.getGoals()[i % arena.getGoals().length]);
            this.data.put(sjp, pdata);
            p.setGameMode(GameMode.ADVENTURE);
            p.setFlying(false);
            p.setAllowFlight(false);
            for(PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            for(SJPlayer sjp1 : players) {
                if(sjp != sjp1) {
                    sjp.showPlayer(sjp1.getPlayer());
                }
            }
            p.eject();
            p.teleport(arena.getSpawns()[i]);
            p.closeInventory();
            p.getInventory().clear();
            p.setScoreboard(scoreboard);
            scoreboard.getObjective("rounds").getScore(sjp.getName()).setScore(pdata.getFalls());
        }
        hidePlayers();
        ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), Theme.SUCCESS.buildTheme(false) + "Beginning match on " + ChatColor.WHITE + arena.getName() + ChatColor.GREEN + " between " + ChatColor.RED + playerNames + "!", SuperJump.getInstance().getStartMessageChannel());
        startCountdown();
    }
    
    private void hidePlayers() {
        List<SJPlayer> battlePlayers = getActivePlayers();
        battlePlayers.addAll(spectators);
        for(SJPlayer sjp : SuperJump.getInstance().getPlayerManager().getAll()) {
            hidePlayers(sjp);
        }
    }
    
    private void hidePlayers(SJPlayer target) {
        List<SJPlayer> battlePlayers = getActivePlayers();
        battlePlayers.addAll(spectators);
        for(SJPlayer active : battlePlayers) {
            if(!battlePlayers.contains(target)) {
                target.hidePlayer(active.getPlayer());
                active.hidePlayer(target.getPlayer());
            }
        }
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
        createSpawnCages();
        for(SJPlayer sjp : getActivePlayers()) {
            sjp.setFrozen(true);
            sjp.setFireTicks(0);
            sjp.teleport(this.data.get(sjp).getSpawn());
        }
        BukkitRunnable br = new BukkitRunnable() {
            private int secondsLeft = 3;

            @Override
            public void run() {
                if (secondsLeft > 0) {
                    ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), secondsLeft + "...", cc);
                    secondsLeft--;
                } else {
                    ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), "GO!", cc);
                    for(SJPlayer sp : getActivePlayers()) {
                        sp.setFrozen(false);
                    }
                    onDone();
                    super.cancel();
                }
            }
            
            public void onDone() {
                removeSpawnCages();
                for(SJPlayer sp : getActivePlayers()) {
                    sp.setFrozen(false);
                }
                inCountdown = false;
            }
        };
        br.runTaskTimer(SuperJump.getInstance(), 20, 20);
        startClock();
    }
    
    private void createSpawnCages() {
        fakeBlocks.clear();
        fakeBlocks.add(getSpawnCageBlocks(Material.GLASS));
        FakeBlockHandler.update(fakeBlocks);
    }
    
    private void removeSpawnCages() {
        fakeBlocks.clear();
        fakeBlocks.add(getSpawnCageBlocks(Material.AIR));
        FakeBlockHandler.update(fakeBlocks);
    }
    
    private FakeArea getSpawnCageBlocks(Material m) {
        FakeArea area = new FakeArea();
        for(Location spawn : arena.getSpawns()) {
            area.add(getCageBlocks(spawn, m));
        }
        return area;
    }
    
    private FakeArea getCageBlocks(Location loc, Material m) {
        loc = loc.getBlock().getLocation();
        FakeArea area = new FakeArea();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    area.addBlock(new FakeBlock(loc.clone().add(x, 2, z), m));
                } else {
                    for (int y = 0; y <= 2; y++) {
                        area.addBlock(new FakeBlock(loc.clone().add(x, y, z), m));
                    }
                }
            }
        }
        return area;
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
            ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), Theme.INCOGNITO.buildTheme(false) + "The battle has been cancelled by a moderator.", cc);
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
        arena.registerGameEnd();
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
        FakeBlockHandler.removeArea(fakeBlocks, slp.getPlayer());
        if(spectators.contains(sp)) {
            spectators.remove(sp);
        }
        else {
            sp.setIngame(false);
            sp.setFrozen(false);
            sp.setRequestingEndgame(false);
            sp.closeInventory();
            data.get(sp).restoreOldData();
        }
        sp.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());    
        sp.teleport(SpleefLeague.getInstance().getSpawnManager().getNext().getLocation());
        hidePlayers(sp);
        slp.removeChatChannel(cc);
        slp.setState(PlayerState.IDLE);
        slp.resetVisibility();
    }
    
    private void applyRatingChange(SJPlayer winner) {
        int winnerPoints = 0;
        final int MIN_RATING = 1, MAX_RATING = 40;
        String playerList = "";
        for(SJPlayer sjp : players) {
            if(sjp != winner) {
                float elo = (float) (1f / (1f + Math.pow(2f, ((sjp.getRating() - winner.getRating()) / 250f))));
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
        ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), ChatColor.GREEN + "Game in arena " + ChatColor.WHITE + arena.getName() + ChatColor.GREEN + " is over. " + playerList, SuperJump.getInstance().getEndMessageChannel());
    }

    public void onArenaLeave(SJPlayer sjp) {
        if(inCountdown) {
            sjp.teleport(data.get(sjp).getSpawn());
        }
        data.get(sjp).increaseFalls();
        sjp.teleport(data.get(sjp).getSpawn());
        scoreboard.getObjective("rounds").getScore(sjp.getName()).setScore(data.get(sjp).getFalls()); 
    }
    
    public PlayerData getData(SJPlayer sjp) {
        return this.data.get(sjp);
    }
    
    protected int getDuration() {
        return ticksPassed;
    }
    
    public static class PlayerData {
        
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

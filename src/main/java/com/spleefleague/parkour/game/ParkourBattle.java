/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game;

import com.comphenix.packetwrapper.WrapperPlayServerPlayerInfo;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.google.common.collect.Lists;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatChannel;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.chat.Theme;
import com.spleefleague.gameapi.events.BattleEndEvent;
import com.spleefleague.gameapi.events.BattleEndEvent.EndReason;
import com.spleefleague.gameapi.events.BattleStartEvent;
import com.spleefleague.core.player.*;
import com.spleefleague.core.utils.Area;
import com.spleefleague.core.utils.debugger.RuntimeCompiler;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.gameapi.GamePlugin;
import com.spleefleague.gameapi.events.BattleStartEvent.StartReason;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.player.ParkourPlayer;
import com.spleefleague.virtualworld.VirtualWorld;
import com.spleefleague.virtualworld.api.FakeWorld;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bson.Document;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Team;

/**
 *
 * @author Jonas
 * @param <A> Arena type
 */
public abstract class ParkourBattle<A extends Arena> implements com.spleefleague.gameapi.queue.Battle<A, ParkourPlayer> {

    protected A arena;
    protected final FakeWorld fakeWorld;
    protected final List<ParkourPlayer> players; //MIGHT CONTAIN PLAYERS WHICH LEFT THE GAME. USE getActivePlayers() FOR ACTIVE PLAYERS INSTEAD
    protected HashMap<ParkourPlayer, PlayerData> data;
    protected final List<ParkourPlayer> spectators;
    protected final ChatChannel cc;
    protected long ticksPassed = 0;
    protected long timeLastLap = 0;
    protected BukkitTask clockScoreboard; // Updates time until reset (and high scores)
    protected BukkitTask clockExperience; // 
    protected BukkitTask clockAction;     // Time is displayed above the item text
    protected Scoreboard scoreboard;
    protected boolean inCountdown;
    protected boolean isOver;
    private final ParkourMode mode;
    private static ItemStack itemEndGame;

    protected ParkourBattle(A arena, List<ParkourPlayer> players) {
        this(arena, players, arena.getParkourMode());
    }
    
    private ParkourBattle(A arena, List<ParkourPlayer> players, ParkourMode mode) {
        this.arena = arena;
        this.mode = mode;
        this.players = players;
        this.spectators = new ArrayList<>();
        this.data = new HashMap<>();
        this.fakeWorld = VirtualWorld.getInstance().getFakeWorldManager().createWorld(arena.getSpawns()[0].getWorld());
        this.cc = ChatChannel.createTemporaryChannel("GAMECHANNEL" + this.hashCode(), null, Rank.DEFAULT, false, false);
    }
    
    protected abstract void addToBattleManager();
    protected abstract void removeFromBattleManager();
    protected abstract void applyRatingChange(ParkourPlayer winner);

    public ParkourMode getParkourMode() {
        return mode;
    }
    
    public void start(StartReason reason) {
        for(ParkourPlayer sjp : players) {
            VirtualWorld.getInstance().getFakeWorldManager().addWorld(sjp.getPlayer(), fakeWorld, 10);
        }
        players.forEach(p -> {
            GamePlugin.unspectateGlobal(p);
            GamePlugin.dequeueGlobal(p);
        });
        BattleStartEvent event = new BattleStartEvent(this, reason);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            initPlayers();
            createScoreboard();
            applyScoreboard();
            announceStart();
            if (arena.getStartDebugger() != null) {
                RuntimeCompiler.debugFromHastebin(arena.getStartDebugger(), null);
            }
            arena.registerGameStart();
            addToBattleManager();
            startCountdown();
        }
    }
    
    public void initChatChannel() {
        ChatManager.registerChannel(cc);
        announceStart();
    }
    
    public void announceStart() {
        if(players.size() == 1) {
            ParkourPlayer sjp = players.get(0);
            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(players.get(0)), Parkour.getInstance().getChatPrefix()
                    + Parkour.fillColor + " You have begun a match on "
                    + Parkour.arenaColor + arena.getName()
                    + Parkour.fillColor + ".");
        }
        else {
            String playerNames = "";
            for(int i = 0; i < players.size(); i++) {
                ParkourPlayer sjp = players.get(i);
                String format = Parkour.playerColor + sjp.getName() + Parkour.fillColor + " (" + Parkour.pointColor + sjp.getParagonLevel() + Parkour.fillColor + ")";
                if (i == 0) {
                    playerNames = format;
                } else if (i == players.size() - 1) {
                    playerNames += Parkour.fillColor + " and " + format;
                } else {
                    playerNames += Parkour.fillColor + ", " + format;
                }
            }
            ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(), Parkour.fillColor + "A match on "
                    + Parkour.arenaColor + arena.getName()
                    + Parkour.fillColor + " has begun between "
                    + playerNames
                    + Parkour.fillColor + "."
                    , Parkour.getInstance().getStartMessageChannel());
        }
    }
    
    public String getTimeString() {
        return DurationFormatUtils.formatDuration(ticksPassed * 50, "HH:mm:ss", true);
    }
    
    public String getLapTimeString() {
        return DurationFormatUtils.formatDuration((timeLastLap - System.currentTimeMillis()) * 50, "ss.SSS", true);
    }
    
    public void initPlayers() {
        Team team = scoreboard.registerNewTeam("NO_COLLISION");
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        for (int i = 0; i < players.size(); i++) {    
            ParkourPlayer sjp = players.get(i);
            team.addEntry(sjp.getName());
            SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer());
            slp.addChatChannel(cc);
            slp.setState(PlayerState.INGAME);
            Player p = sjp.getPlayer();
            p.setHealth(20);
            p.setFoodLevel(20);
            sjp.setIngame(true);
            sjp.setFrozen(true);
            sjp.setMovedInMatch(false);
            PlayerData pdata = new PlayerData(sjp, arena.getSpawns()[i], arena.getGoals()[i % arena.getGoals().length]);
            this.data.putIfAbsent(sjp, pdata);
            p.setGameMode(GameMode.ADVENTURE);
            p.setFlying(false);
            p.setAllowFlight(false);
            p.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(p::removePotionEffect);
            players.stream().filter(sjpt -> sjp != sjpt).forEach(sjpt -> sjp.showPlayer(sjpt.getPlayer()));
            p.eject();
            p.teleport(arena.getSpawns()[i].clone().add(0, 0.0, 0));
            p.closeInventory();
            PlayerInventory inv = sjp.getInventory();
            inv.clear();
            inv.setItem(8, itemEndGame);
            inv.setHeldItemSlot(0);
        }
        hidePlayers();
    }
    
    public void createScoreboard() {
        Objective objective = scoreboard.registerNewObjective("rounds", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.GRAY + "0:0:0 | " + ChatColor.RED + "Times Fallen:");
        ParkourPlayer sjp = players.get(0);
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer());
        slp.addChatChannel(cc);
        slp.setState(PlayerState.INGAME);
        sjp.setIngame(true);
        sjp.setFrozen(true);
        PlayerData pdata = new PlayerData(sjp, arena.getSpawns()[0], arena.getGoals()[0]);
        this.data.putIfAbsent(sjp, pdata);
        scoreboard.getObjective("rounds").getScore(slp.getName()).setScore(0);
    }
    
    public void applyScoreboard() {
        for(int i = 0; i < players.size(); i++) {
            players.get(i).getPlayer().setScoreboard(scoreboard);
        }
    }

    @Override
    public A getArena() {
        return arena;
    }
    
    public FakeWorld getFakeWorld() {
        return fakeWorld;
    }

    @Override
    public List<ParkourPlayer> getPlayers() {
        return players;
    }

    @Override
    public List<ParkourPlayer> getSpectators() {
        return this.spectators;
    }

    public Location getSpawn(ParkourPlayer sjp) {
        return data.get(sjp).getSpawn();
    }
    
    public void setGoal(ParkourPlayer sjp, Area goal) {
        data.get(sjp).setGoal(goal);
    }

    public Area getGoal(ParkourPlayer sjp) {
        return data.get(sjp).getGoal();
    }
    
    public void addSpectator(ParkourPlayer sp, ParkourPlayer target) {
        sp.setScoreboard(scoreboard);
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sp.getPlayer());
        VirtualWorld.getInstance().getFakeWorldManager().addWorld(sp.getPlayer(), fakeWorld, 10);
        slp.setState(PlayerState.SPECTATING);
        slp.addChatChannel(cc);
        for (ParkourPlayer sjp : getActivePlayers()) {
            sjp.showPlayer(sp.getPlayer());
            sp.showPlayer(sjp.getPlayer());
        }
        for (ParkourPlayer sjp : spectators) {
            sjp.showPlayer(sp.getPlayer());
            sp.showPlayer(sjp.getPlayer());
        }
        spectators.add(sp);
        sp.setIngame(true);
        hidePlayers(sp);
        sp.setParkourMode(target.getParkourMode());
        sp.teleport(target.getPlayer());
        sp.setGameMode(GameMode.SPECTATOR);
        sp.setParkourSpectatorTarget(null);
    }

    public boolean isSpectating(ParkourPlayer sjp) {
        return spectators.contains(sjp);
    }

    public void removeSpectator(ParkourPlayer sp) {
        List<Player> ingamePlayers = new ArrayList<>();
        for (ParkourPlayer p : getActivePlayers()) {
            sp.getPlayer().hidePlayer(p.getPlayer());
            p.getPlayer().hidePlayer(sp.getPlayer());
            ingamePlayers.add(p.getPlayer());
        }
        Bukkit.getScheduler().runTaskLater(Parkour.getInstance(), () -> {
            List<PlayerInfoData> list = new ArrayList<>();
            SpleefLeague.getInstance().getPlayerManager().getAll().forEach((SLPlayer slPlayer) -> list.add(new PlayerInfoData(WrappedGameProfile.fromPlayer(slPlayer.getPlayer()), ((CraftPlayer) slPlayer.getPlayer()).getHandle().ping, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(slPlayer.getRank().getColor() + slPlayer.getName()))));
            WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo();
            packet.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            packet.setData(list);
            ingamePlayers.forEach((Player p) -> packet.sendPacket(p));

            list.clear();
            ingamePlayers.forEach((Player p) -> {
                SLPlayer generalPlayer = SpleefLeague.getInstance().getPlayerManager().get(p);
                list.add(new PlayerInfoData(WrappedGameProfile.fromPlayer(p), ((CraftPlayer) p).getHandle().ping, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(generalPlayer.getRank().getColor() + generalPlayer.getName())));
            });
            packet.setData(list);
            packet.sendPacket(sp.getPlayer());
        }, 10);
        resetPlayer(sp);
    }

    public void removePlayer(ParkourPlayer sp, boolean surrender) {
        if (!surrender) {
            for (ParkourPlayer pl : getActivePlayers()) {
                pl.sendMessage(Parkour.getInstance().getChatPrefix() + " " + Theme.ERROR.buildTheme(false) + sp.getName() + " has left the game!");
            }
            for (ParkourPlayer pl : spectators) {
                pl.sendMessage(Parkour.getInstance().getChatPrefix() + " " + Theme.ERROR.buildTheme(false) + sp.getName() + " has left the game!");
            }
        }
        resetPlayer(sp);
        ArrayList<ParkourPlayer> activePlayers = getActivePlayers();
        if (activePlayers.size() == 1) {
            end(activePlayers.get(0), EndReason.ENDGAME);
        }
        else if(activePlayers.isEmpty()) {
            end(null, EndReason.ENDGAME);
        }
    }

    @Override
    public ArrayList<ParkourPlayer> getActivePlayers() {
        ArrayList<ParkourPlayer> active = new ArrayList<>();
        for (ParkourPlayer sjp : players) {
            if (sjp.isIngame()) {
                active.add(sjp);
            }
        }
        return active;
    }

    public boolean isOver() {
        return isOver;
    }

    protected void hidePlayers() {
        List<ParkourPlayer> battlePlayers = getActivePlayers();
        battlePlayers.addAll(spectators);
        for (ParkourPlayer sjp : Parkour.getInstance().getPlayerManager().getAll()) {
            hidePlayers(sjp);
        }
    }

    protected void hidePlayers(ParkourPlayer target) {
        List<ParkourPlayer> battlePlayers = getActivePlayers();
        battlePlayers.addAll(spectators);
        for (ParkourPlayer active : battlePlayers) {
            if (!battlePlayers.contains(target)) {
                target.hidePlayer(active.getPlayer());
                active.hidePlayer(target.getPlayer());
            }
        }
    }

    protected void updateScoreboardTime() {
        if (scoreboard == null) {
            return;
        }
        Objective objective = scoreboard.getObjective("rounds");
        if (objective != null) {
            String s = DurationFormatUtils.formatDuration(ticksPassed * 50, "HH:mm:ss", true);
            objective.setDisplayName(ChatColor.GRAY.toString() + s + " | " + ChatColor.RED + "Score:");
        }
    }
    
    protected void updateExperienceScore() {
        /*
        getActivePlayers().forEach(slp -> {
            slp.setLevel((int)Math.floorDiv(System.currentTimeMillis() - this.timeLastLap, 1000));
            slp.setExp(((System.currentTimeMillis() - this.timeLastLap) % 1000) / 1000.f);
        });
        getSpectators().forEach(slp -> {
            slp.setLevel((int)Math.floorDiv(System.currentTimeMillis() - this.timeLastLap, 1000));
            slp.setExp(((System.currentTimeMillis() - this.timeLastLap) % 1000) / 1000.f);
        });
        */
    }
    
    protected void updateActionBar() {
        this.getActivePlayers().forEach(sjp -> {
            sjp.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder(ChatColor.YELLOW + "" + String.format("%.2f", (float)ticksPassed / 20.f) + " seconds").create());
        });
    }

    public void startCountdown() {
        inCountdown = true;
        for (ParkourPlayer sjp : getActivePlayers()) {
            sjp.setFrozen(true);
            sjp.setFireTicks(0);
            sjp.teleport(this.data.get(sjp).getSpawn());
        }
        BukkitRunnable br = new BukkitRunnable() {
            private int secondsLeft = 3;

            @Override
            public void run() {
                if (secondsLeft > 0) {
                    ChatManager.sendTitle(ChatColor.RED + "" + secondsLeft + "...", "", 0, 15, 5, cc);
                    //ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(), secondsLeft + "...", cc);
                    secondsLeft--;
                } else {
                    ChatManager.sendTitle(ChatColor.GREEN + "GO!", "", 0, 15, 5, cc);
                    //ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(), "GO!", cc);
                    for (ParkourPlayer sp : getActivePlayers()) {
                        sp.setFrozen(false);
                    }
                    onDone();
                    super.cancel();
                }
            }

            public void onDone() {
                for (ParkourPlayer sp : getActivePlayers()) {
                    sp.setFrozen(false);
                }
                inCountdown = false;
                timeLastLap = System.currentTimeMillis();
            }
        };
        br.runTaskTimer(Parkour.getInstance(), 20, 20);
        startClock();
    }
    
    protected void startClock() {
        clockScoreboard = new BukkitRunnable() {
            @Override
            public void run() {
                updateScoreboardTime();
            }
        }.runTaskTimer(Parkour.getInstance(), 0, 20);
        clockExperience = new BukkitRunnable() {
            @Override
            public void run() {
                if (!inCountdown) {
                    updateExperienceScore();
                }
            }
        }.runTaskTimer(Parkour.getInstance(), 0, 100);
        clockAction = new BukkitRunnable() {
            @Override
            public void run() {
                if (!inCountdown) {
                    ticksPassed++;
                    updateActionBar();
                }
            }
        }.runTaskTimer(Parkour.getInstance(), 0, 1);
    }

    public void cancel() {
        end(null, EndReason.CANCEL);
    }

    public void end(ParkourPlayer winner, EndReason reason) {
        saveGameHistory(winner, reason);
        if (reason == BattleEndEvent.EndReason.CANCEL) {
            if (reason == BattleEndEvent.EndReason.CANCEL) {
                ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(), Theme.INCOGNITO.buildTheme(false) + "The battle has been cancelled by a moderator.", cc);
            }
        } else if (reason != BattleEndEvent.EndReason.ENDGAME) {
            SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(winner.getPlayer());
            float levelTime = Math.floorDiv((System.currentTimeMillis() - timeLastLap), 10) / 100.f;
            if(levelTime < arena.getBanTime()) {
                if(!slp.getRank().hasPermission(Rank.DEVELOPER)) {
                    slp.performBan(null,
                            "Console",
                            "Cheating on " + this.arena.getName());
                }
                else {
                    ChatManager.sendMessage(slp.getName() + " abused their power!", Parkour.getInstance().getEndMessageChannel());
                }
            }
            players.forEach(sjp -> {
                slp.setCoins(slp.getCoins() + 1);
            });
            if (arena.isRated()) {
                applyRatingChange(winner);
            }
        }
        Lists.newArrayList(getSpectators()).forEach(this::resetPlayer);
        ArrayList<ParkourPlayer> players = Lists.newArrayList(getActivePlayers());
        players.forEach(this::resetPlayer);
        if (reason == BattleEndEvent.EndReason.NORMAL) {
            players.forEach(sjp -> Parkour.getInstance().requeuePlayer(sjp));
        }
        Bukkit.getPluginManager().callEvent(new BattleEndEvent(this, reason));
        cleanup();
    }

    protected void cleanup() {
        isOver = true;
        clockScoreboard.cancel();
        clockExperience.cancel();
        clockAction.cancel();
        arena.registerGameEnd();
        removeFromBattleManager();
        ChatManager.unregisterChannel(cc);
        if (arena.getEndDebugger() != null) {
            RuntimeCompiler.debugFromHastebin(arena.getEndDebugger(), null);
        }
    }

    protected void saveGameHistory(ParkourPlayer winner, EndReason reason) {
        Bukkit.getScheduler().runTaskAsynchronously(Parkour.getInstance(), () -> {
            GameHistory gh = new GameHistory(this, winner, reason);
            try {
                EntityBuilder.save(gh, Parkour.getInstance().getPluginDB().getCollection("GameHistory"));
            } catch(Exception e) {
                Parkour.LOG.log(Level.WARNING, "Could not save GameHistory!");
                Document doc = EntityBuilder.serialize(gh).get("$set", Document.class);
                Parkour.LOG.log(Level.WARNING, doc.toJson());
                e.printStackTrace();
            }
        });
    }

    protected void resetPlayer(ParkourPlayer sp) {
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sp.getPlayer());
        VirtualWorld.getInstance().getFakeWorldManager().removeWorld(sp.getPlayer(), fakeWorld);
        if (spectators.contains(sp)) {
            spectators.remove(sp);
        } else {
            sp.setFrozen(false);
            sp.setRequestingEndgame(false);
            data.get(sp).restoreOldData();
        }
        sp.setIngame(false);
        sp.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        hidePlayers(sp);
        slp.removeChatChannel(cc);
        slp.setState(PlayerState.IDLE);
        slp.resetVisibility();
        sp.setExp(0);
        sp.setLevel(0);
        sp.setParkourMode(ParkourMode.NONE);
        sp.closeInventory();
        if (sp.getGameMode() == GameMode.SPECTATOR) {
            sp.setParkourSpectatorTarget(null);
        }
        if (slp.getRank().hasPermission(Rank.DEVELOPER)) {
            sp.setGameMode(GameMode.CREATIVE);
        }
        else {
            sp.setGameMode(GameMode.SURVIVAL);
        }
        sp.teleport(SpleefLeague.getInstance().getSpawnManager().getNext().getLocation());
    }
    
    public void focusSpectators() {
        new BukkitRunnable() {
            @Override
            public void run() {
                getSpectators().forEach(spectator -> spectator.focusParkourSpectatorTarget());
            }
        }.runTaskLater(Parkour.getInstance(), 5);
    }
    
    public void onArenaLeave(ParkourPlayer sjp) {
        if (getActivePlayers().contains(sjp)) {
            if (inCountdown) {
                sjp.teleport(data.get(sjp).getSpawn());
            }
            data.get(sjp).increaseFalls();
            if (!sjp.getGameMode().equals(GameMode.CREATIVE)) {
                sjp.teleport(data.get(sjp).getSpawn());
            }
            focusSpectators();
        }
        else if (spectators.contains(sjp)) {
            if (sjp.getParkourSpectatorTarget() != null) {
                sjp.teleport(sjp.getParkourSpectatorTarget());
            }
            else {
                sjp.teleport(arena.getSpawns()[0]);
            }
        }
    }

    public PlayerData getData(ParkourPlayer sjp) {
        return this.data.get(sjp);
    }

    protected long getDuration() {
        return ticksPassed;
    }

    public static class PlayerData {

        private int falls;
        private final Location spawn;
        private final ParkourPlayer sjp;
        private Area goal;
        private final GameMode oldGamemode;
        private final ItemStack[] oldInventory;

        public PlayerData(ParkourPlayer sjp, Location spawn, Area goal) {
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
        
        public void setGoal(Area goal) {
            this.goal = goal;
        }

        public int getFalls() {
            return falls;
        }
        
        public void setFalls(int falls) {
            this.falls = falls;
        }

        public void increaseFalls() {
            this.falls++;
        }

        public ParkourPlayer getPlayer() {
            return sjp;
        }

        public void restoreOldData() {
            Player p = sjp.getPlayer();
            p.setGameMode(oldGamemode);
            p.setFlying(false);
            p.getInventory().setContents(oldInventory);
        }
    }
    
    public static ItemStack getItemEndGame() {
        return itemEndGame;
    }
    
    static {
        itemEndGame = new ItemStack(Material.BARRIER);
        ItemMeta iegm = itemEndGame.getItemMeta();
        iegm.setDisplayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "END MATCH");
        itemEndGame.setItemMeta(iegm);
    }
}

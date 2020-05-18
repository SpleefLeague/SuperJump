/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
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
import java.lang.reflect.InvocationTargetException;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

/**
 *
 * @author Jonas
 * @param <A> Arena type
 */
public abstract class ParkourBattle<A extends Arena> implements com.spleefleague.gameapi.queue.Battle<A, ParkourPlayer> {

    protected final A arena;
    protected final FakeWorld fakeWorld;
    protected final List<ParkourPlayer> players; //MIGHT CONTAIN PLAYERS WHICH LEFT THE GAME. USE getActivePlayers() FOR ACTIVE PLAYERS INSTEAD
    protected final HashMap<ParkourPlayer, PlayerData> data;
    protected final List<ParkourPlayer> spectators;
    protected final ChatChannel cc;
    protected int ticksPassed = 0;
    protected BukkitRunnable clock;
    protected Scoreboard scoreboard;
    protected boolean inCountdown;
    protected boolean isOver;
    private final Collection<Vector> spawnCageDefinition;
    private final ParkourMode mode;

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
        this.spawnCageDefinition = generateSpawnCageDefinition();
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
            if (arena.getStartDebugger() != null) {
                RuntimeCompiler.debugFromHastebin(arena.getStartDebugger(), null);
            }
            arena.registerGameStart();
            ChatManager.registerChannel(cc);
            addToBattleManager();
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Team team = scoreboard.registerNewTeam("NO_COLLISION");
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            Objective objective = scoreboard.registerNewObjective("rounds", "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName(ChatColor.GRAY + "0:0:0 | " + ChatColor.RED + "Times Fallen:");
            String playerNames = "";
            for (int i = 0; i < players.size(); i++) {    
                ParkourPlayer sjp = players.get(i);
                if (i == 0) {
                    playerNames = sjp.getName();
                } else if (i == players.size() - 1) {
                    playerNames += ChatColor.GREEN + " and " + ChatColor.RED + sjp.getName();
                } else {
                    playerNames += ChatColor.GREEN + ", " + ChatColor.RED + sjp.getName();
                }
                team.addEntry(sjp.getName());
                SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer());
                slp.addChatChannel(cc);
                slp.setState(PlayerState.INGAME);
                Player p = sjp.getPlayer();
                p.setHealth(p.getMaxHealth());
                p.setFoodLevel(20);
                sjp.setIngame(true);
                sjp.setFrozen(true);
                PlayerData pdata = new PlayerData(sjp, arena.getSpawns()[i], arena.getGoals()[i % arena.getGoals().length]);
                this.data.putIfAbsent(sjp, pdata);
                p.setGameMode(GameMode.ADVENTURE);
                p.setFlying(false);
                p.setAllowFlight(false);
                p.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(p::removePotionEffect);
                players.stream().filter(sjpt -> sjp != sjpt).forEach(sjpt -> sjp.showPlayer(sjpt.getPlayer()));
                p.eject();
                p.teleport(arena.getSpawns()[i]);
                p.closeInventory();
                p.getInventory().clear();
                p.setScoreboard(scoreboard);
                scoreboard.getObjective("rounds").getScore(sjp.getName()).setScore(pdata.getFalls());
            }
            hidePlayers();
            ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(), Theme.SUCCESS.buildTheme(false) + "Beginning match on " + ChatColor.WHITE + arena.getName() + ChatColor.GREEN + " between " + ChatColor.RED + playerNames + "!", Parkour.getInstance().getStartMessageChannel());
            startCountdown();
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

    public Area getGoal(ParkourPlayer sjp) {
        return data.get(sjp).getGoal();
    }
    
    
    private void setSpawnCageBlock(Material type) {
        for (Location spawn : this.arena.getSpawns()) {
            for (Vector vector : spawnCageDefinition) {
                fakeWorld.getBlockAt(spawn.clone().add(vector)).setType(type);
            }
        }
    }

    public Collection<Vector> generateSpawnCageDefinition() {
        Collection<Vector> col = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    col.add(new Vector(x, 2, z));
                } else {
                    for (int y = 0; y <= 2; y++) {
                        col.add(new Vector(x, y, z));
                    }
                }
            }
        }
        return col;
    }

    public void addSpectator(ParkourPlayer sp) {
        Location spawn = arena.getSpectatorSpawn();
        if (spawn != null) {
            sp.teleport(spawn);
        }
        sp.setScoreboard(scoreboard);
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sp.getPlayer());
        VirtualWorld.getInstance().getFakeWorldManager().addWorld(sp.getPlayer(), fakeWorld, 10);
        slp.setState(PlayerState.SPECTATING);
        sp.setGameMode(GameMode.ADVENTURE);
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
        hidePlayers(sp);
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
            PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
            packetContainer.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            List<PlayerInfoData> playerInfoDatas = new ArrayList<>();
            for (SLPlayer slPlayer : SpleefLeague.getInstance().getPlayerManager().getAll()) {
                playerInfoDatas.add(new PlayerInfoData(
                        WrappedGameProfile.fromPlayer(slPlayer.getPlayer()),
                        ((CraftPlayer) slPlayer.getPlayer()).getHandle().ping,
                        EnumWrappers.NativeGameMode.SURVIVAL,
                        WrappedChatComponent.fromText(slPlayer.getRank().getColor() + slPlayer.getName())));
            }
            packetContainer.getPlayerInfoDataLists().write(0, playerInfoDatas);
            for (Player p : ingamePlayers) {
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(p, packetContainer);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(ParkourBattle.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            packetContainer = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
            packetContainer.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            playerInfoDatas.clear();
            for (Player p : ingamePlayers) {
                SLPlayer generalPlayer = SpleefLeague.getInstance().getPlayerManager().get(p);
                playerInfoDatas.add(new PlayerInfoData(
                        WrappedGameProfile.fromPlayer(p),
                        ((CraftPlayer) p).getHandle().ping,
                        EnumWrappers.NativeGameMode.SURVIVAL,
                        WrappedChatComponent.fromText(generalPlayer.getRank().getColor() + generalPlayer.getName())));
            }
            packetContainer.getPlayerInfoDataLists().write(0, playerInfoDatas);
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(sp.getPlayer(), packetContainer);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(ParkourBattle.class.getName()).log(Level.SEVERE, null, ex);
            }
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
            end(activePlayers.get(0), EndReason.NORMAL);
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

    public void startCountdown() {
        inCountdown = true;
        setSpawnCageBlock(Material.GLASS);
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
                    ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(), secondsLeft + "...", cc);
                    secondsLeft--;
                } else {
                    ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(), "GO!", cc);
                    for (ParkourPlayer sp : getActivePlayers()) {
                        sp.setFrozen(false);
                    }
                    onDone();
                    super.cancel();
                }
            }

            public void onDone() {
                setSpawnCageBlock(Material.AIR);
                for (ParkourPlayer sp : getActivePlayers()) {
                    sp.setFrozen(false);
                }
                inCountdown = false;
            }
        };
        br.runTaskTimer(Parkour.getInstance(), 20, 20);
        startClock();
    }
    
    protected void startClock() {
        clock = new BukkitRunnable() {
            @Override
            public void run() {
                if (!inCountdown) {
                    ticksPassed++;
                }
                updateScoreboardTime();
            }
        };
        clock.runTaskTimer(Parkour.getInstance(), 0, 1);
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
            if (arena.isRated()) {
                applyRatingChange(winner);
            }
        }
        Lists.newArrayList(getSpectators()).forEach(this::resetPlayer);
        Lists.newArrayList(getActivePlayers()).forEach(this::resetPlayer);
        Bukkit.getPluginManager().callEvent(new BattleEndEvent(this, reason));
        cleanup();
    }

    private void cleanup() {
        isOver = true;
        clock.cancel();
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

    private void resetPlayer(ParkourPlayer sp) {
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sp.getPlayer());
        VirtualWorld.getInstance().getFakeWorldManager().removeWorld(sp.getPlayer(), fakeWorld);
        if (spectators.contains(sp)) {
            spectators.remove(sp);
        } else {
            sp.setIngame(false);
            sp.setFrozen(false);
            sp.setRequestingEndgame(false);
            sp.closeInventory();
            data.get(sp).restoreOldData();
        }
        if (sp.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            sp.getPlayer().setSpectatorTarget(null);
        }
        sp.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        sp.teleport(SpleefLeague.getInstance().getSpawnManager().getNext().getLocation());
        sp.setGameMode(GameMode.SURVIVAL);
        hidePlayers(sp);
        slp.removeChatChannel(cc);
        slp.setState(PlayerState.IDLE);
        slp.resetVisibility();
    }

    

    public void onArenaLeave(ParkourPlayer sjp) {
        if (inCountdown) {
            sjp.teleport(data.get(sjp).getSpawn());
        }
        data.get(sjp).increaseFalls();
        sjp.teleport(data.get(sjp).getSpawn());
        scoreboard.getObjective("rounds").getScore(sjp.getName()).setScore(data.get(sjp).getFalls());
    }

    public PlayerData getData(ParkourPlayer sjp) {
        return this.data.get(sjp);
    }

    protected int getDuration() {
        return ticksPassed;
    }

    public static class PlayerData {

        private int falls;
        private final Location spawn;
        private final ParkourPlayer sjp;
        private final Area goal;
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

        public int getFalls() {
            return falls;
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
}

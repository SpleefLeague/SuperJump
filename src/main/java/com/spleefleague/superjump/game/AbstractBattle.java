/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.game;

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
import com.spleefleague.core.events.BattleEndEvent;
import com.spleefleague.core.events.BattleEndEvent.EndReason;
import com.spleefleague.core.events.BattleStartEvent;
import com.spleefleague.core.io.EntityBuilder;
import com.spleefleague.core.player.*;
import com.spleefleague.core.utils.Area;
import com.spleefleague.core.utils.debugger.RuntimeCompiler;
import com.spleefleague.fakeblocks.packet.FakeBlockHandler;
import com.spleefleague.fakeblocks.representations.FakeArea;
import com.spleefleague.fakeblocks.representations.FakeBlock;
import com.spleefleague.superjump.SuperJump;
import com.spleefleague.superjump.game.signs.GameSign;
import com.spleefleague.superjump.player.SJPlayer;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Jonas
 */
public abstract class AbstractBattle implements com.spleefleague.core.queue.Battle<Arena, SJPlayer> {

    protected final Arena arena;
    protected final FakeArea fakeBlocks;
    protected final List<SJPlayer> players; //MIGHT CONTAIN PLAYERS WHICH LEFT THE GAME. USE getActivePlayers() FOR ACTIVE PLAYERS INSTEAD
    protected final HashMap<SJPlayer, PlayerData> data;
    protected final List<SJPlayer> spectators;
    protected final ChatChannel cc;
    protected int ticksPassed = 0;
    protected BukkitRunnable clock;
    protected Scoreboard scoreboard;
    protected boolean inCountdown;
    protected boolean isOver;
    protected FakeBlockHandler handler;

    protected AbstractBattle(Arena arena, List<SJPlayer> players) {
        this.handler = SpleefLeague.getInstance().getFakeBlockHandler();
        this.arena = arena;
        this.players = players;
        this.spectators = new ArrayList<>();
        this.data = new HashMap<>();
        this.fakeBlocks = new FakeArea();
        this.cc = ChatChannel.createTemporaryChannel("GAMECHANNEL" + this.hashCode(), null, Rank.DEFAULT, false, false);
    }
    
    public abstract void start(BattleStartEvent.StartReason reason);
    
    protected abstract void getSpawnCageBlocks();
    
    protected abstract void applyRatingChange(SJPlayer winner);

    @Override
    public Arena getArena() {
        return arena;
    }

    @Override
    public Collection<SJPlayer> getPlayers() {
        return players;
    }

    @Override
    public Collection<SJPlayer> getSpectators() {
        return this.spectators;
    }

    public Location getSpawn(SJPlayer sjp) {
        return data.get(sjp).getSpawn();
    }

    public Area getGoal(SJPlayer sjp) {
        return data.get(sjp).getGoal();
    }

    public void addSpectator(SJPlayer sp) {
        Location spawn = arena.getSpectatorSpawn();
        if (spawn != null) {
            sp.teleport(spawn);
        }
        sp.setScoreboard(scoreboard);
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sp.getPlayer());
        handler.addArea(fakeBlocks, sp.getPlayer());
        slp.setState(PlayerState.SPECTATING);
        slp.addChatChannel(cc);
        for (SJPlayer sjp : getActivePlayers()) {
            sjp.showPlayer(sp.getPlayer());
            sp.showPlayer(sjp.getPlayer());
        }
        for (SJPlayer sjp : spectators) {
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
        List<Player> ingamePlayers = new ArrayList<>();
        for (SJPlayer p : getActivePlayers()) {
            sp.getPlayer().hidePlayer(p.getPlayer());
            p.getPlayer().hidePlayer(sp.getPlayer());
            ingamePlayers.add(p.getPlayer());
        }
        Bukkit.getScheduler().runTaskLater(SuperJump.getInstance(), () -> {
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

    public void removePlayer(SJPlayer sp, boolean surrender) {
        if (!surrender) {
            for (SJPlayer pl : getActivePlayers()) {
                pl.sendMessage(SuperJump.getInstance().getChatPrefix() + " " + Theme.ERROR.buildTheme(false) + sp.getName() + " has left the game!");
            }
            for (SJPlayer pl : spectators) {
                pl.sendMessage(SuperJump.getInstance().getChatPrefix() + " " + Theme.ERROR.buildTheme(false) + sp.getName() + " has left the game!");
            }
        }
        resetPlayer(sp);
        ArrayList<SJPlayer> activePlayers = getActivePlayers();
        if (activePlayers.size() == 1) {
            end(activePlayers.get(0), surrender ? EndReason.SURRENDER : EndReason.QUIT);
        }
    }

    @Override
    public ArrayList<SJPlayer> getActivePlayers() {
        ArrayList<SJPlayer> active = new ArrayList<>();
        for (SJPlayer sjp : players) {
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
        List<SJPlayer> battlePlayers = getActivePlayers();
        battlePlayers.addAll(spectators);
        for (SJPlayer sjp : SuperJump.getInstance().getPlayerManager().getAll()) {
            hidePlayers(sjp);
        }
    }

    protected void hidePlayers(SJPlayer target) {
        List<SJPlayer> battlePlayers = getActivePlayers();
        battlePlayers.addAll(spectators);
        for (SJPlayer active : battlePlayers) {
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
        createSpawnCages();
        for (SJPlayer sjp : getActivePlayers()) {
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
                    for (SJPlayer sp : getActivePlayers()) {
                        sp.setFrozen(false);
                    }
                    onDone();
                    super.cancel();
                }
            }

            public void onDone() {
                removeSpawnCages();
                for (SJPlayer sp : getActivePlayers()) {
                    sp.setFrozen(false);
                }
                inCountdown = false;
            }
        };
        br.runTaskTimer(SuperJump.getInstance(), 20, 20);
        startClock();
    }

    protected void createSpawnCages() {
        for (FakeBlock block : fakeBlocks.getBlocks()) {
            block.setType(Material.GLASS);
        }
        handler.update(fakeBlocks);
    }

    protected void removeSpawnCages() {
        for (FakeBlock block : fakeBlocks.getBlocks()) {
            block.setType(Material.AIR);
        }
        handler.update(fakeBlocks);
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
        clock.runTaskTimer(SuperJump.getInstance(), 0, 1);
    }

    public void cancel() {
        end(null, EndReason.CANCEL);
    }

    public void end(SJPlayer winner, EndReason reason) {
        saveGameHistory(winner);
        if (reason == BattleEndEvent.EndReason.CANCEL) {
            if (reason == BattleEndEvent.EndReason.CANCEL) {
                ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), Theme.INCOGNITO.buildTheme(false) + "The battle has been cancelled by a moderator.", cc);
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
        SuperJump.getInstance().getBattleManager().remove(this);
        ChatManager.unregisterChannel(cc);
        GameSign.updateGameSigns();
        if (arena.getEndDebugger() != null) {
            RuntimeCompiler.debugFromHastebin(arena.getEndDebugger(), null);
        }
    }

    private void saveGameHistory(SJPlayer winner) {
        GameHistory gh = new GameHistory(this, winner);
        EntityBuilder.save(gh, SuperJump.getInstance().getPluginDB().getCollection("GameHistory"));
    }

    private void resetPlayer(SJPlayer sp) {
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sp.getPlayer());
        handler.removeArea(fakeBlocks, slp.getPlayer());
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
        hidePlayers(sp);
        slp.removeChatChannel(cc);
        slp.setState(PlayerState.IDLE);
        slp.resetVisibility();
    }

    

    public void onArenaLeave(SJPlayer sjp) {
        if (inCountdown) {
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

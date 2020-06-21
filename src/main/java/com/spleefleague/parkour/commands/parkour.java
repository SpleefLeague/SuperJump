/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.commands;

import static com.spleefleague.annotations.CommandSource.COMMAND_BLOCK;
import static com.spleefleague.annotations.CommandSource.CONSOLE;
import static com.spleefleague.annotations.CommandSource.PLAYER;
import com.spleefleague.annotations.Endpoint;
import com.spleefleague.annotations.LiteralArg;
import com.spleefleague.annotations.PlayerArg;
import com.spleefleague.annotations.StringArg;
import com.spleefleague.commands.command.BasicCommand;
import com.spleefleague.gameapi.events.BattleStartEvent.StartReason;
import com.spleefleague.core.player.DBPlayerManager;
import com.spleefleague.core.player.PlayerState;
import com.spleefleague.core.player.Rank;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.plugin.CorePlugin;
import com.spleefleague.gameapi.GamePlugin;
import com.spleefleague.gameapi.queue.Challenge;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.classic.ClassicParkourArena;
import com.spleefleague.parkour.player.ParkourPlayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Jonas
 */
public class parkour extends BasicCommand {

    public parkour(CorePlugin plugin, String name, String usage) {
        super(Parkour.getInstance(), new parkourDispatcher(), name, usage);
    }

    private boolean checkQueuesClosed(CommandSender p) {
        if (!Parkour.getInstance().queuesOpen()) {
            error(p, "All queues are currently paused!");
            return true;
        }
        return false;
    }

    private boolean checkIngame(CommandSender p) {
        if (p instanceof Player && GamePlugin.isIngameGlobal((Player)p)) {
            error(p, "You are currently ingame!");
            return true;
        }
        return false;
    }
    
    @Endpoint(target = {COMMAND_BLOCK})
    public void forceQueueGlobally(CommandSender sender, @LiteralArg("queue") String l, @PlayerArg Player target) {
        if (checkQueuesClosed(sender)) {
            return;
        }
        if (checkIngame(target)) {
            return;
        }
        GamePlugin.dequeueGlobal(target);
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(target);
        Parkour.getInstance().getClassicBattleManager().queue(sjp);
        success(target, "You have been added to the queue");
    }

    @Endpoint(target = {COMMAND_BLOCK})
    public void forceQueueArena(CommandSender sender, @LiteralArg("queue") String l, @PlayerArg Player target, @StringArg String arenaName) {
        if (checkQueuesClosed(sender)) {
            return;
        }
        if (checkIngame(target)) {
            return;
        }
        ClassicParkourArena arena = ClassicParkourArena.byName(arenaName);
        if (arena == null) {
            error(target, "This arena does not exist.");
            return;
        }
        if (arena.isPaused()) {
            error(target, "This arena is currently paused.");
            return;
        }
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(target);
        if (!arena.isAvailable(sjp)) {
            error(target, "You have not visited this arena yet!");
            return;
        }
        Parkour.getInstance().getClassicBattleManager().queue(sjp, arena);
        success(target, "You have been added to the queue for: " + ChatColor.GREEN + arena.getName());
    }

    @Endpoint(target = {COMMAND_BLOCK})
    public void forceQueueSingleArena(CommandSender sender, @LiteralArg("queue") String l, @LiteralArg(value = "single", aliases = {"s"}) String s, @PlayerArg Player target, @StringArg String arenaName) {
        if (checkIngame(sender)) {
            return;
        }
        ClassicParkourArena arena = ClassicParkourArena.byName(arenaName);
        if (arena == null) {
            error(sender, "This arena does not exist.");
            return;
        }
        if (arena.isOccupied()) {
            error(sender, "This arena is currently occupied.");
            return;
        }
        if (arena.isPaused()) {
            error(sender, "This arena is currently paused.");
            return;
        }
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(target);
        if (!arena.isAvailable(sjp)) {
            error(sender, "You have not visited this arena yet!");
            return;
        }
        arena.startSingleplayerBattle(sjp, StartReason.QUEUE);
    }

    @Endpoint(target = {PLAYER})
    public void queueGlobally(Player sender) {
        forceQueueGlobally(sender, "queue", sender);
    }

    @Endpoint(target = {PLAYER})
    public void queueArena(Player sender, @StringArg String arenaName) {
        forceQueueArena(sender, "queue", sender, arenaName);
    }

    @Endpoint(target = {PLAYER, CONSOLE, COMMAND_BLOCK})
    public void forcestart(CommandSender sender, @LiteralArg(value = "match") String l, @StringArg String arenaName, @PlayerArg Player[] players) {
        if (checkIngame(sender)) {
            return;
        }
        if(sender instanceof SLPlayer) {
            SLPlayer slp = (SLPlayer)sender;
            if (!slp.getRank().hasPermission(Rank.MODERATOR) && slp.getRank() != Rank.ORGANIZER) {
                sendUsage(sender);
                return;
            }
        }
        ClassicParkourArena arena = ClassicParkourArena.byName(arenaName);
        if (arena == null) {
            error(sender, "This arena does not exist.");
            return;
        }
        if (arena.isOccupied()) {
            error(sender, "This arena is currently occupied.");
            return;
        }
        if (arena.getRequiredPlayers() > players.length || arena.getSize() < players.length) {
            if (arena.getRequiredPlayers() == arena.getSize()) {
                error(sender, "This arena requires " + arena.getSize() + " players.");
            } else {
                error(sender, "This arena requires between " + arena.getRequiredPlayers() + " and " + arena.getSize() + " players");
            }
            return;
        }
        DBPlayerManager<ParkourPlayer> pm = Parkour.getInstance().getPlayerManager();
        List<ParkourPlayer> sjplayers = Arrays.stream(players)
                .map(pm::get)
                .collect(Collectors.toList());
        arena.startBattle(sjplayers, StartReason.FORCE);
        success(sender, "You started a battle on the arena " + arena.getName());
    }

    @Endpoint(target = {PLAYER})
    public void single(SLPlayer sender, @LiteralArg(value = "single", aliases = {"s"}) String l, @StringArg String arenaName) {
        if (checkIngame(sender)) {
            return;
        }
        ClassicParkourArena arena = ClassicParkourArena.byName(arenaName);
        if (arena == null) {
            error(sender, "This arena does not exist.");
            return;
        }
        if (arena.isOccupied()) {
            error(sender, "This arena is currently occupied.");
            return;
        }
        if (arena.isPaused()) {
            error(sender, "This arena is currently paused.");
            return;
        }
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(sender);
        if (!arena.isAvailable(sjp)) {
            error(sender, "You have not visited this arena yet!");
            return;
        }
        arena.startSingleplayerBattle(sjp, StartReason.QUEUE);
    }

    @Endpoint(target = {PLAYER})
    public void challenge(SLPlayer sender, @LiteralArg(value = "challenge", aliases = {"c"}) String l, @StringArg String arenaName, @PlayerArg Player[] players) {
        handleChallenge(sender, false, arenaName, players);
    }

    @Endpoint(target = {PLAYER})
    public void multichallenge(SLPlayer sender, @LiteralArg(value = "multichallenge", aliases = {"mc"}) String l, @StringArg String arenaName, @PlayerArg Player[] players) {
        handleChallenge(sender, false, arenaName, players);
    }

    public void handleChallenge(SLPlayer sender, boolean isMulti, String arenaName, Player[] players) {
        if (checkQueuesClosed(sender)) {
            return;
        }
        if (checkIngame(sender)) {
            return;
        }
        ClassicParkourArena arena = ClassicParkourArena.byName(arenaName);
        if (arena == null) {
            error(sender, "This arena does not exist.");
            return;
        }
        if (arena.isPaused()) {
            error(sender, "This arena is currently paused.");
            return;
        }
        if (arena.isOccupied()) {
            error(sender, "This arena is currently occupied.");
            return;
        }
        if (arena.getRequiredPlayers() - 1 > players.length || arena.getSize() - 1 < players.length) {
            if (arena.getRequiredPlayers() == arena.getSize()) {
                error(sender, "This arena requires " + arena.getSize() + " players.");
            } else {
                error(sender, "This arena requires between " + arena.getRequiredPlayers() + " and " + arena.getSize() + " players");
            }
            return;
        }
        DBPlayerManager<ParkourPlayer> pm = Parkour.getInstance().getPlayerManager();
        ParkourPlayer sendersjp = pm.get(sender);
        if (sender.getState() == PlayerState.INGAME) {
            error(sender, "You are currently ingame.");
            return;
        }
        if (!arena.isAvailable(sendersjp)) {
            error(sender, "You have not discovered this arena yet.");
        }
        List<ParkourPlayer> challenged = new ArrayList<>();
        for (Player player : players) {
            ParkourPlayer pp = pm.get(player);
            if (pp == sendersjp) {
                error(sender, "You cannot challenge yourself.");
                return;
            }
            if (challenged.contains(pp)) {
                error(sender, "You cannot challenge " + pp.getName() + " more than once.");
                return;
            }
            if (GamePlugin.isIngameGlobal(pp)) {
                error(sender, player.getName() + " is currently ingame.");
                return;
            }
            if (!arena.isAvailable(pp)) {
                error(sender, player.getName() + " has not discovered this arena yet.");
            }
            challenged.add(pp);
        }
        Challenge<ParkourPlayer> challenge = new Challenge<ParkourPlayer>(sendersjp, challenged) {
            @Override
            public void start(List<ParkourPlayer> accepted) {
                arena.startBattle(accepted, StartReason.CHALLENGE);
            }
        };
        challenge.sendMessages(Parkour.getInstance().getChatPrefix(), arena.getName(), Arrays.asList(players));
        success(sender, "The players have been challenged.");
    }

    @Endpoint(target = {PLAYER, CONSOLE})
    public void pause(CommandSender sender, @LiteralArg(value = "pause") String l, @StringArg String arenaName) {
        handlePause(sender, true, arenaName);
    }

    @Endpoint(target = {PLAYER, CONSOLE})
    public void unpause(CommandSender sender, @LiteralArg(value = "unpause") String l, @StringArg String arenaName) {
        handlePause(sender, false, arenaName);
    }

    private void handlePause(CommandSender sender, boolean pauseValue, @StringArg String arenaName) {
        if(sender instanceof SLPlayer) {
            SLPlayer slp = (SLPlayer)sender;
            if (!slp.getRank().hasPermission(Rank.MODERATOR) && slp.getRank() != Rank.ORGANIZER) {
                sendUsage(sender);
                return;
            }
        }
        ClassicParkourArena arena = ClassicParkourArena.byName(arenaName);
        if (arena == null) {
            error(sender, "This arena does not exist.");
            return;
        }
        arena.setPaused(pauseValue);
        if (pauseValue) {
            success(sender, "You have paused the arena " + arena.getName());
        } else {
            success(sender, "You have unpaused the arena " + arena.getName());
        }
    }
}

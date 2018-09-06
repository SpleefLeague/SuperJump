/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.commands;

import com.spleefleague.annotations.CommandSource;
import static com.spleefleague.annotations.CommandSource.COMMAND_BLOCK;
import static com.spleefleague.annotations.CommandSource.CONSOLE;
import static com.spleefleague.annotations.CommandSource.PLAYER;
import com.spleefleague.annotations.Endpoint;
import com.spleefleague.annotations.IntArg;
import com.spleefleague.annotations.LiteralArg;
import com.spleefleague.annotations.PlayerArg;
import com.spleefleague.annotations.StringArg;
import com.spleefleague.commands.command.BasicCommand;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.gameapi.events.BattleStartEvent.StartReason;
import com.spleefleague.core.player.DBPlayerManager;
import com.spleefleague.core.player.PlayerState;
import com.spleefleague.core.player.Rank;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.plugin.CorePlugin;
import com.spleefleague.gameapi.GamePlugin;
import com.spleefleague.gameapi.queue.Challenge;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.Arena;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.game.versus.classic.VersusClassicParkourArena;
import com.spleefleague.parkour.game.endless.EndlessParkourArena;
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
    
    @Endpoint(target = {COMMAND_BLOCK, PLAYER})
    public void resetPlayer(CommandSender sender, @LiteralArg("reset") String l, @PlayerArg Player target) {
        if(sender instanceof Player) {
            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get((Player) sender)
                    , Parkour.getInstance().getChatPrefix()
                    + Parkour.fillColor + " You reset "
                    + Parkour.playerColor + target.getName()
                    + Parkour.fillColor + "'s SuperJump stats.");
        }
        Parkour.getInstance().getPlayerManager().get(target).resetDB();
    }
    
    @Endpoint(target = {COMMAND_BLOCK})
    public void forceQueueVersus(CommandSender sender, @LiteralArg("queue") String l, @PlayerArg Player target) {
        if (checkQueuesClosed(sender)) {
            return;
        }
        if (checkIngame(target)) {
            return;
        }
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(target);
        Parkour.getInstance().queuePlayer(sjp, ParkourMode.CLASSIC, true);
    }
    
    @Endpoint(target = {COMMAND_BLOCK})
    public void forceQueueVersusArena(CommandSender sender, @LiteralArg("queue") String l, @PlayerArg Player target, @StringArg String arenaName) {
        if (checkQueuesClosed(sender)) {
            return;
        }
        if (checkIngame(target)) {
            return;
        }
        VersusClassicParkourArena arena = VersusClassicParkourArena.byName(arenaName);
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
        Parkour.getInstance().queuePlayer(sjp, arena, true);
    }
    
    @Endpoint(target = {COMMAND_BLOCK})
    public void forceQueueEndless(CommandSender sender, @LiteralArg("queue") String l, @PlayerArg Player target) {
        if(checkQueuesClosed(sender)) {
            return;
        }
        if(checkIngame(target)) {
            return;
        }
        if(EndlessParkourArena.isDisabled()) {
            error(target, "Endless is currently disabled for scheduled maintenance (11:55pm - 12:00am PST)");
            return;
        }
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(target);
        Parkour.getInstance().queuePlayer(sjp, ParkourMode.ENDLESS, true);
    }

    @Endpoint(target = {PLAYER})
    public void queueClassic(Player sender, @LiteralArg("classic") String l) {
        forceQueueVersus(sender, "queue", sender);
    }

    @Endpoint(target = {PLAYER})
    public void queueArena(Player sender, @LiteralArg("versus") String l, @StringArg String arenaName) {
        forceQueueVersusArena(sender, "queue", sender, arenaName);
    }
    
    @Endpoint(target = {PLAYER})
    public void queueEndless(Player sender, @LiteralArg("endless") String l) {
        forceQueueEndless(sender, "queue", sender);
    }
    
    @Endpoint(target = {PLAYER})
    public void setEndlessLevel(Player sender, @LiteralArg("endless") String l, @PlayerArg Player player, @IntArg Integer level) {
        if(SpleefLeague.getInstance().getPlayerManager().get(sender).getRank().hasPermission(Rank.DEVELOPER)) {
            Parkour.getInstance().getPlayerManager().get(player).setEndlessLevel(level);
        }
    }
    
    @Endpoint(target = {PLAYER})
    public void getLeaderboard(Player sender, @LiteralArg("leaderboard") String l, @StringArg String arena) {
        Arena a = Parkour.getInstance().getArena(arena);
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sender);
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(sender);
        if (a != null) {
            if (a.hasLeaderboard()) {
                ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                        + Parkour.fillColor + " Top 5 players for "
                        + Parkour.arenaColor + a.getName());
                a.getLeaderboard().forEach(s -> ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix() + " " + s));
            }
            else {
                ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                        + Parkour.arenaColor + " " + a.getName()
                        + Parkour.fillColor + " does not have a Leaderboard.");
            }
        }
        else {
            ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                    + Parkour.arenaColor + " " + arena
                    + Parkour.fillColor + " does not exist.");
        }
    }
    
    @Endpoint(target = {PLAYER})
    public void getStats(Player sender, @LiteralArg("stats") String l, @StringArg String mode) {
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sender);
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(sender);
        mode = mode.toLowerCase();
        if(mode.equals("endless")) {
                ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                        + ChatColor.GREEN + " Your Endless Stats");
                ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                        + ChatColor.GREEN + " Total Falls: "
                        + ChatColor.GOLD + sjp.getTotalEndlessFalls());
                ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                        + ChatColor.GREEN + " Current Level: "
                        + ChatColor.GOLD + sjp.getEndlessLevel());
                ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                        + ChatColor.GREEN + " Best Level: "
                        + ChatColor.GOLD + sjp.getEndlessLevelRecord());
                ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                        + ChatColor.GREEN + " Time Played: "
                        + ChatColor.GOLD + sjp.getEndlessTimeFormatted());
                ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                        + ChatColor.GREEN + " Average Run Time: "
                        + ChatColor.GOLD + "???");
        }
    }
    
    @Endpoint(target = {PLAYER})
    public void getStats(Player sender, @LiteralArg("stats") String l, @StringArg String mode, @PlayerArg Player target) {
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sender);
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(target);
        mode = mode.toLowerCase();
        if(mode.equals("endless")) {
                ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                        + ChatColor.RED + " " + target.getName() + "'s"
                        + ChatColor.GREEN + " Endless Stats");
                ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                        + ChatColor.GREEN + " Total Falls: "
                        + ChatColor.GOLD + sjp.getTotalEndlessFalls());
                ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                        + ChatColor.GREEN + " Current Level: "
                        + ChatColor.GOLD + sjp.getEndlessLevel());
                ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                        + ChatColor.GREEN + " Best Level: "
                        + ChatColor.GOLD + sjp.getEndlessLevelRecord());
                ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                        + ChatColor.GREEN + " Time Played: "
                        + ChatColor.GOLD + sjp.getEndlessTimeFormatted());
                ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                        + ChatColor.GREEN + " Average Run Time: "
                        + ChatColor.GOLD + "???");
        }
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
        VersusClassicParkourArena arena = VersusClassicParkourArena.byName(arenaName);
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
        VersusClassicParkourArena arena = VersusClassicParkourArena.byName(arenaName);
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
        VersusClassicParkourArena arena = VersusClassicParkourArena.byName(arenaName);
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

    @Endpoint(target = {PLAYER})
    public void showModes(Player sender, @LiteralArg("modes") String l) {
        // TODO: Finish this line when ready for release
        success(sender, "Valid modes are: Classic, Endless, TODO");
    }
}

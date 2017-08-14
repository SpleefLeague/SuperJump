/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.commands;

import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.Theme;
import com.spleefleague.core.command.BasicCommand;
import com.spleefleague.core.events.BattleStartEvent.StartReason;
import com.spleefleague.core.io.EntityBuilder;
import com.spleefleague.core.player.PlayerState;
import com.spleefleague.core.player.Rank;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.plugin.CorePlugin;
import com.spleefleague.core.plugin.GamePlugin;
import com.spleefleague.core.queue.BattleManager;
import com.spleefleague.core.queue.Challenge;
import com.spleefleague.superjump.SuperJump;
import com.spleefleague.superjump.game.Arena;
import com.spleefleague.superjump.game.signs.GameSign;
import com.spleefleague.superjump.player.SJPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Jonas
 */
public class superjump extends BasicCommand {

    public superjump(CorePlugin plugin, String name, String usage) {
        super(SuperJump.getInstance(), name, usage);
    }

    @Override
    protected void run(Player p, SLPlayer slp, Command cmd, String[] args) {
        if (SuperJump.getInstance().queuesOpen()) {
            SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(p);
            BattleManager bm = SuperJump.getInstance().getBattleManager();
            if (!GamePlugin.isIngameGlobal(p)) {
                if (args.length == 0) {
                    GamePlugin.dequeueGlobal(p);
                    bm.queue(sjp);
                    success(p, "You have been added to the queue.");
                } else if (args.length == 1) {
                    Arena arena = Arena.byName(args[0]);
                    if (arena != null) {
                        if (!arena.isPaused()) {
                            if (arena.isAvailable(sjp)) {
                                bm.queue(sjp, arena);
                                success(p, "You have been added to the queue for: " + ChatColor.GREEN + arena.getName());
                            } else {
                                error(p, "You have not visited this arena yet!");
                            }
                        } else {
                            error(p, "This arena is currently paused.");
                        }
                    } else {
                        error(p, "This arena does not exist.");
                    }
                } else if (args.length >= 2 && args[0].equalsIgnoreCase("match")) {
                    if (slp.getRank().hasPermission(Rank.MODERATOR) || slp.getRank() == Rank.ORGANIZER) {
                        Arena arena = Arena.byName(args[1]);
                        if (arena != null) {
                            if (!arena.isOccupied()) {
                                if ((args.length - 2) == /*arena.getQueueLength()*/ 2) {
                                    ArrayList<SJPlayer> players = new ArrayList<>();
                                    for (int i = 0; i < args.length - 2; i++) {
                                        Player pl = Bukkit.getPlayer(args[i + 2]);
                                        if (pl != null) {
                                            players.add(SuperJump.getInstance().getPlayerManager().get(pl));
                                        } else {
                                            error(p, "The player " + args[i + 2] + " is currently not online.");
                                            return;
                                        }
                                    }
                                    arena.startBattle(players, StartReason.FORCE);
                                    success(p, "You started a battle on the arena " + arena.getName());
                                } else {
                                    error(p, "You need to list " + (args.length - 2) + " players for this arena.");
                                    return;
                                }
                            } else {
                                error(p, "This arena is currently occupied.");
                                return;
                            }
                        } else {
                            error(p, "This arena does not exist.");
                            return;
                        }
                    } else {
                        sendUsage(p);
                        return;
                    }
                } else if (args.length == 2) {
                    if (slp.getRank().hasPermission(Rank.MODERATOR) || slp.getRank() == Rank.ORGANIZER) {
                        Arena arena = Arena.byName(args[1]);
                        if (arena != null) {
                            if (args[0].equalsIgnoreCase("pause")) {
                                arena.setPaused(true);
                                success(p, "You have paused the arena " + arena.getName());
                            } else if (args[0].equalsIgnoreCase("unpause")) {
                                arena.setPaused(false);
                                success(p, "You have unpaused the arena " + arena.getName());
                            } else {
                                sendUsage(p);
                            }
                            GameSign.updateGameSigns(arena);
                            EntityBuilder.save(arena, SuperJump.getInstance().getPluginDB().getCollection("Arenas"));
                        } else {
                            error(p, "This arena does not exist.");
                        }
                    } else {
                        sendUsage(p);
                    }
                } else if (args.length == 3 && (args[0].equalsIgnoreCase("challenge") || args[0].equalsIgnoreCase("c"))) {
                    challenge(p, slp, sjp, bm, args);
                } else if(args.length >= 4 && (args[0].equalsIgnoreCase("multichallenge") || args[0].equalsIgnoreCase("mc"))) {
                    multichallenge(p, slp, sjp, bm, args);
                }
                else if (args.length > 0 && args[0].equalsIgnoreCase("points") && (slp.getRank() != null && slp.getRank().hasPermission(Rank.SENIOR_MODERATOR) || Collections.singletonList(Rank.MODERATOR).contains(slp.getRank()))) {
                    if (args.length != 4 || !(args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
                        p.sendMessage(plugin.getChatPrefix() + " " + Theme.ERROR.buildTheme(false) + "Correct Usage: ");
                        p.sendMessage(plugin.getChatPrefix() + " " + Theme.INCOGNITO.buildTheme(false) + "/sj points <add|remove> <player> <points>");
                        return;
                    }
                    Player player = Bukkit.getPlayer(args[2]);
                    if (player == null) {
                        error(p, args[2] + " isn't online!");
                        return;
                    }
                    int points;
                    try {
                        points = Integer.valueOf(args[3]);
                    } catch (Exception e) {
                        error(p, "The points value must be a number!");
                        return;
                    }
                    SJPlayer sjPlayer = SuperJump.getInstance().getPlayerManager().get(player);
                    if (args[1].equalsIgnoreCase("add")) {
                        sjPlayer.setRating(sjPlayer.getRating() + points);
                    } else {
                        sjPlayer.setRating(sjPlayer.getRating() - points);
                    }
                    success(p, "You have " + (args[1].equalsIgnoreCase("add") ? "added " : "removed ") + points + " points " + (args[1].equalsIgnoreCase("add") ? "to " : "from ") + player.getName() + "!");
                } else {
                    sendUsage(p);
                }
            } else {
                error(p, "You are currently ingame!");
            }
        } else {
            error(p, "All queues are currently paused!");
        }
    }
    
    private void multichallenge(Player p, SLPlayer slp, SJPlayer sjp, BattleManager bm, String[] args) {
        Arena arena = Arena.byName(args[1]);
        if (arena != null) {
            if (!arena.isAvailable(sjp)) {
                error(p, "You have not discovered this arena");
                return;
            }
            SLPlayer[] players = new SLPlayer[args.length - 2];
            List<SLPlayer> challenged = new ArrayList<>();
            for(int i = 2; i < args.length; ++i) {
                Player t = Bukkit.getPlayer(args[i]);
                if(t == null) {
                    error(p, "The player " + args[i] + " is not online.");
                    return;
                }
                SLPlayer splayer = SpleefLeague.getInstance().getPlayerManager().get(t.getUniqueId());
                if(splayer.getState() == PlayerState.INGAME) {
                    error(p, splayer.getName() + " is currently ingame!");
                    return;
                }
                if(challenged.contains(splayer)) {
                    error(p, "You cannot challenge " + splayer.getName() + " twice!");
                    return;
                }
                SJPlayer sjpt = SuperJump.getInstance().getPlayerManager().get(t);
                if(!arena.isAvailable(sjpt)) {
                    error(p, t.getName() + " has not discovered that arena");
                    return;
                }
                players[i - 2] = splayer;
                challenged.add(splayer);
            }
            Challenge challenge = new Challenge(slp, players) {
                @Override
                public void start(SLPlayer[] accepted) {
                    List<SJPlayer> players = new ArrayList<>();
                    for (SLPlayer slpt : accepted) {
                        players.add(SuperJump.getInstance().getPlayerManager().get(slpt));
                    }
                    arena.startMultiBattle(players, StartReason.CHALLENGE);
                }
            };
            success(p, "The players have been challenged.");
            Collection<Player> bplayers = new ArrayList<>();
            for (SLPlayer slpt : players) {
                slpt.addChallenge(challenge);
                bplayers.add(slpt.getPlayer());
            }
            challenge.sendMessages(SuperJump.getInstance().getChatPrefix(), arena.getName(), bplayers);
        } else {
            error(p, "The arena " + args[1] + " does not exist.");
        }
    }
    
    private void challenge(Player p, SLPlayer slp, SJPlayer sjp, BattleManager bm, String[] args) {
        Arena arena = Arena.byName(args[1]);
        if (arena != null) {
            if (!arena.isAvailable(sjp)) {
                error(p, "You have not discovered this arena");
                return;
            }
            if (args.length - 1 == arena.getSize()) {
                SLPlayer[] players = new SLPlayer[arena.getSize()-1];
                Collection<SLPlayer> challenged = new ArrayList();
                for (int i = 2; i < args.length; i++) {
                    Player t = Bukkit.getPlayer(args[i]);
                    if (t != null) {
                        SLPlayer splayer = SpleefLeague.getInstance().getPlayerManager().get(t.getUniqueId());
                        if (splayer.getState() == PlayerState.INGAME) {
                            error(p, splayer.getName() + " is currently ingame!");
                            return;
                        }
                        SJPlayer spt = SuperJump.getInstance().getPlayerManager().get(t.getUniqueId());
                        if (!arena.isAvailable(spt)) {
                            error(p, spt.getName() + " has not visited this arena yet!");
                            return;
                        }
                        // in case maps get added with > 2 player support
                        if (challenged.contains(splayer)) {
                            error(p, "You cannot challenge " + splayer.getName() + " twice!");
                            return;
                        }
                        players[i-2] = splayer;
                        challenged.add(splayer);
                    } else {
                        error(p, "The player " + args[i] + " is not online.");
                        return;
                    }
                }
                Challenge challenge = new Challenge(slp, players) {
                    @Override
                    public void start(SLPlayer[] accepted) {
                        List<SJPlayer> players = new ArrayList<>();
                        for (SLPlayer slpt : accepted) {
                            players.add(SuperJump.getInstance().getPlayerManager().get(slpt));
                        }
                        arena.startBattle(players, StartReason.CHALLENGE);
                    }
                };
                success(p, "The players have been challenged.");
                Collection<Player> bplayers = new ArrayList<>();
                for (SLPlayer slpt : players) {
                    slpt.addChallenge(challenge);
                    bplayers.add(slpt.getPlayer());
                }
                challenge.sendMessages(SuperJump.getInstance().getChatPrefix(), arena.getName(), bplayers);
            } else {
                error(p, "This arena requires " + arena.getSize() + " players.");
            }
        } else {
            error(p, "The arena " + args[1] + " does not exist.");
        }
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump.commands;

import net.spleefleague.core.command.BasicCommand;
import net.spleefleague.core.player.SLPlayer;
import net.spleefleague.core.plugin.CorePlugin;
import net.spleefleague.core.plugin.GamePlugin;
import net.spleefleague.superjump.SuperJump;
import net.spleefleague.superjump.game.Arena;
import net.spleefleague.superjump.game.BattleManager;
import net.spleefleague.superjump.player.SJPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

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
        SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(p);
        BattleManager bm = SuperJump.getInstance().getBattleManager();
        if(!GamePlugin.isIngameAll(p)) {
            if(args.length == 0) {
                if(!GamePlugin.isQueuedAll(p)) {
                    bm.queue(sjp);
                    success(p, "You have been added to the queue.");
                }
                else {
                    error(p, "You are already in a queue! Enter /leave to leave the queue.");
                }
            }
            else if(args.length == 1) {
                Arena arena = Arena.byName(args[0]);
                if(arena != null) {
                    if (!arena.isPaused()) {
                        if(sjp.getVisitedArenas().contains(arena)) {
                            bm.queue(sjp, arena);
                            success(p, "You have been added to the queue for: " + ChatColor.GREEN + arena.getName());
                        }
                        else {
                            error(p, "You have not visited this arena yet!");
                        }
                    } else {
                        error(p, "This arena is currently paused.");
                    }
                }
                else {
                    error(p, "This arena does not exist.");
                }
            }
        }
        else {
            error(p, "You are currently ingame!");
        }
    }
}
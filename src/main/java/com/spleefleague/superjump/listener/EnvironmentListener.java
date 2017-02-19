/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.listener;

import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.player.PlayerState;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.utils.PlayerUtil;
import com.spleefleague.superjump.SuperJump;
import com.spleefleague.superjump.game.Arena;
import com.spleefleague.superjump.player.SJPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 *
 * @author Jonas
 */
public class EnvironmentListener implements Listener {

    private static Listener instance;

    public static void init() {
        if (instance == null) {
            instance = new EnvironmentListener();
            Bukkit.getPluginManager().registerEvents(instance, SuperJump.getInstance());
        }
    }

    private EnvironmentListener() {

    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(event.getPlayer());
        if (slp != null && slp.getState() == PlayerState.IDLE) {
            SJPlayer sp = SuperJump.getInstance().getPlayerManager().get(event.getPlayer());
            if (sp != null) {
                for (Arena arena : Arena.getAll()) {
                    if (!sp.getVisitedArenas().contains(arena)) {
                        if (arena.getArea() != null && arena.getArea().isInArea(event.getTo())) {
                            sp.getVisitedArenas().add(arena);
                            String title = ChatColor.GREEN + "You have discovered " + ChatColor.RED + arena.getName() + ChatColor.GREEN + "!";
                            String subtitle = ChatColor.GRAY + String.valueOf(sp.getVisitedArenas().size()) + "/" + String.valueOf(Arena.getAll().size()) + ChatColor.GOLD + " SJ arenas found!";
                            PlayerUtil.sendTitle(event.getPlayer(), title, subtitle, 10, 40, 10);
                            event.getPlayer().playSound(event.getTo(), Sound.ENTITY_FIREWORK_BLAST, 1, 0);
                            break;
                        }
                    }
                }
            }
        }
    }
}

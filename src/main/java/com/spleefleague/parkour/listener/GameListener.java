/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.listener;

import com.spleefleague.core.SpleefLeague;
import com.spleefleague.gameapi.events.BattleEndEvent.EndReason;
import com.spleefleague.core.player.Rank;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.utils.Area;
import com.spleefleague.core.utils.PlayerUtil;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.ParkourBattle;
import com.spleefleague.parkour.game.Arena;
import com.spleefleague.parkour.player.ParkourPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 *
 * @author Jonas
 */
public class GameListener implements Listener {

    private static Listener instance;

    public static void init() {
        if (instance == null) {
            instance = new GameListener();
            Bukkit.getPluginManager().registerEvents(instance, Parkour.getInstance());
        }
    }

    private GameListener() {

    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
        if (sjp != null) {
            if (sjp.isFrozen()) {
                Location spawn = sjp.getCurrentBattle().getData(sjp).getSpawn();
                if (spawn.distanceSquared(sjp.getLocation()) > 2) {
                    sjp.teleport(spawn);
                }
            } else if (!sjp.isIngame()) {
                SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(event.getPlayer());
                if (slp != null && !(slp.getRank().hasPermission(Rank.MODERATOR) || slp.getRank() == Rank.ORGANIZER)) {
                    for (Arena arena : Arena.getAll()) {
                        if (arena.getBorders() != null && arena.isTpBackSpectators() && Area.isInAny(sjp.getLocation(), arena.getBorders())) {
                            Location loc = arena.getSpectatorSpawn();
                            if (loc == null) {
                                loc = SpleefLeague.getInstance().getSpawnLocation();
                            }
                            sjp.teleport(loc);
                            break;
                        }
                    }
                }
            } else {
                ParkourBattle battle = Parkour.getInstance().getClassicBattleManager().getBattle(sjp);
                Arena arena = battle.getArena();
                if (!Area.isInAny(sjp.getLocation(), arena.getBorders())) {
                    battle.onArenaLeave(sjp);
                } else if (arena.isLiquidLose() && PlayerUtil.isInWater(event.getPlayer())) {
                    battle.onArenaLeave(sjp);
                } else if (battle.getGoal(sjp).isInArea(sjp.getLocation())) {
                    battle.end(sjp, EndReason.NORMAL);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
        if (sjp == null || sjp.isIngame()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
        if (sjp == null || sjp.isIngame()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
        if (sjp == null || sjp.isIngame()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }
}

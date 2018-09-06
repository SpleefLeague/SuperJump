/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.listener;

import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.player.PlayerState;
import com.spleefleague.gameapi.events.BattleEndEvent.EndReason;
import com.spleefleague.core.player.Rank;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.utils.Area;
import com.spleefleague.core.utils.PlayerUtil;
import com.spleefleague.gameapi.GamePlugin;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.ParkourBattle;
import com.spleefleague.parkour.game.Arena;
import com.spleefleague.parkour.player.ParkourPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

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
                Location from = event.getFrom();
                Location to = event.getTo();
                from.setYaw(to.getYaw());
                from.setPitch(to.getPitch());
                event.setTo(from);
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
                ParkourBattle battle = sjp.getCurrentBattle();
                if(battle != null) {
                    Arena arena = battle.getArena();
                    sjp.setMovedInMatch(true);
                    if (!Area.isInAny(sjp.getLocation(), arena.getBorders())) {
                        battle.onArenaLeave(sjp);
                    } else if (arena.isLiquidLose() && (PlayerUtil.isInLava(event.getPlayer()) || PlayerUtil.isInWater(event.getPlayer()))) {
                        battle.onArenaLeave(sjp);
                    } else if (!GamePlugin.isSpectatingGlobal(event.getPlayer())
                            && battle.getGoal(sjp).isInArea(sjp.getLocation())) {
                        battle.end(sjp, EndReason.NORMAL);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
        if (sjp != null && sjp.isIngame()) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPunch(EntityDamageByEntityEvent event) {
        if(event.getEntity() instanceof Player
                && event.getDamager() instanceof Player) {
            Player player1 = (Player) (event.getDamager());
            Player player2 = (Player) (event.getEntity());
            if(SpleefLeague.getInstance().getPlayerManager().get(player1).getRank().hasPermission(Rank.DEVELOPER)) {
                Vector dir = player1.getLocation().getDirection();
                player2.setVelocity(dir.multiply(25).setY(1));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
        if (sjp != null && sjp.isIngame()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
        if (sjp != null && sjp.isIngame()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHandClick(PlayerInteractEvent event) {
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack is = event.getItem();
            if (is != null) {
                if(sjp.isIngame() && is.equals(ParkourBattle.getItemEndGame())) {
                    Parkour.getInstance().requestEndgame(sjp.getPlayer());
                    event.setCancelled(true);
                }
            }
        }
        if (sjp.getParkourSpectatorTarget() != null) {
            if (event.getAction() == Action.LEFT_CLICK_AIR) {
                sjp.nextParkourSpectatorTarget();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        ItemStack is = event.getCurrentItem();
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getWhoClicked().getUniqueId());
        if(sjp != null && sjp.isIngame() && is != null) {
            if(sjp.isIngame() && is.equals(ParkourBattle.getItemEndGame())) {
                Parkour.getInstance().requestEndgame(sjp.getPlayer());
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(PlayerTeleportEvent event) {
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
        if (sjp != null && Parkour.getInstance().isSpectating(sjp.getPlayer())
                && event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            if (!sjp.isSpectatorLoading()) {
                if (sjp.getParkourSpectatorTarget() != null
                    && sjp.getSpectatorTarget() == null) {
                    sjp.setParkourSpectatorTarget(null);
                }
                else if (sjp.getParkourSpectatorTarget() != (Player) sjp.getSpectatorTarget()) {
                    sjp.setParkourSpectatorTarget((Player) sjp.getSpectatorTarget());
                }
            } else {
                sjp.setSpectatorLoading(false);
            }
        }
    }
}

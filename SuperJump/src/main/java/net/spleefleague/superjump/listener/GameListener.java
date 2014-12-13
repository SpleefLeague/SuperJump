/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump.listener;

import net.spleefleague.core.SpleefLeague;
import net.spleefleague.superjump.SuperJump;
import net.spleefleague.superjump.game.Arena;
import net.spleefleague.superjump.game.Battle;
import net.spleefleague.superjump.player.SJPlayer;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 *
 * @author Jonas
 */
public class GameListener implements Listener{
    
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(event.getPlayer());
        if(sjp.isFrozen()) {
            Location from = event.getFrom();
            Location to = event.getTo();
            from.setY(to.getY());
            from.setYaw(to.getYaw());
            from.setPitch(to.getPitch());
            event.setTo(from);
        }
        else if(!sjp.isIngame()) {
            for(Arena arena : Arena.getAll()) {
                if(arena.getBorder().isInArea(sjp.getPlayer().getLocation())) {
                    Location loc = arena.getSpectatorSpawn();
                    if(loc == null) {
                        loc = SpleefLeague.DEFAULT_WORLD.getSpawnLocation();
                    }
                    sjp.getPlayer().teleport(loc);
                    break;
                }
            }
        }
        else {
            Battle battle = SuperJump.getInstance().getBattleManager().getBattle(sjp);
            battle.onArenaLeave(sjp);
        }
    }
}
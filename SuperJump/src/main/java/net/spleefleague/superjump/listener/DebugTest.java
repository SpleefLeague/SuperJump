/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump.listener;

import net.spleefleague.core.utils.Area;
import net.spleefleague.core.utils.Debugger;
import net.spleefleague.superjump.SuperJump;
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
public class DebugTest implements Debugger, Listener {
    
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(event.getPlayer());
        if(sjp.isIngame()) {
            Battle battle = SuperJump.getInstance().getBattleManager().getBattle(sjp);
            Area goal = battle.getGoal(sjp);
            System.out.println("Low: " + locToString(goal.getLow()));
            System.out.println(sjp.getName() + ": " + locToString(sjp.getPlayer().getLocation()) + " (" + goal.isInArea(sjp.getPlayer().getLocation()) + ")");
            System.out.println("High: " + locToString(goal.getHigh()));
        }
    }

    @Override
    public void debug() {
    
    }
    
    private String locToString(Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
}

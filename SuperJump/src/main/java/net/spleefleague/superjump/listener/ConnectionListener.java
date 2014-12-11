/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump.listener;

import net.spleefleague.superjump.SuperJump;
import net.spleefleague.superjump.player.SJPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author Jonas
 */
public class ConnectionListener implements Listener {
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(event.getPlayer());
        SuperJump.getInstance().getGameQueue().dequeue(sjp);
    } 
}

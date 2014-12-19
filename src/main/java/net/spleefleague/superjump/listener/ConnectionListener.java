/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump.listener;

import net.spleefleague.superjump.SuperJump;
import net.spleefleague.superjump.player.SJPlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author Jonas
 */
public class ConnectionListener implements Listener {
    
    private static Listener instance;
    
    public static void init() {
        if(instance == null) {
            instance = new ConnectionListener();
            Bukkit.getPluginManager().registerEvents(instance, SuperJump.getInstance());
        }
    }
    
    private ConnectionListener() {
        
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(event.getPlayer());
        if(sjp.isIngame()) {
            SuperJump.getInstance().getBattleManager().getBattle(sjp).removePlayer(sjp);
        }
        else {
            SuperJump.getInstance().getBattleManager().dequeue(sjp);
        }
    }
}

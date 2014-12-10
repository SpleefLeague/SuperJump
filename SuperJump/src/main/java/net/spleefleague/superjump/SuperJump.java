/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump;

import com.mongodb.DB;
import net.spleefleague.core.CorePlugin;
import net.spleefleague.core.SpleefLeague;
import net.spleefleague.core.player.PlayerManager;
import net.spleefleague.superjump.player.SJPlayer;
import org.bukkit.ChatColor;

/**
 *
 * @author Jonas
 */
public class SuperJump extends CorePlugin{

    private PlayerManager<SJPlayer> playerManager;
    
    public SuperJump(String prefix, String chatPrefix) {
        super("[SuperJump]", ChatColor.GRAY + "[" + ChatColor.GOLD + "SuperJump" + ChatColor.GRAY + "]" + ChatColor.RESET);
    }
    
    @Override
    public void onEnable() {
        playerManager = new PlayerManager<>(this, SJPlayer.class);
    }

    @Override
    public DB getPluginDB() {
        return SpleefLeague.getInstance().getMongo().getDB("SuperJump");
    }
    
    public PlayerManager<SJPlayer> getPlayerManager() {
        return playerManager;
    }
}
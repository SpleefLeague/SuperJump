/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump;

import com.mongodb.DB;
import net.spleefleague.core.SpleefLeague;
import net.spleefleague.core.player.GeneralPlayer;
import net.spleefleague.core.player.PlayerManager;
import net.spleefleague.core.plugin.QueueableCoreGame;
import net.spleefleague.superjump.game.Arena;
import net.spleefleague.superjump.game.BattleManager;
import net.spleefleague.superjump.listener.ConnectionListener;
import net.spleefleague.superjump.player.SJPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 *
 * @author Jonas
 */
public class SuperJump extends QueueableCoreGame<SJPlayer, Arena>{

    private static SuperJump instance;
    private PlayerManager<SJPlayer> playerManager;
    private BattleManager battleManager;
    
    public SuperJump(String prefix, String chatPrefix) {
        super("[SuperJump]", ChatColor.GRAY + "[" + ChatColor.GOLD + "SuperJump" + ChatColor.GRAY + "]" + ChatColor.RESET);
    }
    
    @Override
    public void start() {
        instance = this;
        playerManager = new PlayerManager<>(this, SJPlayer.class);
        battleManager = new BattleManager(getGameQueue());
        Bukkit.getPluginManager().registerEvents(new ConnectionListener(), this);
    }

    @Override
    public DB getPluginDB() {
        return SpleefLeague.getInstance().getMongo().getDB("SuperJump");
    }
    
    public PlayerManager<SJPlayer> getPlayerManager() {
        return playerManager;
    }
    
    public BattleManager getBattleManager() {
        return battleManager;
    }
    
    public static SuperJump getInstance() {
        return instance;
    }

    @Override
    public boolean isIngame(GeneralPlayer gp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void endGame(GeneralPlayer gp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
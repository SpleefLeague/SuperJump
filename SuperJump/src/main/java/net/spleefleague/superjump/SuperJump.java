/*  
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */ 
package net.spleefleague.superjump;
    
import com.mongodb.DB;
import net.spleefleague.core.SpleefLeague;
import net.spleefleague.core.chat.ChatManager;
import net.spleefleague.core.chat.Theme;
import net.spleefleague.core.command.CommandLoader;
import net.spleefleague.core.player.GeneralPlayer;
import net.spleefleague.core.player.PlayerManager;
import net.spleefleague.core.plugin.QueueableCoreGame;
import net.spleefleague.superjump.game.Arena;
import net.spleefleague.superjump.game.Battle;
import net.spleefleague.superjump.game.BattleManager;
import net.spleefleague.superjump.listener.ConnectionListener;
import net.spleefleague.superjump.listener.GameListener;
import net.spleefleague.superjump.player.SJPlayer;
import org.bukkit.ChatColor;
    
/** 
 *   
 * @author Jonas
 */ 
public class SuperJump extends QueueableCoreGame<SJPlayer, Arena>{
    
    private static SuperJump instance;
    private PlayerManager<SJPlayer> playerManager;
    private BattleManager battleManager;
    
    public SuperJump() {
        super("[SuperJump]", ChatColor.GRAY + "[" + ChatColor.GOLD + "SuperJump" + ChatColor.GRAY + "]" + ChatColor.RESET);
    }
    
    @Override
    public void start() {
        instance = this;
        playerManager = new PlayerManager<>(this, SJPlayer.class);
        battleManager = new BattleManager(getGameQueue());
        ConnectionListener.init();
        GameListener.init();
        CommandLoader.loadCommands(this, "net.spleefleague.superjump.commands");
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
        return battleManager.isIngame(playerManager.get(gp.getPlayer()));
    }
    
    @Override
    public void endGame(GeneralPlayer gp) {
        Battle b = playerManager.get(gp.getPlayer()).getCurrentBattle();
        ChatManager.sendMessage(Theme.SUPER_SECRET + " The battle on " + b.getArena().getName() + " has been cancelled.", "STAFF");
        playerManager.get(gp.getPlayer()).getCurrentBattle().cancel();
    }
}   
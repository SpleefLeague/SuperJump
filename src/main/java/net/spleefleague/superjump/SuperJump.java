/*  
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */ 
package net.spleefleague.superjump;
    
import com.mongodb.client.MongoDatabase;
import java.util.Iterator;
import net.spleefleague.core.SpleefLeague;
import net.spleefleague.core.chat.ChatChannel;
import net.spleefleague.core.chat.ChatManager;
import net.spleefleague.core.chat.Theme;
import net.spleefleague.core.command.CommandLoader;
import net.spleefleague.core.player.PlayerManager;
import net.spleefleague.core.player.Rank;
import net.spleefleague.core.plugin.GamePlugin;
import net.spleefleague.superjump.game.Arena;
import net.spleefleague.superjump.game.Battle;
import net.spleefleague.superjump.game.BattleManager;
import net.spleefleague.superjump.game.signs.GameSign;
import net.spleefleague.superjump.listener.ConnectionListener;
import net.spleefleague.superjump.listener.EnvironmentListener;
import net.spleefleague.superjump.listener.GameListener;
import net.spleefleague.superjump.listener.SignListener;
import net.spleefleague.superjump.player.SJPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
    
/** 
 *   
 * @author Jonas
 */ 
public class SuperJump extends GamePlugin {
    
    private static SuperJump instance;
    private PlayerManager<SJPlayer> playerManager;
    private BattleManager battleManager;
    private boolean queuesOpen = true;
    
    public SuperJump() {
        super("[SuperJump]", ChatColor.GRAY + "[" + ChatColor.GOLD + "SuperJump" + ChatColor.GRAY + "]" + ChatColor.RESET);
    }
    
    @Override
    public void start() {
        instance = this;
        Arena.initialize();
        playerManager = new PlayerManager<>(this, SJPlayer.class);
        battleManager = new BattleManager();
        ChatManager.registerChannel(new ChatChannel("GAME_MESSAGE_JUMP_END", "SuperJump game start notifications", Rank.DEFAULT, true));
        ChatManager.registerChannel(new ChatChannel("GAME_MESSAGE_JUMP_START", "SuperJump game result messages", Rank.DEFAULT, true));
        ConnectionListener.init();
        GameListener.init();
        SignListener.init();
        EnvironmentListener.init();
        GameSign.initialize();
        CommandLoader.loadCommands(this, "net.spleefleague.superjump.commands");
    }
    
    @Override
    public void stop() {
        for(Battle battle : battleManager.getAll()) {
            battle.cancel(false);
        }
        playerManager.saveAll();
    }
    
    @Override
    public MongoDatabase getPluginDB() {
        return SpleefLeague.getInstance().getMongo().getDatabase("SuperJump");
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
    public boolean spectate(Player target, Player p) {
        SJPlayer tsjp = getPlayerManager().get(target);
        SJPlayer sjp = getPlayerManager().get(p);
        if(sjp.getVisitedArenas().contains(tsjp.getCurrentBattle().getArena())) {
            tsjp.getCurrentBattle().addSpectator(sjp);
            return true;
        }
        else {
            p.sendMessage(SuperJump.getInstance().getChatPrefix() + Theme.ERROR.buildTheme(false) + " You can only spectate arenas you have already visited!");
            return false;
        }
    }
    
    @Override
    public void unspectate(Player p) {
        SJPlayer sjp = getPlayerManager().get(p);
        for(Battle battle : getBattleManager().getAll()) {
            if(battle.isSpectating(sjp)) {
                battle.removeSpectator(sjp);
            }
        }
    }
    
    @Override
    public boolean isSpectating(Player p) {
        SJPlayer sjp = getPlayerManager().get(p);
        for(Battle battle : getBattleManager().getAll()) {
            if(battle.isSpectating(sjp)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void dequeue(Player p) {
        SJPlayer sjp = getPlayerManager().get(p);
        getBattleManager().dequeue(sjp);
    }

    @Override
    public void cancel(Player p) {
        SJPlayer sjp = getPlayerManager().get(p);
        Battle battle = getBattleManager().getBattle(sjp);
        if(battle != null) {
            battle.cancel();    
            ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix() + Theme.SUPER_SECRET.buildTheme(false) + " The battle on " + battle.getArena().getName() + " has been cancelled.", "STAFF");
        }
    }

    @Override
    public void surrender(Player p) {
        SJPlayer sjp = getPlayerManager().get(p);
        Battle battle = getBattleManager().getBattle(sjp);
        if(battle != null) {
            for(SJPlayer active : battle.getActivePlayers()) {
                active.sendMessage(SuperJump.getInstance().getChatPrefix() + Theme.SUPER_SECRET.buildTheme(false) + " " + p.getName() + " has surrendered!");
            }
            battle.removePlayer(sjp);
        }
    }

    @Override
    public boolean isQueued(Player p) {
        SJPlayer sjp = getPlayerManager().get(p);
        return getBattleManager().isQueued(sjp);
    }

    @Override
    public boolean isIngame(Player p) {
        SJPlayer sjp = getPlayerManager().get(p);
        return getBattleManager().isIngame(sjp);
     }
   
    @Override
    public void cancelAll() {
        for(Battle battle : battleManager.getAll()) {
            battle.cancel();
        }
    }

    @Override
    public void printStats(Player p) {
        SJPlayer sjp = playerManager.get(p);
        p.sendMessage(Theme.INFO + p.getName() + "'s SuperJump stats");
        p.sendMessage(Theme.INCOGNITO + "Rating: " + ChatColor.YELLOW + sjp.getRating());
        p.sendMessage(Theme.INCOGNITO + "Rank: " + ChatColor.YELLOW + sjp.getRank());
    }
    
    @Override
    public void requestEndgame(Player p) {
        SJPlayer sp = SuperJump.getInstance().getPlayerManager().get(p);
        Battle battle = sp.getCurrentBattle();
        if(battle != null) {
            sp.setRequestingEndgame(true);
            boolean shouldEnd = true;
            for(SJPlayer spleefplayer : battle.getActivePlayers()) {
                if(!spleefplayer.isRequestingEndgame()) {
                    shouldEnd = false;
                    break;
                }
            }
            if(shouldEnd) {
                battle.cancel(false);
            }
            else {
                for(SJPlayer spleefplayer : battle.getActivePlayers()) {
                    if(!spleefplayer.isRequestingEndgame()) {
                        spleefplayer.getPlayer().sendMessage(SuperJump.getInstance().getChatPrefix() + " " + Theme.WARNING.buildTheme(false) + "Your opponent wants to end this game. To agree enter " + ChatColor.YELLOW + "/endgame.");
                    }
                }
                sp.sendMessage(SuperJump.getInstance().getChatPrefix() + " " + Theme.WARNING.buildTheme(false) + "You requested this game to be cancelled.");
            }
        }
    }

    @Override
    public void setQueueStatus(boolean open) {
        queuesOpen = open;
    }
    
    public boolean queuesOpen() {
        return queuesOpen;
    }
}   
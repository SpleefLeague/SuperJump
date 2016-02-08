/*  
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */ 
package com.spleefleague.superjump;
    
import com.mongodb.client.MongoDatabase;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatChannel;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.chat.Theme;
import com.spleefleague.core.command.CommandLoader;
import com.spleefleague.core.events.BattleEndEvent.EndReason;
import com.spleefleague.core.events.BattleStartEvent.StartReason;
import com.spleefleague.core.menus.SLMenu;
import com.spleefleague.core.player.PlayerManager;
import com.spleefleague.core.plugin.GamePlugin;
import static com.spleefleague.core.utils.inventorymenu.InventoryMenuAPI.item;
import com.spleefleague.core.utils.inventorymenu.InventoryMenuTemplateBuilder;
import com.spleefleague.superjump.game.Arena;
import com.spleefleague.superjump.game.Battle;
import com.spleefleague.core.queue.BattleManager;
import com.spleefleague.superjump.game.signs.GameSign;
import com.spleefleague.superjump.listener.ConnectionListener;
import com.spleefleague.superjump.listener.EnvironmentListener;
import com.spleefleague.superjump.listener.GameListener;
import com.spleefleague.superjump.listener.SignListener;
import com.spleefleague.superjump.player.SJPlayer;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

    
/** 
 *   
 * @author Jonas
 */ 
public class SuperJump extends GamePlugin {
    
    private static SuperJump instance;
    
    private PlayerManager<SJPlayer> playerManager;
    private BattleManager<Arena, SJPlayer, Battle> battleManager;
    private boolean queuesOpen = true;
    private ChatChannel start, end;
    
    public SuperJump() {
        super("[SuperJump]", ChatColor.GRAY + "[" + ChatColor.GOLD + "SuperJump" + ChatColor.GRAY + "]" + ChatColor.RESET);
    }
    
    @Override
    public void start() {
        instance = this;
        playerManager = new PlayerManager<>(this, SJPlayer.class);
        battleManager = new BattleManager<Arena, SJPlayer, Battle>() {
            @Override
            public void startBattle(Arena queue, List<SJPlayer> players) {
                queue.startBattle(players, StartReason.QUEUE);
            }
        };
        Arena.init();
        createGameMenu();
        start = ChatChannel.valueOf("GAME_MESSAGE_SUPERJUMP_START");
        end = ChatChannel.valueOf("GAME_MESSAGE_SUPERJUMP_END");
        ConnectionListener.init();
        GameListener.init();
        SignListener.init();
        EnvironmentListener.init();
        GameSign.initialize();
        CommandLoader.loadCommands(this, "com.spleefleague.superjump.commands");
    }
    
    @Override
    public void stop() {
        for(Battle battle : battleManager.getAll()) {
            battle.cancel();
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
    
    public BattleManager<Arena, SJPlayer, Battle> getBattleManager() {
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
            ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix() + Theme.SUPER_SECRET.buildTheme(false) + " The battle on " + battle.getArena().getName() + " has been cancelled.", ChatChannel.STAFF_NOTIFICATIONS);
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
            battle.removePlayer(sjp, true);
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
        for(Battle battle : new ArrayList<>(battleManager.getAll())) {
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
                battle.end(null, EndReason.ENDGAME);
            }
            else {
                for(SJPlayer spleefplayer : battle.getActivePlayers()) {
                    if(!spleefplayer.isRequestingEndgame()) {
                        spleefplayer.sendMessage(SuperJump.getInstance().getChatPrefix() + " " + Theme.WARNING.buildTheme(false) + "Your opponent wants to end this game. To agree enter " + ChatColor.YELLOW + "/endgame.");
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
    
    public ChatChannel getStartMessageChannel() {
        return start;
    }
    
    public ChatChannel getEndMessageChannel() {
        return end;
    }
    
    private void createGameMenu() {
        InventoryMenuTemplateBuilder menu = SLMenu.getNewGamemodeMenu()
                .displayName("SuperJump")
                .displayIcon(Material.LEATHER_BOOTS)
                .exitOnClickOutside(true)
                .visibilityController((slp) -> (queuesOpen));
        Arena.getAll().stream().forEach((arena) -> {
            menu.component(item()
                    .displayName(arena.getName())
                    .description(arena.getDynamicDescription())
                    .displayIcon((slp) -> (arena.isAvailable(playerManager.get(slp)) ? Material.MAP : Material.EMPTY_MAP))
                    .onClick((event) -> {
                        SJPlayer sp = getPlayerManager().get(event.getPlayer());
                        if (arena.isAvailable(sp)) {
                            if (arena.isOccupied()) {
                                battleManager.getBattle(arena).addSpectator(sp);
                            }
                            else {
                                if (!arena.isPaused()) {
                                    battleManager.queue(sp, arena);
                                    event.getItem().getParent().update();
                                }
                            }
                        }
                    })
            );
        });
    }
}   
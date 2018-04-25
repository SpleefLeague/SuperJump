/*  
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour;

import com.mongodb.client.MongoDatabase;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatChannel;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.chat.Theme;
import com.spleefleague.commands.command.CommandLoader;
import com.spleefleague.gameapi.events.BattleEndEvent.EndReason;
import com.spleefleague.gameapi.events.BattleStartEvent.StartReason;
import com.spleefleague.core.menus.SLMenu;
import com.spleefleague.core.player.DBPlayerManager;
import com.spleefleague.gameapi.GamePlugin;
import com.spleefleague.core.plugin.PlayerHandling;
import com.spleefleague.gameapi.queue.BattleManager;
import com.spleefleague.gameapi.queue.RatedBattleManager;
import com.spleefleague.core.utils.inventorymenu.InventoryMenuTemplateBuilder;
import com.spleefleague.parkour.game.Arena;
import com.spleefleague.parkour.listener.ConnectionListener;
import com.spleefleague.parkour.listener.EnvironmentListener;
import com.spleefleague.parkour.listener.GameListener;
import com.spleefleague.parkour.player.ParkourPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;

import static com.spleefleague.core.utils.inventorymenu.InventoryMenuAPI.item;
import com.spleefleague.core.utils.inventorymenu.InventoryMenuFlag;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.parkour.game.ParkourBattle;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.game.classic.ClassicParkourArena;
import com.spleefleague.parkour.game.classic.ClassicParkourBattle;
import java.util.StringJoiner;

/**
 *
 * @author Jonas
 */
public class Parkour extends GamePlugin implements PlayerHandling {

    private static Parkour instance;

    private DBPlayerManager<ParkourPlayer> playerManager;
    private RatedBattleManager<ClassicParkourArena, ParkourPlayer, ClassicParkourBattle> battleManagerClassic;
    private boolean queuesOpen = true;
    private ChatChannel start, end;

    public Parkour() {
        super(ChatColor.GRAY + "[" + ChatColor.GOLD + "Parkour" + ChatColor.GRAY + "]" + ChatColor.RESET);
    }

    @Override
    public void start() {
        instance = this;
        playerManager = new DBPlayerManager<>(this, ParkourPlayer.class);
        this.battleManagerClassic = new RatedBattleManager<>(
                m -> m.getQueue().startBattle(m.getPlayers(), StartReason.QUEUE), 
                sp -> sp.getRating(ParkourMode.CLASSIC)
        );
        Arena.init();
        createGameMenu();
        start = ChatChannel.valueOf("GAME_MESSAGE_SUPERJUMP_START");
        end = ChatChannel.valueOf("GAME_MESSAGE_SUPERJUMP_END");
        ConnectionListener.init();
        GameListener.init();
        EnvironmentListener.init();
        CommandLoader.loadCommands(this, "com.spleefleague.parkour.commands");
    }

    @Override
    public void stop() {
        battleManagerClassic.getAll().forEach(ParkourBattle::cancel);
        playerManager.saveAll();
    }

    @Override
    public MongoDatabase getPluginDB() {
        return SpleefLeague.getInstance().getMongo().getDatabase("SuperJump");//Compatibility
    }

    public DBPlayerManager<ParkourPlayer> getPlayerManager() {
        return playerManager;
    }

    public BattleManager<ClassicParkourArena, ParkourPlayer, ClassicParkourBattle> getClassicBattleManager() {
        return battleManagerClassic;
    }

    public static Parkour getInstance() {
        return instance;
    }

    @Override
    public boolean spectate(Player target, Player p) {
        ParkourPlayer tsjp = getPlayerManager().get(target);
        ParkourPlayer sjp = getPlayerManager().get(p);
        if (tsjp.getCurrentBattle().getArena().isAvailable(sjp)) {
            tsjp.getCurrentBattle().addSpectator(sjp);
            return true;
        } else {
            p.sendMessage(Parkour.getInstance().getChatPrefix() + Theme.ERROR.buildTheme(false) + " You can only spectate arenas you have already visited!");
            return false;
        }
    }

    @Override
    public void unspectate(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        getClassicBattleManager().getAll().stream().filter(b -> b.isSpectating(sjp)).forEach(b -> b.removeSpectator(sjp));
    }

    @Override
    public boolean isSpectating(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        return getClassicBattleManager().getAll().stream().anyMatch(b -> b.isSpectating(sjp));
    }

    @Override
    public void dequeue(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        getClassicBattleManager().dequeue(sjp);
    }

    @Override
    public void cancel(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        ParkourBattle battle = getClassicBattleManager().getBattle(sjp);
        if (battle != null) {
            battle.cancel();
            ChatManager.sendMessage(Parkour.getInstance().getChatPrefix() + Theme.SUPER_SECRET.buildTheme(false) + " The battle on " + battle.getArena().getName() + " has been cancelled.", ChatChannel.STAFF_NOTIFICATIONS);
        }
    }

    @Override
    public void surrender(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        ParkourBattle<?> battle = getClassicBattleManager().getBattle(sjp);
        if (battle != null) {
            for (ParkourPlayer active : battle.getActivePlayers()) {
                active.sendMessage(Parkour.getInstance().getChatPrefix() + Theme.SUPER_SECRET.buildTheme(false) + " " + p.getName() + " has surrendered!");
            }
            battle.removePlayer(sjp, true);
        }
    }

    @Override
    public boolean isQueued(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        return getClassicBattleManager().isQueued(sjp);
    }

    @Override
    public boolean isIngame(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        return getClassicBattleManager().isIngame(sjp);
    }

    @Override
    public void cancelAll() {
        new ArrayList<>(battleManagerClassic.getAll()).forEach(ParkourBattle::cancel);
    }
    
    @Override
    public void printStats(Player p, Player target) {
        ParkourPlayer sjp = playerManager.get(target);
        StringJoiner sj = new StringJoiner("\n");
        sj.add(Theme.INFO + sjp.getName() + "'s Parkour stats");
        boolean print = false;
        for (ParkourMode mode : ParkourMode.values()) {
            print = true;
            int rank = sjp.getRank(mode);
            if(rank == -1) continue;
            int rating = sjp.getRating(mode);
            sj.add(mode.getChatPrefix() + ChatColor.GRAY + " Rating: " + ChatColor.YELLOW + rating + ChatColor.GRAY + " (" + ChatColor.GOLD + "#" + rank + ChatColor.GRAY + ")");
        }
        if(print) {
            p.sendMessage(sj.toString());
        }
    }

    @Override
    public void requestEndgame(Player p) {
        ParkourPlayer sp = Parkour.getInstance().getPlayerManager().get(p);
        ParkourBattle<?> battle = sp.getCurrentBattle();
        if (battle != null) {
            sp.setRequestingEndgame(true);
            boolean shouldEnd = true;
            for (ParkourPlayer spleefplayer : battle.getActivePlayers()) {
                if (!spleefplayer.isRequestingEndgame()) {
                    shouldEnd = false;
                    break;
                }
            }
            if (shouldEnd) {
                battle.end(null, EndReason.ENDGAME);
            } else {
                for (ParkourPlayer spleefplayer : battle.getActivePlayers()) {
                    if (!spleefplayer.isRequestingEndgame()) {
                        spleefplayer.sendMessage(Parkour.getInstance().getChatPrefix() + " " + Theme.WARNING.buildTheme(false) + "Your opponent wants to end this game. To agree enter " + ChatColor.YELLOW + "/endgame.");
                    }
                }
                sp.sendMessage(Parkour.getInstance().getChatPrefix() + " " + Theme.WARNING.buildTheme(false) + "You requested this game to be cancelled.");
            }
        }
    }

    @Override
    public void setQueueStatus(boolean open) {
        queuesOpen = open;
    }

    @Override
    public void syncSave(Player p) {
        ParkourPlayer slp = playerManager.get(p);
        if (slp != null) {
            EntityBuilder.save(slp, getPluginDB().getCollection("Players"));
        }
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
                .flags(InventoryMenuFlag.EXIT_ON_CLICK_OUTSIDE)
                .visibilityController((slp) -> (queuesOpen));
        ClassicParkourArena.getAll().stream().forEach(arena -> {
            menu.component(item()
                    .displayName(arena.getName())
                    .description(arena.getDynamicDescription())
                    .displayIcon((slp) -> (arena.isAvailable(playerManager.get(slp)) ? Material.MAP : Material.EMPTY_MAP))
                    .onClick((event) -> {
                        ParkourPlayer sp = getPlayerManager().get(event.getPlayer());
                        if (arena.isAvailable(sp)) {
                            if (arena.isOccupied()) {
                                battleManagerClassic.getBattle(arena).addSpectator(sp);
                            } else if (!arena.isPaused()) {
                                battleManagerClassic.queue(sp, arena);
                                event.getItem().getParent().update();
                            }
                        }
                    })
            );
        });
    }

    @Override
    public BattleManager<? extends Arena, ParkourPlayer, ? extends ParkourBattle>[] getBattleManagers() {
        return new BattleManager[]{battleManagerClassic};
    }
}

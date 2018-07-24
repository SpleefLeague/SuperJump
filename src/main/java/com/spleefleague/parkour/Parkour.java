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
import com.spleefleague.core.player.SLPlayer;
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
import com.spleefleague.parkour.game.versus.classic.VersusClassicParkourArena;
import com.spleefleague.parkour.game.versus.classic.VersusClassicParkourBattle;
import com.spleefleague.parkour.game.conquest.ConquestParkourArena;
import com.spleefleague.parkour.game.conquest.ConquestParkourBattle;
import com.spleefleague.parkour.game.endless.EndlessParkourArena;
import com.spleefleague.parkour.game.endless.EndlessParkourBattle;
import com.spleefleague.parkour.game.party.PartyParkourArena;
import com.spleefleague.parkour.game.party.PartyParkourBattle;
import com.spleefleague.parkour.game.practice.PracticeParkourArena;
import com.spleefleague.parkour.game.practice.PracticeParkourBattle;
import com.spleefleague.parkour.game.pro.ProParkourArena;
import com.spleefleague.parkour.game.pro.ProParkourBattle;
import com.spleefleague.parkour.game.versus.random.VersusRandomParkourArena;
import com.spleefleague.parkour.game.versus.random.VersusRandomParkourBattle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Jonas
 */
public class Parkour extends GamePlugin implements PlayerHandling {

    private static Parkour instance;

    private DBPlayerManager<ParkourPlayer> playerManager;
    private Map<ParkourMode, RatedBattleManager> battleManagers = new HashMap<>();
    private boolean queuesOpen = true;
    private ChatChannel start, end;
    private ItemStack itemEndGame;

    public Parkour() {
        super(ChatColor.GRAY + "[" + ChatColor.GOLD + "SuperJump" + ChatColor.GRAY + "]" + ChatColor.RESET);
    }

    @Override
    public void start() {
        instance = this;
        playerManager = new DBPlayerManager<>(this, ParkourPlayer.class);
        battleManagers.put(ParkourMode.CLASSIC, new RatedBattleManager<VersusClassicParkourArena, ParkourPlayer, VersusClassicParkourBattle>(
                m -> {
                    m.getQueue().startBattle(m.getPlayers(), StartReason.QUEUE);
                    m.getPlayers().forEach(slp -> slp.setParkourMode(ParkourMode.CLASSIC));
                }, 
                sp -> sp.getRating(ParkourMode.CLASSIC)
        ));
        battleManagers.put(ParkourMode.RANDOM, new RatedBattleManager<VersusRandomParkourArena, ParkourPlayer, VersusRandomParkourBattle>(
                m -> {
                    m.getQueue().startBattle(m.getPlayers(), StartReason.QUEUE);
                    m.getPlayers().forEach(slp -> slp.setParkourMode(ParkourMode.RANDOM));
                }, 
                sp -> sp.getRating(ParkourMode.RANDOM)
        ));
        battleManagers.put(ParkourMode.CONQUEST, new RatedBattleManager<ConquestParkourArena, ParkourPlayer, ConquestParkourBattle>(
                m -> {
                    m.getQueue().startBattle(m.getPlayers(), StartReason.QUEUE);
                    m.getPlayers().forEach(slp -> slp.setParkourMode(ParkourMode.CONQUEST));
                }, 
                sp -> sp.getRating(ParkourMode.CONQUEST)
        ));
        battleManagers.put(ParkourMode.ENDLESS, new RatedBattleManager<EndlessParkourArena, ParkourPlayer, EndlessParkourBattle>(
                m -> {
                    m.getQueue().startBattle(m.getPlayers(), StartReason.QUEUE);
                    m.getPlayers().forEach(slp -> slp.setParkourMode(ParkourMode.ENDLESS));
                }, 
                sp -> sp.getRating(ParkourMode.ENDLESS)
        ));
        battleManagers.put(ParkourMode.PARTY, new RatedBattleManager<PartyParkourArena, ParkourPlayer, PartyParkourBattle>(
                m -> {
                    m.getQueue().startBattle(m.getPlayers(), StartReason.QUEUE);
                    m.getPlayers().forEach(slp -> slp.setParkourMode(ParkourMode.PARTY));
                }, 
                sp -> sp.getRating(ParkourMode.PARTY)
        ));
        battleManagers.put(ParkourMode.PRACTICE, new RatedBattleManager<PracticeParkourArena, ParkourPlayer, PracticeParkourBattle>(
                m -> {
                    m.getQueue().startBattle(m.getPlayers(), StartReason.QUEUE);
                    m.getPlayers().forEach(slp -> slp.setParkourMode(ParkourMode.PRACTICE));
                }, 
                sp -> sp.getRating(ParkourMode.PRACTICE)
        ));
        battleManagers.put(ParkourMode.PRO, new RatedBattleManager<ProParkourArena, ParkourPlayer, ProParkourBattle>(
                m -> {
                    m.getQueue().startBattle(m.getPlayers(), StartReason.QUEUE);
                    m.getPlayers().forEach(slp -> slp.setParkourMode(ParkourMode.PRO));
                }, 
                sp -> sp.getRating(ParkourMode.PRO)
        ));
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
        for(Map.Entry<ParkourMode, RatedBattleManager> entry : battleManagers.entrySet()) {
            switch(entry.getKey()) {
                case CLASSIC:
                    ((BattleManager<VersusClassicParkourArena, ParkourPlayer, VersusClassicParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case RANDOM:
                    ((BattleManager<VersusRandomParkourArena, ParkourPlayer, VersusRandomParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case CONQUEST:
                    ((BattleManager<ConquestParkourArena, ParkourPlayer, ConquestParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case ENDLESS:
                    ((BattleManager<EndlessParkourArena, ParkourPlayer, EndlessParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case PARTY:
                    ((BattleManager<PartyParkourArena, ParkourPlayer, PartyParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case PRACTICE:
                    ((BattleManager<PracticeParkourArena, ParkourPlayer, PracticeParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case PRO:
                    ((BattleManager<ProParkourArena, ParkourPlayer, ProParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                default: {} break;
            }
        }
        playerManager.saveAll();
        Arena.terminate();
    }

    @Override
    public MongoDatabase getPluginDB() {
        return SpleefLeague.getInstance().getMongo().getDatabase("SuperJump");//Compatibility
    }

    public DBPlayerManager<ParkourPlayer> getPlayerManager() {
        return playerManager;
    }

    public BattleManager getBattleManager(ParkourMode mode) {
        return battleManagers.get(mode);
    }
    
    public Map<ParkourMode, ? extends RatedBattleManager> getParkourBattleManagers() {
        return battleManagers;
    }
    
    public ParkourBattle getBattle(ParkourPlayer sjp) {
        ParkourBattle battle;
        switch(sjp.getParkourMode()) {
            case CLASSIC:
                battle = ((BattleManager<VersusClassicParkourArena, ParkourPlayer, VersusClassicParkourBattle>)(battleManagers.get(ParkourMode.CLASSIC))).getBattle(sjp);
                break;
            case RANDOM:
                battle = ((BattleManager<VersusRandomParkourArena, ParkourPlayer, VersusRandomParkourBattle>)(battleManagers.get(ParkourMode.RANDOM))).getBattle(sjp);
                break;
            case CONQUEST:
                battle = ((BattleManager<ConquestParkourArena, ParkourPlayer, ConquestParkourBattle>)(battleManagers.get(ParkourMode.CONQUEST))).getBattle(sjp);
                break;
            case ENDLESS:
                battle = ((BattleManager<EndlessParkourArena, ParkourPlayer, EndlessParkourBattle>)(battleManagers.get(ParkourMode.ENDLESS))).getBattle(sjp);
                break;
            case PARTY:
                battle = ((BattleManager<PartyParkourArena, ParkourPlayer, PartyParkourBattle>)(battleManagers.get(ParkourMode.PARTY))).getBattle(sjp);
                break;
            case PRACTICE:
                battle = ((BattleManager<PracticeParkourArena, ParkourPlayer, PracticeParkourBattle>)(battleManagers.get(ParkourMode.PRACTICE))).getBattle(sjp);
                break;
            case PRO:
                battle = ((BattleManager<ProParkourArena, ParkourPlayer, ProParkourBattle>)(battleManagers.get(ParkourMode.PRO))).getBattle(sjp);
                break;
            default:
                battle = null;
                break;
        }
        return battle;
    }

    public static Parkour getInstance() {
        return instance;
    }

    @Override
    public boolean spectate(Player target, Player p) {
        /*
        ParkourPlayer tsjp = getPlayerManager().get(target);
        ParkourPlayer sjp = getPlayerManager().get(p);
        if (tsjp.getCurrentBattle().getArena().isAvailable(sjp)) {
            tsjp.getCurrentBattle().addSpectator(sjp);
            return true;
        } else {
            p.sendMessage(Parkour.getInstance().getChatPrefix() + Theme.ERROR.buildTheme(false) + " You can only spectate arenas you have already visited!");
            return false;
        }
        */
        return false;
    }

    @Override
    public void unspectate(Player p) {
        /*
        ParkourPlayer sjp = getPlayerManager().get(p);
        getClassicBattleManager().getAll().stream().filter(b -> b.isSpectating(sjp)).forEach(b -> b.removeSpectator(sjp));
        */
    }

    @Override
    public boolean isSpectating(Player p) {
        /*
        ParkourPlayer sjp = getPlayerManager().get(p);
        return getClassicBattleManager().getAll().stream().anyMatch(b -> b.isSpectating(sjp));
        */
        return false;
    }

    @Override
    public void dequeue(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        for(Map.Entry<ParkourMode, RatedBattleManager> entry : battleManagers.entrySet()) {
            switch(entry.getKey()) {
                case CLASSIC:
                    BattleManager<VersusClassicParkourArena, ParkourPlayer, VersusClassicParkourBattle> classic = (BattleManager<VersusClassicParkourArena, ParkourPlayer, VersusClassicParkourBattle>) entry.getValue();
                    classic.dequeue(sjp);
                    break;
                case CONQUEST:
                    BattleManager<ConquestParkourArena, ParkourPlayer, ConquestParkourBattle> conquest = (BattleManager<ConquestParkourArena, ParkourPlayer, ConquestParkourBattle>) entry.getValue();
                    conquest.dequeue(sjp);
                    break;
                case ENDLESS:
                    BattleManager<EndlessParkourArena, ParkourPlayer, EndlessParkourBattle> endless = (BattleManager<EndlessParkourArena, ParkourPlayer, EndlessParkourBattle>) entry.getValue();
                    endless.dequeue(sjp);
                    break;
                case PARTY:
                    BattleManager<PartyParkourArena, ParkourPlayer, PartyParkourBattle> party = (BattleManager<PartyParkourArena, ParkourPlayer, PartyParkourBattle>) entry.getValue();
                    party.dequeue(sjp);
                    break;
                case PRACTICE:
                    BattleManager<PracticeParkourArena, ParkourPlayer, PracticeParkourBattle> practice = (BattleManager<PracticeParkourArena, ParkourPlayer, PracticeParkourBattle>) entry.getValue();
                    practice.dequeue(sjp);
                    break;
                case PRO:
                    BattleManager<ProParkourArena, ParkourPlayer, ProParkourBattle> pro = (BattleManager<ProParkourArena, ParkourPlayer, ProParkourBattle>) entry.getValue();
                    pro.dequeue(sjp);
                    break;
                case RANDOM:
                    BattleManager<VersusRandomParkourArena, ParkourPlayer, VersusRandomParkourBattle> random = (BattleManager<VersusRandomParkourArena, ParkourPlayer, VersusRandomParkourBattle>) entry.getValue();
                    random.dequeue(sjp);
                    break;
                default: break;
            }
        }
    }
    
    public ParkourBattle<?> getParkourBattle(ParkourPlayer sjp) {
        for(Map.Entry<ParkourMode, RatedBattleManager> entry : battleManagers.entrySet()) {
            if(entry.getKey() != sjp.getParkourMode()) continue;
            switch(entry.getKey()) {
                case CLASSIC:
                    return((BattleManager<VersusClassicParkourArena, ParkourPlayer, VersusClassicParkourBattle>) entry.getValue()).getBattle(sjp);
                case CONQUEST:
                    return((BattleManager<ConquestParkourArena, ParkourPlayer, ConquestParkourBattle>) entry.getValue()).getBattle(sjp);
                case ENDLESS:
                    return((BattleManager<EndlessParkourArena, ParkourPlayer, EndlessParkourBattle>) entry.getValue()).getBattle(sjp);
                case PARTY:
                    return((BattleManager<PartyParkourArena, ParkourPlayer, PartyParkourBattle>) entry.getValue()).getBattle(sjp);
                case PRACTICE:
                    return((BattleManager<PracticeParkourArena, ParkourPlayer, PracticeParkourBattle>) entry.getValue()).getBattle(sjp);
                case PRO:
                    return((BattleManager<ProParkourArena, ParkourPlayer, ProParkourBattle>) entry.getValue()).getBattle(sjp);
                case RANDOM:
                    return((BattleManager<VersusRandomParkourArena, ParkourPlayer, VersusRandomParkourBattle>) entry.getValue()).getBattle(sjp);
                default: break;
            }
        }
        return null;
    }

    @Override
    public void cancel(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        ParkourBattle<?> battle = getParkourBattle(sjp);
        if (battle != null) {
            battle.cancel();
            ChatManager.sendMessage(Parkour.getInstance().getChatPrefix() + Theme.SUPER_SECRET.buildTheme(false) + " The battle on " + battle.getArena().getName() + " has been cancelled.", ChatChannel.STAFF_NOTIFICATIONS);
            sjp.setParkourMode(ParkourMode.NONE);
        }
    }

    @Override
    public void surrender(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        ParkourBattle<?> battle = getParkourBattle(sjp);
        if (battle != null) {
            for (ParkourPlayer active : battle.getActivePlayers()) {
                active.sendMessage(Parkour.getInstance().getChatPrefix() + Theme.SUPER_SECRET.buildTheme(false) + " " + p.getName() + " has surrendered!");
            }
            battle.removePlayer(sjp, true);
        }
    }

    @Override
    public boolean isQueued(Player p) {
        for(Map.Entry<ParkourMode, RatedBattleManager> entry : battleManagers.entrySet())
            if(entry.getValue().isQueued(getPlayerManager().get(p))) return true;
        return false;
    }

    @Override
    public boolean isIngame(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        return sjp.getParkourMode() != ParkourMode.NONE;
    }

    @Override
    public void cancelAll() {
        for(Map.Entry<ParkourMode, RatedBattleManager> entry : battleManagers.entrySet()) {
            switch(entry.getKey()) {
                case CLASSIC:
                    ((BattleManager<VersusClassicParkourArena, ParkourPlayer, VersusClassicParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case CONQUEST:
                    ((BattleManager<ConquestParkourArena, ParkourPlayer, ConquestParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case ENDLESS:
                    ((BattleManager<EndlessParkourArena, ParkourPlayer, EndlessParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case PARTY:
                    ((BattleManager<PartyParkourArena, ParkourPlayer, PartyParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case PRACTICE:
                    ((BattleManager<PracticeParkourArena, ParkourPlayer, PracticeParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case PRO:
                    ((BattleManager<ProParkourArena, ParkourPlayer, ProParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case RANDOM:
                    ((BattleManager<VersusRandomParkourArena, ParkourPlayer, VersusRandomParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                default: break;
            }
        }
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
    
    private Function<SLPlayer, List<String>> getSoloDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.GRAY + "Relax and play a variety of different parkour gametypes in");
            description.add(ChatColor.GRAY + "this singleplayer take on Superjump.");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Currently Playing: " + ChatColor.GOLD 
                    + (this.getBattleManager(ParkourMode.ENDLESS).getAll().size()));
            return description;
        };
    }
    
    private Function<SLPlayer, List<String>> getVersusDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.GRAY + "Clash against other players in a race to the finish line");
            description.add(ChatColor.GRAY + "in this competitive take on SuperJump.");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Currently Playing: " + ChatColor.GOLD 
                    + (this.getBattleManager(ParkourMode.CLASSIC).getAll().size() 
                            + this.getBattleManager(ParkourMode.RANDOM).getAll().size()));
            return description;
        };
    }
    
    private Function<SLPlayer, List<String>> getClassicDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.GRAY + "Play against another player on a preset field of preset jumps.");
            description.add(ChatColor.GRAY + "The first to reach the finish line wins!");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Currently Playing: " + ChatColor.GOLD 
                    + (this.getBattleManager(ParkourMode.CLASSIC).getAll().size()));
            return description;
        };
    }
    
    private Function<SLPlayer, List<String>> getRandomDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.GRAY + "Compete against another player on a field of randomly");
            description.add(ChatColor.GRAY + "generated jumps of four different difficulties.");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Currently Playing: " + ChatColor.GOLD 
                    + (this.getBattleManager(ParkourMode.RANDOM).getAll().size()));
            return description;
        };
    }
    
    private Function<SLPlayer, List<String>> getEndlessDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.GRAY + "In this singleplayer mode, move on endlessly through");
            description.add(ChatColor.GRAY + "random, progressively more challenging levels!");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Currently Playing: " + ChatColor.GOLD 
                    + (this.getBattleManager(ParkourMode.ENDLESS).getAll().size()));
            return description;
        };
    }
    
    private Function<SLPlayer, List<String>> getSuperJumpDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.GRAY + "Jump and run your way to the finish line in this");
            description.add(ChatColor.GRAY + "fast paced parkour gamemode.");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Currently Playing: " + ChatColor.GOLD 
                    + (this.getBattleManager(ParkourMode.CLASSIC).getAll().size()
                            + this.getBattleManager(ParkourMode.CONQUEST).getAll().size()
                            + this.getBattleManager(ParkourMode.ENDLESS).getAll().size()
                            + this.getBattleManager(ParkourMode.PARTY).getAll().size()
                            + this.getBattleManager(ParkourMode.PRACTICE).getAll().size()
                            + this.getBattleManager(ParkourMode.PRO).getAll().size()
                            + this.getBattleManager(ParkourMode.RANDOM).getAll().size()));
            return description;
        };
    }
    
    private void addGameMenuMode(InventoryMenuTemplateBuilder menu, ParkourMode parkourMode) {
        switch(parkourMode) {
            case CLASSIC:
                menu.description(getClassicDescription());
                VersusClassicParkourArena.getAll().stream().forEach(arena -> {
                    menu.component(arena.getMenuPos(), item()
                            .displayName(ChatColor.WHITE + "" + ChatColor.BOLD + arena.getName() + " " + arena.getDifficultyStars())
                            .description(arena.getDynamicDescription())
                            .displayIcon((slp) -> (arena.isAvailable(playerManager.get(slp)) ? Material.MAP : Material.EMPTY_MAP))
                            .onClick((event) -> {
                                ParkourPlayer sjp = getPlayerManager().get(event.getPlayer());
                                if (arena.isAvailable(sjp)) {
                                    if (arena.isOccupied()) {
                                        ((VersusClassicParkourBattle)this.getBattleManager(ParkourMode.CLASSIC).getBattle(arena)).addSpectator(sjp);
                                    } else if (!arena.isPaused()) {
                                        this.getBattleManager(ParkourMode.CLASSIC).queue(sjp, arena);
                                        event.getItem().getParent().update();
                                        ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(event.getPlayer())
                                                , Parkour.getInstance().getChatPrefix()
                                                + Theme.SUCCESS.buildTheme(false) + " You have been added to the queue for Classic: " + arena.getName());
                                    }
                                }
                            })
                    );
                });
                break;
            case RANDOM:
                menu.description(getRandomDescription());
                VersusRandomParkourArena.getAll().stream().forEach(arena -> {
                    menu.component(arena.getMenuPos(), item()
                            .displayName(ChatColor.WHITE + "" + ChatColor.BOLD + arena.getName() + " " + arena.getDifficultyStars())
                            .description(arena.getDynamicDescription())
                            .displayItem(arena.getMenuItem())
                            .onClick((event) -> {
                                ParkourPlayer sjp = getPlayerManager().get(event.getPlayer());
                                if (arena.isAvailable(sjp)) {
                                    if (arena.isOccupied()) {
                                        ((VersusRandomParkourBattle)this.getBattleManager(ParkourMode.RANDOM).getBattle(arena)).addSpectator(sjp);
                                    } else if (!arena.isPaused()) {
                                        this.getBattleManager(ParkourMode.RANDOM).queue(sjp, arena);
                                        event.getItem().getParent().update();
                                        ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(event.getPlayer())
                                                , Parkour.getInstance().getChatPrefix()
                                                + Theme.SUCCESS.buildTheme(false) + " You have been added to the queue for Random: " + arena.getName());
                                    }
                                }
                            })
                    );
                });
                break;
            default: break;
        }
    }

    private void createGameMenu() {
        InventoryMenuTemplateBuilder menu = SLMenu.getNewGamemodeMenu()
                .title("SuperJump")
                .displayName(ChatColor.GREEN + "" + ChatColor.BOLD + "SuperJump")
                .description(getSuperJumpDescription())
                .displayIcon(Material.DIAMOND_BOOTS)
                .flags(InventoryMenuFlag.EXIT_ON_CLICK_OUTSIDE)
                .visibilityController((slp) -> (queuesOpen));
        InventoryMenuTemplateBuilder menuVersus = new InventoryMenuTemplateBuilder()
                .title("SJ Versus")
                .displayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Versus")
                .description(getVersusDescription())
                .displayIcon(Material.ARMOR_STAND)
                .flags(InventoryMenuFlag.EXIT_ON_CLICK_OUTSIDE);
        InventoryMenuTemplateBuilder menuVersusClassic = new InventoryMenuTemplateBuilder()
                .title("SJ Classic")
                .displayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Classic")
                .description(getClassicDescription())
                .displayIcon(Material.IRON_BOOTS)
                .flags(InventoryMenuFlag.EXIT_ON_CLICK_OUTSIDE);
        InventoryMenuTemplateBuilder menuVersusRandom = new InventoryMenuTemplateBuilder()
                .title("SJ Random")
                .displayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Random")
                .description(getRandomDescription())
                .displayIcon(Material.GOLD_BOOTS)
                .flags(InventoryMenuFlag.EXIT_ON_CLICK_OUTSIDE);
        InventoryMenuTemplateBuilder menuSolo = new InventoryMenuTemplateBuilder()
                .title("SJ Solo")
                .displayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Solo")
                .description(getSoloDescription())
                .displayIcon(Material.BED)
                .flags(InventoryMenuFlag.EXIT_ON_CLICK_OUTSIDE);
        EndlessParkourArena.getAll().stream().forEach(arena -> {
            menuSolo.component(item()
                    .displayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + arena.getName())
                    .description(getEndlessDescription())
                    .displayIcon(Material.EYE_OF_ENDER)
                    .onClick((event) -> {
                        ParkourPlayer sjp = getPlayerManager().get(event.getPlayer());
                        if(EndlessParkourArena.isDisabled()) {
                            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(event.getPlayer())
                                    , Parkour.getInstance().getChatPrefix()
                                    + Theme.ERROR.buildTheme(false) + " Endless is currently disabled for scheduled maintenance (11:55pm - 12:00am PST)");
                        }
                        else {
                            this.getBattleManager(ParkourMode.ENDLESS).queue(sjp, arena);
                            event.getItem().getParent().update();
                            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(event.getPlayer())
                                    , Parkour.getInstance().getChatPrefix()
                                    + Theme.SUCCESS.buildTheme(false) + " You have been added to the queue for Endless");
                        }
                    })
            );
        });
        addGameMenuMode(menuVersusClassic, ParkourMode.CLASSIC);
        addGameMenuMode(menuVersusRandom, ParkourMode.RANDOM);
        menuVersus.component(3, menuVersusClassic);
        menuVersus.component(5, menuVersusRandom);
        menu.component(3, menuVersus);
        menu.component(5, menuSolo);
    }

    @Override
    public BattleManager<? extends Arena, ParkourPlayer, ? extends ParkourBattle>[] getBattleManagers() {
        return ((BattleManager<? extends Arena, ParkourPlayer, ? extends ParkourBattle>[])(battleManagers.values().toArray()));
    }
}

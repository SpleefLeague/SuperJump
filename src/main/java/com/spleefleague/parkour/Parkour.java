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
import com.spleefleague.parkour.game.Arena;
import com.spleefleague.parkour.listener.ConnectionListener;
import com.spleefleague.parkour.listener.EnvironmentListener;
import com.spleefleague.parkour.listener.GameListener;
import com.spleefleague.parkour.player.ParkourPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.parkour.game.ParkourBattle;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.game.versus.classic.VersusClassicParkourArena;
import com.spleefleague.parkour.game.versus.classic.VersusClassicParkourBattle;
import com.spleefleague.parkour.game.conquest.ConquestParkourArena;
import com.spleefleague.parkour.game.conquest.ConquestParkourBattle;
import com.spleefleague.parkour.game.endless.EndlessParkourArena;
import com.spleefleague.parkour.game.endless.EndlessParkourBattle;
import com.spleefleague.parkour.game.memory.MemoryParkourArena;
import com.spleefleague.parkour.game.memory.MemoryParkourBattle;
import com.spleefleague.parkour.game.party.PartyParkourArena;
import com.spleefleague.parkour.game.party.PartyParkourBattle;
import com.spleefleague.parkour.game.practice.PracticeParkourArena;
import com.spleefleague.parkour.game.practice.PracticeParkourBattle;
import com.spleefleague.parkour.game.pro.ProParkourArena;
import com.spleefleague.parkour.game.pro.ProParkourBattle;
import com.spleefleague.parkour.game.versus.shuffle.VersusShuffleParkourArena;
import com.spleefleague.parkour.game.versus.shuffle.VersusShuffleParkourBattle;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import com.spleefleague.parkour.menu.ParkourMenu;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author Jonas
 */
public class Parkour extends GamePlugin implements PlayerHandling {

    private static Parkour instance;

    private DBPlayerManager<ParkourPlayer> playerManager;
    private Map<ParkourMode, RatedBattleManager> battleManagers;
    private boolean queuesOpen = true;
    private ChatChannel start, end;
    
    public static final String modeColor = ChatColor.GOLD + "";
    public static final String fillColor = ChatColor.GRAY + "";
    public static final String arenaColor = ChatColor.RED + "";
    public static final String pointColor = ChatColor.YELLOW + "";
    public static final String timeColor = ChatColor.YELLOW + "";
    public static final String levelColor = ChatColor.YELLOW + "";
    public static final String playerColor = ChatColor.RED + "";
    public static final String shopColor = ChatColor.AQUA + "";

    public Parkour() {
        super(ChatColor.GRAY + "[" + ChatColor.GOLD + "SuperJump" + ChatColor.GRAY + "]" + ChatColor.RESET);
        this.battleManagers = new HashMap<>();
    }

    @Override
    public void start() {
        instance = this;
        playerManager = new DBPlayerManager<>(this, ParkourPlayer.class);
        battleManagers.put(ParkourMode.CLASSIC, new RatedBattleManager<VersusClassicParkourArena, ParkourPlayer, VersusClassicParkourBattle>(
                m -> {
                    boolean isRandom = true;
                    for (ParkourPlayer sjp : m.getPlayers()) {
                        if (!sjp.isRandomQueued()) {
                            isRandom = false;
                            break;
                        }
                    }
                    if (isRandom) {
                        startRandomBattle(m.getPlayers());
                    }
                    else {
                        m.getQueue().startBattle(m.getPlayers(), StartReason.QUEUE);
                        m.getPlayers().forEach(slp -> slp.setParkourMode(ParkourMode.CLASSIC));
                    }
                }, 
                sp -> sp.getRating(ParkourMode.CLASSIC)
        ));
        battleManagers.put(ParkourMode.SHUFFLE, new RatedBattleManager<VersusShuffleParkourArena, ParkourPlayer, VersusShuffleParkourBattle>(
                m -> {
                    boolean isRandom = true;
                    for (ParkourPlayer sjp : m.getPlayers()) {
                        if (!sjp.isRandomQueued()) {
                            isRandom = false;
                            break;
                        }
                    }
                    if (isRandom) {
                        startRandomBattle(m.getPlayers());
                    }
                    else {
                        m.getQueue().startBattle(m.getPlayers(), StartReason.QUEUE);
                        m.getPlayers().forEach(slp -> slp.setParkourMode(ParkourMode.SHUFFLE));
                    }
                }, 
                sp -> sp.getRating(ParkourMode.SHUFFLE)
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
        battleManagers.put(ParkourMode.MEMORY, new RatedBattleManager<MemoryParkourArena, ParkourPlayer, MemoryParkourBattle>(
                m -> {
                    m.getQueue().startBattle(m.getPlayers(), StartReason.QUEUE);
                    m.getPlayers().forEach(slp -> slp.setParkourMode(ParkourMode.MEMORY));
                }, 
                sp -> sp.getRating(ParkourMode.MEMORY)
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
                case SHUFFLE:
                    ((BattleManager<VersusShuffleParkourArena, ParkourPlayer, VersusShuffleParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case CONQUEST:
                    ((BattleManager<ConquestParkourArena, ParkourPlayer, ConquestParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case ENDLESS:
                    ((BattleManager<EndlessParkourArena, ParkourPlayer, EndlessParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
                    break;
                case MEMORY:
                    ((BattleManager<MemoryParkourArena, ParkourPlayer, MemoryParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
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
            case SHUFFLE:
                battle = ((BattleManager<VersusShuffleParkourArena, ParkourPlayer, VersusShuffleParkourBattle>)(battleManagers.get(ParkourMode.SHUFFLE))).getBattle(sjp);
                break;
            case CONQUEST:
                battle = ((BattleManager<ConquestParkourArena, ParkourPlayer, ConquestParkourBattle>)(battleManagers.get(ParkourMode.CONQUEST))).getBattle(sjp);
                break;
            case ENDLESS:
                battle = ((BattleManager<EndlessParkourArena, ParkourPlayer, EndlessParkourBattle>)(battleManagers.get(ParkourMode.ENDLESS))).getBattle(sjp);
                break;
            case MEMORY:
                battle = ((BattleManager<MemoryParkourArena, ParkourPlayer, MemoryParkourBattle>)(battleManagers.get(ParkourMode.MEMORY))).getBattle(sjp);
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
        ParkourPlayer tsjp = getPlayerManager().get(target);
        ParkourPlayer sjp = getPlayerManager().get(p);
        if(tsjp.getCurrentBattle() != null) {
            tsjp.getCurrentBattle().addSpectator(sjp, tsjp);
            return true;
        }
        return false;
    }

    @Override
    public void unspectate(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        if (sjp.getParkourMode().equals(ParkourMode.NONE)
                || sjp.getParkourMode().equals(ParkourMode.REQUEUE)) {
            return;
        }
        getBattleManager(sjp.getParkourMode()).getAll().forEach(b -> {
            ParkourBattle pb = (ParkourBattle) b;
            if (pb.isSpectating(sjp)) {
                pb.removeSpectator(sjp);
            }
        });
    }

    @Override
    public boolean isSpectating(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        if (sjp.getParkourMode().equals(ParkourMode.NONE)
                || sjp.getParkourMode().equals(ParkourMode.REQUEUE)) {
            return false;
        }
        for (Object o : getBattleManager(sjp.getParkourMode()).getAll()) {
            if (((ParkourBattle) o).isSpectating(sjp)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void dequeue(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        for(Map.Entry<ParkourMode, RatedBattleManager> entry : battleManagers.entrySet()) {
            switch(entry.getKey()) {
                case CLASSIC:
                    ((BattleManager<VersusClassicParkourArena, ParkourPlayer, VersusClassicParkourBattle>) entry.getValue()).dequeue(sjp);
                    break;
                case CONQUEST:
                    ((BattleManager<ConquestParkourArena, ParkourPlayer, ConquestParkourBattle>) entry.getValue()).dequeue(sjp);
                    break;
                case ENDLESS:
                    ((BattleManager<EndlessParkourArena, ParkourPlayer, EndlessParkourBattle>) entry.getValue()).dequeue(sjp);
                    break;
                case MEMORY:
                    ((BattleManager<MemoryParkourArena, ParkourPlayer, MemoryParkourBattle>) entry.getValue()).dequeue(sjp);
                    break;
                case PARTY:
                    ((BattleManager<PartyParkourArena, ParkourPlayer, PartyParkourBattle>) entry.getValue()).dequeue(sjp);
                    break;
                case PRACTICE:
                    ((BattleManager<PracticeParkourArena, ParkourPlayer, PracticeParkourBattle>) entry.getValue()).dequeue(sjp);
                    break;
                case PRO:
                    ((BattleManager<ProParkourArena, ParkourPlayer, ProParkourBattle>) entry.getValue()).dequeue(sjp);
                    break;
                case SHUFFLE:
                    ((BattleManager<VersusShuffleParkourArena, ParkourPlayer, VersusShuffleParkourBattle>) entry.getValue()).dequeue(sjp);
                    break;
                default: break;
            }
        }
        if (sjp.getParkourMode() == ParkourMode.REQUEUE) {
            sjp.setParkourMode(ParkourMode.NONE);
        }
    }
    
    public ParkourBattle<?> getParkourBattle(ParkourPlayer sjp) {
        if (sjp.getParkourMode() != ParkourMode.NONE && sjp.getParkourMode() != ParkourMode.REQUEUE) {
            for(Map.Entry<ParkourMode, RatedBattleManager> entry : battleManagers.entrySet()) {
                if(entry.getKey() != sjp.getParkourMode()) continue;
                switch(entry.getKey()) {
                    case CLASSIC:
                        return((BattleManager<VersusClassicParkourArena, ParkourPlayer, VersusClassicParkourBattle>) entry.getValue()).getBattle(sjp);
                    case CONQUEST:
                        return((BattleManager<ConquestParkourArena, ParkourPlayer, ConquestParkourBattle>) entry.getValue()).getBattle(sjp);
                    case ENDLESS:
                        return((BattleManager<EndlessParkourArena, ParkourPlayer, EndlessParkourBattle>) entry.getValue()).getBattle(sjp);
                    case MEMORY:
                        return((BattleManager<MemoryParkourArena, ParkourPlayer, MemoryParkourBattle>) entry.getValue()).getBattle(sjp);
                    case PARTY:
                        return((BattleManager<PartyParkourArena, ParkourPlayer, PartyParkourBattle>) entry.getValue()).getBattle(sjp);
                    case PRACTICE:
                        return((BattleManager<PracticeParkourArena, ParkourPlayer, PracticeParkourBattle>) entry.getValue()).getBattle(sjp);
                    case PRO:
                        return((BattleManager<ProParkourArena, ParkourPlayer, ProParkourBattle>) entry.getValue()).getBattle(sjp);
                    case SHUFFLE:
                        return((BattleManager<VersusShuffleParkourArena, ParkourPlayer, VersusShuffleParkourBattle>) entry.getValue()).getBattle(sjp);
                    default: break;
                }
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
        return (getPlayerManager().get(p).getParkourMode() == ParkourMode.REQUEUE);
    }

    @Override
    public boolean isIngame(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        return (sjp != null && !(sjp.getParkourMode().equals(ParkourMode.NONE)));
    }
    
    public String getPlayerName(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        return (Parkour.playerColor + sjp.getName()
                + Parkour.fillColor + " ("
                + Parkour.pointColor + sjp.getParagonLevel()
                + Parkour.fillColor + ")");
    }
    
    public List<String> getPlayerDescription(Player p) {
        ParkourPlayer sjp = getPlayerManager().get(p);
        List<String> description = new ArrayList<>();
        ParkourBattle battle = sjp.getCurrentBattle();
        description.add("");
        description.add(Parkour.fillColor + "GameMode: " + Parkour.modeColor + "SuperJump");
        if (sjp.isIngame()) {
            description.add(Parkour.fillColor + "Arena: " + Parkour.arenaColor + battle.getArena().getName());
            if (sjp.getParkourMode() == ParkourMode.ENDLESS) {
                EndlessParkourBattle ebattle = (EndlessParkourBattle) battle;
                description.add(Parkour.fillColor + "Level: " + Parkour.levelColor + ebattle.getLevel());
            }
            else {
                String opponent = sjp.getOpponentFormatted();
                if (!opponent.equals("")) {
                    description.add(Parkour.fillColor + "Players: " + Parkour.playerColor + opponent);
                }
            }
            description.add(Parkour.fillColor + "Time: " + Parkour.timeColor + sjp.getCurrentBattle().getTimeString());
        }
        return description;
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
                case MEMORY:
                    ((BattleManager<MemoryParkourArena, ParkourPlayer, MemoryParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
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
                case SHUFFLE:
                    ((BattleManager<VersusShuffleParkourArena, ParkourPlayer, VersusShuffleParkourBattle>) entry.getValue()).getAll().forEach(ParkourBattle::cancel);
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
            if(!sp.isRequestingEndgame()) {
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
                        if (!spleefplayer.isRequestingEndgame() && !spleefplayer.equals(sp)) {
                            spleefplayer.sendMessage(Parkour.getInstance().getChatPrefix() + " " + Theme.WARNING.buildTheme(false) + "Your opponent wants to end this game. To agree enter " + ChatColor.YELLOW + "/endgame.");
                        }
                    }
                    sp.sendMessage(Parkour.getInstance().getChatPrefix() + " " + Theme.WARNING.buildTheme(false) + "You requested this game to be cancelled.");
                }
            }
            else {
                sp.setRequestingEndgame(false);
                for (ParkourPlayer spleefplayer : battle.getActivePlayers()) {
                    if (!spleefplayer.isRequestingEndgame() && !spleefplayer.equals(sp)) {
                        spleefplayer.sendMessage(Parkour.getInstance().getChatPrefix() + " " + Theme.WARNING.buildTheme(false) + "Your opponent no longer wants to end this game." + ChatColor.YELLOW + "/endgame.");
                    }
                }
                sp.sendMessage(Parkour.getInstance().getChatPrefix() + " " + Theme.WARNING.buildTheme(false) + "You are no longer requesting to end this game.");
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
    
    public void startRandomBattle(List<ParkourPlayer> players) {
        Set<Arena> arenas = new HashSet<>();
        for (ParkourPlayer sjp : players) {
            for (Arena arena : sjp.getLastArenas()) {
                arenas.add(arena);
            }
        }
        Random random = new Random(System.currentTimeMillis());
        Arena arena = (Arena) arenas.toArray()[random.nextInt(arenas.size())];
        players.forEach(sjp -> sjp.setParkourMode(arena.getParkourMode()));
        arena.startBattle(players, StartReason.QUEUE);
    }
    
    public void dequeuePlayer(ParkourPlayer sjp) {
        this.battleManagers.forEach((mode, manager) -> {
            manager.dequeue(sjp);
        });
        sjp.stopRequeue();
        sjp.clearLastArenas();
    }
    
    public void queuePlayerSilent(ParkourPlayer sjp, Arena arena, boolean dequeue) {
        if(arena.getParkourMode().equals(ParkourMode.ENDLESS) && EndlessParkourArena.isDisabled()) {
            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer())
                    , Parkour.getInstance().getChatPrefix()
                        + arenaColor + " Endless"
                        + fillColor + " is currently disabled for scheduled maintenance "
                        + timeColor + "(11:55pm - 12:00am PST)"
                        + fillColor + ".");
            return;
        }
        if (dequeue) {
            dequeuePlayer(sjp);
        }
        this.battleManagers.get(arena.getParkourMode()).queue(sjp, arena, dequeue);
        sjp.setRandomQueued(false);
        sjp.addLastArena(arena);
    }
    
    public void queuePlayer(ParkourPlayer sjp, Arena arena, boolean dequeue) {
        queuePlayerSilent(sjp, arena, dequeue);
        ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer())
                , Parkour.getInstance().getChatPrefix()
                        + fillColor + " Queued for "
                        + arenaColor + arena.getName()
                        + fillColor +  "."
                        + fillColor + " Type `/leave` to leave the queue.");
    }
    
    public void queuePlayerRandom(ParkourPlayer sjp) {
        dequeuePlayer(sjp);
        VersusClassicParkourArena.getAll().forEach(arena -> {
            if(!sjp.getVersusRandomBlacklist().contains(arena)) {
                this.battleManagers.get(arena.getParkourMode()).queue(sjp, arena, false);
                sjp.addLastArena(arena);
            }
        });
        VersusShuffleParkourArena.getAll().forEach(arena -> {
            if(!sjp.getVersusRandomBlacklist().contains(arena)) {
                this.battleManagers.get(arena.getParkourMode()).queue(sjp, arena, false);
                sjp.addLastArena(arena);
            }
        });
        ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer())
                , Parkour.getInstance().getChatPrefix()
                        + fillColor + " Queued for all "
                        + arenaColor + "Versus"
                        + fillColor + " maps."
                        + fillColor + " Type `/leave` to leave the queue.");
        sjp.setRandomQueued(true);
    }
    
    public void queuePlayer(ParkourPlayer sjp, ParkourMode mode, boolean dequeue) {
        if(mode.equals(ParkourMode.ENDLESS) && EndlessParkourArena.isDisabled()) {
            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer())
                    , Parkour.getInstance().getChatPrefix()
                        + arenaColor + " Endless"
                        + fillColor + " is currently disabled for scheduled maintenance "
                        + timeColor + "(11:55pm - 12:00am PST)"
                        + fillColor + ".");
            return;
        }
        if (dequeue) {
            dequeuePlayer(sjp);
            
        }
        this.battleManagers.get(mode).queue(sjp);
        ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer())
                , Parkour.getInstance().getChatPrefix()
                    + fillColor + " Queued for "
                    + modeColor + mode.getName()
                    + fillColor +  ".");
    }
    
    public void requeuePlayer(ParkourPlayer sjp) {
        if (SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer()).isRequeueing()) {
            if (sjp.getMovedLastMatch()) {
                ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer())
                        , Parkour.getInstance().getChatPrefix()
                            + fillColor + " You will be requeued in "
                            + timeColor + "5"
                            + fillColor + " seconds.");
                sjp.startRequeue();
            }
            else {
                ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer())
                        , Parkour.getInstance().getChatPrefix()
                            + fillColor + " You didn't move last game and have been removed from the queue.");
            }
        }
    }

    private void createGameMenu() {
        ParkourMenu.createSuperJumpMenu(SLMenu.getMenuBuilder());
    }
    
    public Arena getArena(String name) {
        for (ConquestParkourArena arena : ConquestParkourArena.getAll()) {
            if (arena.getName().equalsIgnoreCase(name)) {
                return arena;
            }
        }
        for (EndlessParkourArena arena : EndlessParkourArena.getAll()) {
            if (arena.getName().equalsIgnoreCase(name)) {
                return arena;
            }
        }
        for (PartyParkourArena arena : PartyParkourArena.getAll()) {
            if (arena.getName().equalsIgnoreCase(name)) {
                return arena;
            }
        }
        for (PracticeParkourArena arena : PracticeParkourArena.getAll()) {
            if (arena.getName().equalsIgnoreCase(name)) {
                return arena;
            }
        }
        for (ProParkourArena arena : ProParkourArena.getAll()) {
            if (arena.getName().equalsIgnoreCase(name)) {
                return arena;
            }
        }
        for (VersusClassicParkourArena arena : VersusClassicParkourArena.getAll()) {
            if (arena.getName().equalsIgnoreCase(name)) {
                return arena;
            }
        }
        for (VersusShuffleParkourArena arena : VersusShuffleParkourArena.getAll()) {
            if (arena.getName().equalsIgnoreCase(name)) {
                return arena;
            }
        }
        return null;
    }

    @Override
    public BattleManager<? extends Arena, ParkourPlayer, ? extends ParkourBattle>[] getBattleManagers() {
        return new BattleManager[]{ 
            battleManagers.get(ParkourMode.CLASSIC),
            battleManagers.get(ParkourMode.CONQUEST),
            battleManagers.get(ParkourMode.ENDLESS),
            battleManagers.get(ParkourMode.MEMORY),
            battleManagers.get(ParkourMode.PARTY),
            battleManagers.get(ParkourMode.PRACTICE),
            battleManagers.get(ParkourMode.PRO),
            battleManagers.get(ParkourMode.SHUFFLE)
        };
    }
}

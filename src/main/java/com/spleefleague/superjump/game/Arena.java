/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.game;

import com.mongodb.client.MongoCursor;
import com.spleefleague.core.events.BattleStartEvent.StartReason;
import com.spleefleague.core.io.DBEntity;
import com.spleefleague.core.io.DBLoad;
import com.spleefleague.core.io.DBLoadable;
import com.spleefleague.core.io.DBSave;
import com.spleefleague.core.io.DBSaveable;
import com.spleefleague.core.io.EntityBuilder;
import com.spleefleague.core.io.TypeConverter;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.queue.QueueableArena;
import com.spleefleague.core.utils.Area;
import com.spleefleague.core.utils.function.Dynamic;
import com.spleefleague.superjump.SuperJump;
import com.spleefleague.superjump.player.SJPlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;


/**
 *
 * @author Jonas
 */
public class Arena extends DBEntity implements DBLoadable, DBSaveable, QueueableArena<SJPlayer>{
    
    @DBLoad(fieldName = "border")
    private Area[] borders;
    @DBLoad(fieldName = "spawns", typeConverter = TypeConverter.LocationConverter.class)
    private Location[] spawns;
    @DBLoad(fieldName = "goals")
    private Area[] goals;
    @DBLoad(fieldName = "creator")
    private String creator;
    @DBLoad(fieldName = "name")
    private String name;
    @DBLoad(fieldName = "tpBackSpectators")
    private boolean tpBackSpectators = true;
    @DBLoad(fieldName = "rated")
    private boolean rated;
    @DBLoad(fieldName = "queued")
    private boolean queued;
    @DBLoad(fieldName = "liquidLose")
    private boolean liquidLose = true;
    @DBLoad(fieldName = "spectatorSpawn", typeConverter = TypeConverter.LocationConverter.class)
    private Location spectatorSpawn;
    @DBLoad(fieldName = "paused")
    @DBSave(fieldName = "paused")
    private boolean paused = false;
    @DBLoad(fieldName = "area")
    private Area area;
    @DBLoad(fieldName = "debuggerStart")
    private String debuggerStart;
    @DBLoad(fieldName = "debuggerEnd")
    private String debuggerEnd;
    private int runningGames = 0;
    
    public Location[] getSpawns() {
        return spawns;
    }
    
    public Area[] getGoals() {
        return goals;
    }
    
    public Area getArea() {
        return area;
    }
    
    public Area[] getBorders() {
        return borders;
    }
    
    public Location getSpectatorSpawn() {
        return spectatorSpawn;
    }
    
    public String getCreator() {
        return creator;
    }
    
    public String getStartDebugger() {
        return debuggerStart;
    }
    
    public String getEndDebugger() {
        return debuggerEnd;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean isOccupied() {
        return false;
    }
    
    public int getRunningGamesCount() {
        return runningGames;
    }
    
    public void registerGameStart() {
        runningGames++;
    }
    
    public void registerGameEnd() {
        runningGames--;
    }
    
    public boolean isRated() {
        return rated;
    }
    
    public boolean isTpBackSpectators() {
        return tpBackSpectators;
    }
    
    @Override
    public boolean isPaused() {
        return paused;
    }
    
    public void setPaused(boolean paused) {
        this.paused = paused;
    }
    
    public boolean isLiquidLose() {
        return liquidLose;
    }
    
    @Override
    public int getSize() {
        return getSpawns().length;
    }
    
    public Battle startBattle(List<SJPlayer> players, StartReason reason) {
        if(!isOccupied()) {
            Battle battle = new Battle(this, players);
            battle.start(reason);
            return battle;
        }
        return null;
    }

    @Override
    public boolean isQueued() {
        return queued;
    }

    @Override
    public boolean isAvailable(SJPlayer sjp) {
        return sjp.getVisitedArenas().contains(this);
    }
    
    public Dynamic<List<String>> getDynamicDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(slp.getUniqueId());
            if(Arena.this.isAvailable(sjp)) {
                if(Arena.this.isPaused()) {
                    description.add(ChatColor.RED + "This arena is");
                    description.add(ChatColor.RED + "currently paused.");
                }
                else if(getRunningGamesCount() == 0) {
                    description.add(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "Click to join the queue");
                }
            }
            else {
                description.add(ChatColor.RED + "You have not discovered");
                description.add(ChatColor.RED + "this arena yet.");
            }
            return description;
        };
    }
    
    private static Map<String, Arena> arenas;
    
    public static Arena byName(String name) {
        Arena arena = arenas.get(name);
        if(arena == null) {
            for(Arena a : arenas.values()) {
                if(a.getName().equalsIgnoreCase(name)) {
                    arena = a;
                }
            }
        }
        return arena;
    }
    
    public static Collection<Arena> getAll() {
        return arenas.values();
    }
    
    public static void init(){
        arenas = new HashMap<>();
        MongoCursor<Document> dbc = SuperJump.getInstance().getPluginDB().getCollection("Arenas").find().iterator();
        while(dbc.hasNext()) {
            Document d = dbc.next();
            Arena arena;
            if(!d.containsKey("isRandom") || !d.getBoolean("isRandom")) {
                arena = EntityBuilder.load(d, Arena.class);
                if(arena.getSize() == 2) {
                    arenas.put(arena.getName(), arena);
                    SuperJump.getInstance().getBattleManager().registerArena(arena);
                }
            }
//            else {
//                arena = EntityBuilder.load(d, RandomArena.class);
//            }
        }
        SuperJump.getInstance().log("Loaded " + arenas.size() + " arenas!");
    }
}

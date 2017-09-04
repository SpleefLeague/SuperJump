/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.game;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.spleefleague.core.events.BattleStartEvent.StartReason;
import com.spleefleague.core.io.typeconverters.LocationConverter;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.queue.QueueableArena;
import com.spleefleague.core.utils.Area;
import com.spleefleague.core.utils.function.Dynamic;
import com.spleefleague.entitybuilder.DBEntity;
import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.DBLoadable;
import com.spleefleague.entitybuilder.DBSave;
import com.spleefleague.entitybuilder.DBSaveable;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.superjump.SuperJump;
import com.spleefleague.superjump.player.SJPlayer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;

/**
 *
 * @author Jonas
 */
public class Arena extends DBEntity implements DBLoadable, DBSaveable, QueueableArena<SJPlayer> {

    @DBLoad(fieldName = "border")
    private Area[] borders;
    private Location[] spawns;
    @DBLoad(fieldName = "requiredPlayers", priority = 2)
    private int requiredPlayers;
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
    @DBLoad(fieldName = "spectatorSpawn", typeConverter = LocationConverter.class)
    private Location spectatorSpawn;
    @DBLoad(fieldName = "paused")
    @DBSave(fieldName = "paused")
    private boolean paused = false;
    @DBLoad(fieldName = "isDefault")
    private boolean defaultArena = false;
    @DBLoad(fieldName = "area")
    private Area area;
    @DBLoad(fieldName = "debuggerStart")
    private String debuggerStart;
    @DBLoad(fieldName = "debuggerEnd")
    private String debuggerEnd;
    private int runningGames = 0;
    
    @DBLoad(fieldName = "spawns", typeConverter = LocationConverter.class, priority = 1)
    private void setSpawns(Location[] spawns) {
        this.spawns = spawns;
        this.requiredPlayers = spawns.length;//Will be overwritten if requiredPlayers value exists
    }
    
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
    
    @Deprecated
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

    @Override
    public int getRequiredPlayers() {
        return requiredPlayers;
    }
    
    public Battle startBattle(List<SJPlayer> players, StartReason reason) {
        if (!isOccupied()) {
            Battle battle = new Battle(this, players);
            battle.start(reason);
            return battle;
        }
        return null;
    }

    public MultiBattle startMultiBattle(List<SJPlayer> players, StartReason reason) {
        if (!isOccupied()) {
            MultiBattle battle = new MultiBattle(this, players);
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
        return this.isDefaultArena() || sjp.getVisitedArenas().contains(this);
    }

    public Dynamic<List<String>> getDynamicDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(slp.getUniqueId());
            if (Arena.this.isAvailable(sjp)) {
                if (Arena.this.isPaused()) {
                    description.add(ChatColor.RED + "This arena is");
                    description.add(ChatColor.RED + "currently paused.");
                } else if (getRunningGamesCount() == 0) {
                    description.add(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "Click to join the queue");
                }
            } else {
                description.add(ChatColor.RED + "You have not discovered");
                description.add(ChatColor.RED + "this arena yet.");
            }
            return description;
        };
    }
    
    public boolean isDefaultArena() {
        return this.defaultArena;
    }

    private static Map<String, Arena> arenas;
    private static final MongoCollection<Document> collection;
    
    public static Arena byName(String name) {
        Arena arena = arenas.get(name);
        if (arena == null) {
            for (Arena a : arenas.values()) {
                if (a.getName().equalsIgnoreCase(name)) {
                    arena = a;
                }
            }
        }
        return arena;
    }

    public static Collection<Arena> getAll() {
        return arenas.values();
    }
    
    /**
     * 
     * @param name Name of the arena to reload
     * @return true if the arena exists
     */
    public static boolean reload(String name) {
        Document d = collection.find(new Document("name", name)).first();
        if(d == null) {
            return false;
        }
        else {
            Arena arena = loadArena(d);
            if (arena == null) return false;
            Arena old = Arena.byName(name);
            if(old != null) {
                recursiveCopy(arena, old, Arena.class);
            }
            else {
                addArena(arena);
            }
            return true;
        }
    }

    public static void init() {
        arenas = new HashMap<>();
        MongoCursor<Document> dbc = collection.find().iterator();
        while (dbc.hasNext()) {
            Document d = dbc.next();
            Arena arena = loadArena(d);
            if (arena == null) continue;
            addArena(arena);
        }
        SuperJump.getInstance().log("Loaded " + arenas.size() + " arenas!");
    }
    
    private static Arena loadArena(Document d) {
        if (!d.containsKey("arenaClass")) {
            return EntityBuilder.load(d, Arena.class);
        } else {
            try {
                Class<? extends Arena> c = (Class<? extends Arena>) Class.forName(d.getString("arenaClass"));
                return EntityBuilder.load(d, c);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Arena.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }
    }
    
    private static void addArena(Arena arena) {
        if (arena.getSize() == 2) {
            arenas.put(arena.getName(), arena);
            SuperJump.getInstance().getBattleManager().registerArena(arena);
        }
    }
    
    private static void recursiveCopy(Object src, Object target, Class targetClass) {
        Class srcClass = src.getClass();
        while(true) {
            for(Field f : srcClass.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    f.set(target, f.get(src));
                } catch (Exception ex) {
                    Logger.getLogger(Arena.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if(srcClass == Object.class || srcClass == targetClass) {
                break;
            }
            srcClass = srcClass.getSuperclass();
            if(srcClass == null) {
                break;
            }
        }
    }

    static {
        collection = SuperJump.getInstance().getPluginDB().getCollection("Arenas");
    }
}

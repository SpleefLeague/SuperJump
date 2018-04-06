/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game;

import com.spleefleague.gameapi.events.BattleStartEvent.StartReason;
import com.spleefleague.core.io.typeconverters.LocationConverter;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.gameapi.queue.QueueableArena;
import com.spleefleague.core.utils.Area;
import com.spleefleague.entitybuilder.DBEntity;
import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.DBLoadable;
import com.spleefleague.entitybuilder.DBSave;
import com.spleefleague.entitybuilder.DBSaveable;
import com.spleefleague.parkour.Parkour;
import static com.spleefleague.parkour.game.ParkourMode.CLASSIC;
import com.spleefleague.parkour.game.classic.ClassicParkourArena;
import com.spleefleague.parkour.player.ParkourPlayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Location;

/**
 *
 * @author Jonas
 */
public abstract class Arena<B extends ParkourBattle> extends DBEntity implements DBLoadable, DBSaveable, QueueableArena<ParkourPlayer> {

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
    @DBLoad(fieldName = "parkourMode")
    private ParkourMode parkourMode;
    private int runningGames = 0;
    @DBLoad(fieldName = "description")
    private List<String> description = Collections.emptyList();
    
    @DBLoad(fieldName = "spawns", typeConverter = LocationConverter.class, priority = 1)
    private void setSpawns(Location[] spawns) {
        this.spawns = spawns;
        this.requiredPlayers = spawns.length;//Will be overwritten if requiredPlayers value exists
    }
    
    public Location[] getSpawns() {
        return spawns;
    }

    public ParkourMode getParkourMode() {
        return parkourMode;
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
    
    public abstract B startBattle(List<ParkourPlayer> player, StartReason reason);

    @Override
    public boolean isQueued() {
        return queued;
    }

    @Override
    public boolean isAvailable(ParkourPlayer sjp) {
        return this.isDefaultArena() || sjp.getVisitedArenas().contains(this);
    }

    public Function<SLPlayer, List<String>> getDynamicDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
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

    @Override
    public List<String> getDescription() {
        return description;
    }
    
    public static Arena byName(String name, ParkourMode mode) {
        switch(mode) {
            case CLASSIC: {
                return ClassicParkourArena.byName(name);
            }
        }
        return null;
    }

    public static Collection<? extends Arena<?>> getAll() {
        return Stream.of(
                ClassicParkourArena.getAll()
        ).flatMap(c -> c.stream()).collect(Collectors.toList());  
    }
    
    protected static void recursiveCopy(Object src, Object target, Class targetClass) {
        Class srcClass = src.getClass();
        while(true) {
            for(java.lang.reflect.Field f : srcClass.getDeclaredFields()) {
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

    public static void init() {
        Iterator<Document> arenaTypes = Parkour.getInstance().getPluginDB().getCollection("Arenas").aggregate(Arrays.asList(
                new Document("$unwind", new Document("path", "$parkourMode")),
                new Document("$group", new Document("_id", "$parkourMode").append("arenas", new Document("$addToSet", "$$ROOT")))
        )).iterator();
        while(arenaTypes.hasNext()) {
            Document arenas = arenaTypes.next();
            List<Document> arenaInstances = arenas.get("arenas", List.class);
            try {
                ParkourMode mode = ParkourMode.valueOf(arenas.get("_id", String.class));
                int amount;
                switch(mode) {
                    case CLASSIC: {
                        amount = loadArenas(arenaInstances, ClassicParkourArena::loadArena);
                        break;
                    }
                    default: {
                        continue;
                    }
                }
                String modeName = mode.toString().substring(0, 1).toUpperCase().concat(mode.toString().substring(1).toLowerCase());
                Parkour.getInstance().log("Loaded " + amount + " " + modeName + " Spleef arenas.");
            } catch(IllegalArgumentException e) {
                System.err.println(arenas.get("_id") + " is not a valid spleef mode.");
            }
        }
    }
    
    private static int loadArenas(List<Document> arenas, Consumer<Document> arenaCreator) {
        int successCounter = 0;
        for(Document arena : arenas) {
            try {
                arenaCreator.accept(arena);
                successCounter++;
            } catch(Exception e) {
                System.err.println("Error loading arena " + arena.get("name") + " (" + arena.get("type") + ")");
                e.printStackTrace();
            }
        }
        return successCounter;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game;

import com.mongodb.client.MongoCursor;
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
import com.spleefleague.parkour.game.conquest.ConquestParkourArena;
import com.spleefleague.parkour.game.endless.EndlessParkourArena;
import com.spleefleague.parkour.game.memory.MemoryParkourArena;
import com.spleefleague.parkour.game.versus.classic.VersusClassicParkourArena;
import com.spleefleague.parkour.game.versus.shuffle.VersusShuffleParkourArena;
import com.spleefleague.parkour.player.ParkourPlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Jonas
 */
public abstract class Arena<B extends ParkourBattle> extends DBEntity implements DBLoadable, DBSaveable, QueueableArena<ParkourPlayer> {

    @DBLoad(fieldName = "border")
    private Area[] borders;
    protected Location[] spawns;
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
    @DBLoad(fieldName = "difficultyStars")
    private int difficultyStars;
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
    @DBLoad(fieldName = "menuPos")
    private int menuPos = -1;
    @DBLoad(fieldName = "menuMaterial")
    private Material menuMaterial = Material.AIR;
    @DBLoad(fieldName = "menuMeta")
    private int menuMeta = 0;
    private double mapMultiplier = 0;
    @DBLoad(fieldName = "banTime")
    private int banTime = 0;
    
    @DBLoad(fieldName = "mapMultiplier")
    private void setMapMultiplier(Object multiplier) {
        mapMultiplier = (double)multiplier;
    }
    
    @DBLoad(fieldName = "spawns", typeConverter = LocationConverter.class, priority = 1)
    public void setSpawns(Location[] spawns) {
        this.spawns = spawns;
        this.requiredPlayers = spawns.length;//Will be overwritten if requiredPlayers value exists
    }
    
    public Location[] getSpawns() {
        return spawns;
    }

    public ParkourMode getParkourMode() {
        return parkourMode;
    }
    
    public float getDifficulty() {
        return difficultyStars;
    }
    
    public void setGoals(Area[] goals) {
        this.goals = goals;
    }
    
    public Area[] getGoals() {
        return goals;
    }

    public void setArea(Area area) {
        this.area = area;
    }
    
    public Area getArea() {
        return area;
    }

    public void setBorders(Area[] borders) {
        this.borders = borders;
    }
    
    public Area[] getBorders() {
        return borders;
    }
    
    public int getMenuPos() {
        return menuPos;
    }

    public Material getMenuMaterial() {
        return menuMaterial;
    }
    
    public byte getMenuMeta() {
        return (byte)menuMeta;
    }
    
    public ItemStack getMenuItem() {
        return new ItemStack(menuMaterial, 1, (byte)menuMeta);
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
    
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
    
    public boolean hasLeaderboard() {
        return false;
    }
    
    public void setHighscore(String player, long score) {
        
    }
    
    public List<String> getLeaderboard() {
        return new ArrayList<>();
    }
    
    public String getDifficultyStars() {
        String s = "";
        for(int i = 0; i < 5; i++) {
            if(i < this.difficultyStars) {
                s += ChatColor.GOLD + "✫";
            }
            else {
                s += ChatColor.GRAY + "✫";
            }
        }
        return s;
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
    
    public float getMapMultiplier() {
        return (float)mapMultiplier;
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
    
    public int getBanTime() {
        return banTime;
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
                description.add("");
                if (Arena.this.isPaused()) {
                    description.add(ChatColor.RED + "This arena is");
                    description.add(ChatColor.RED + "currently paused.");
                } else if (getRunningGamesCount() == 0) {
                    description.add(ChatColor.GOLD + "Click to join the queue.");
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
            case CLASSIC:
                return VersusClassicParkourArena.byName(name);
            case SHUFFLE:
                return VersusShuffleParkourArena.byName(name);
            case ENDLESS:
                return EndlessParkourArena.byName(name);
            case MEMORY:
                return MemoryParkourArena.byName(name);
        }
        return null;
    }

    public static Collection<? extends Arena<?>> getAll() {
        return Stream.of(VersusClassicParkourArena.getAll()
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
        Document query = new Document("disabled", new Document("$ne", true));
        MongoCursor<Document> dbc = Parkour.getInstance().getPluginDB()
                .getCollection("Arenas")
                .find(query)
                .iterator();
        while (dbc.hasNext()) {
            Document arenaDoc = dbc.next();
            try {
                ParkourMode mode = ParkourMode.valueOf(arenaDoc.get("parkourMode", String.class));
                switch(mode) {
                    case CLASSIC:
                        loadArena(arenaDoc, VersusClassicParkourArena::loadArena);
                        break;
                    case SHUFFLE:
                        loadArena(arenaDoc, VersusShuffleParkourArena::loadArena);
                        break;
                    case CONQUEST:
                        loadArena(arenaDoc, ConquestParkourArena::loadArena);
                        break;
                    case ENDLESS:
                        loadArena(arenaDoc, EndlessParkourArena::loadArena);
                        break;
                    case MEMORY:
                        loadArena(arenaDoc, MemoryParkourArena::loadArena);
                        break;
                    default:
                        continue;
                }
            } catch(Exception e) {
                Parkour.getInstance().log("Error loading arena: " + arenaDoc.get("name"));
            }
        }
        EndlessParkourArena.initHighscores();
        ConquestParkourArena.initPacks();
    }
    
    public static void terminate() {
        
    }
    
    private static void loadArena(Document arena, Consumer<Document> arenaCreator) {
        try {
            arenaCreator.accept(arena);
        } catch(Exception e) {
            System.err.println("Error loading arena " + arena.get("name") + " (" + arena.get("type") + ")");
            e.printStackTrace();
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.game;

import com.mongodb.client.MongoCursor;
import com.spleefleague.core.io.DBEntity;
import com.spleefleague.core.io.DBLoad;
import com.spleefleague.core.io.DBLoadable;
import com.spleefleague.core.io.DBSave;
import com.spleefleague.core.io.DBSaveable;
import com.spleefleague.core.io.EntityBuilder;
import com.spleefleague.core.io.TypeConverter;
import com.spleefleague.core.player.GeneralPlayer;
import com.spleefleague.core.queue.Queue;
import com.spleefleague.core.utils.Area;
import com.spleefleague.superjump.SuperJump;
import com.spleefleague.superjump.player.SJPlayer;
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
public class Arena extends DBEntity implements DBLoadable, DBSaveable, Queue{
    
    @DBLoad(fieldName = "border")
    private Area border;
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
    private boolean occupied = false;
    
    public Arena() {
        
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
    
    public Area getBorder() {
        return border;
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
        return occupied;
    }
    
    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }
    
    public boolean isRated() {
        return rated;
    }
    
    public boolean isTpBackSpectators() {
        return tpBackSpectators;
    }
    
    public boolean isQueued() {
        return queued;
    }
    
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
        return spawns.length;
    }

    @Override
    public boolean isQueued(GeneralPlayer gp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isAvailable(GeneralPlayer gp) {
        return SuperJump.getInstance().getPlayerManager().get(gp.getPlayer()).getVisitedArenas().contains(this);
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
    
    public Battle startBattle(List<SJPlayer> players) {
        if(!isOccupied()) {
            Battle battle = new Battle(this, players);
            battle.start();
            return battle;
        }
        return null;
    }
    
    @Override
    public int getQueueLength() {
        return SuperJump.getInstance().getBattleManager().getGameQueue().getQueueLength(this);
    }

    @Override
    public int getQueuePosition(GeneralPlayer gp) {
        return SuperJump.getInstance().getBattleManager().getGameQueue().getQueuePosition(this, (SJPlayer)gp);
    }

    @Override
    public String getCurrentState() {
        if(occupied) {
            Battle battle = SuperJump.getInstance().getBattleManager().getBattle(this);
            if(battle.getArena().getSize() == 2) {
                return ChatColor.GOLD + battle.getActivePlayers().get(0).getName() + ChatColor.GRAY + ChatColor.ITALIC + " vs. " + ChatColor.RESET + ChatColor.GOLD + battle.getActivePlayers().get(1).getName();
            }
            else {
                StringBuilder sb = new StringBuilder();
                sb.append(ChatColor.GRAY).append("Currently playing: ");
                for(SJPlayer sjp : battle.getActivePlayers()) {
                    if(sb.length() > 0) {
                        sb.append(ChatColor.GRAY).append(", ");
                    }
                    sb.append(ChatColor.GOLD).append(sjp.getName());
                }
                return sb.toString();
            }
        }
        else {
            return ChatColor.BLUE + "Empty";
        }
    }
    
    public static void initialize(){
        arenas = new HashMap<>();
        MongoCursor<Document> dbc = SuperJump.getInstance().getPluginDB().getCollection("Arenas").find().iterator();
        while(dbc.hasNext()) {
            Arena arena = EntityBuilder.load(dbc.next(), Arena.class);
            arenas.put(arena.getName(), arena);
        }
        SuperJump.getInstance().log("Loaded " + arenas.size() + " arenas!");
    }
}

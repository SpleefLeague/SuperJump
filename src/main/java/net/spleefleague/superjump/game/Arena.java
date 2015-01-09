/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump.game;

import com.mongodb.BasicDBList;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.spleefleague.core.io.DBEntity;
import net.spleefleague.core.io.DBLoad;
import net.spleefleague.core.io.DBLoadable;
import net.spleefleague.core.io.EntityBuilder;
import net.spleefleague.core.io.TypeConverter;
import net.spleefleague.core.player.GeneralPlayer;
import net.spleefleague.core.queue.Queue;
import net.spleefleague.core.utils.Area;
import net.spleefleague.superjump.SuperJump;
import net.spleefleague.superjump.player.SJPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Location;

/**
 *
 * @author Jonas
 */
public class Arena extends DBEntity implements DBLoadable, Queue{
    
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
    @DBLoad(fieldName = "rated")
    private boolean rated;
    @DBLoad(fieldName = "queued")
    private boolean queued;
    @DBLoad(fieldName = "liquidLose")
    private boolean liquidLose = true;
    @DBLoad(fieldName = "spectatorSpawn", typeConverter = TypeConverter.LocationConverter.class)
    private Location spectatorSpawn; //null -> default world spawn
    private boolean occupied = false;
    
    public Arena() {
        
    }
    
    public Location[] getSpawns() {
        return spawns;
    }
    
    public Area[] getGoals() {
        return goals;
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
    
    public boolean isQueued() {
        return queued;
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
    
    private static Map<String, Arena> arenas;
    
    public static Arena byName(String name) {
        return arenas.get(name);
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
        DBCursor dbc = SuperJump.getInstance().getPluginDB().getCollection("Arenas").find();
        while(dbc.hasNext()) {
            Arena arena = EntityBuilder.load(dbc.next(), Arena.class);
            arenas.put(arena.getName(), arena);
        }
        SuperJump.getInstance().log("Loaded " + arenas.size() + " arenas!");
    }
    
    public static class AreaArrayConverter extends TypeConverter<BasicDBList, Area[]> {

        @Override
        public Area[] convertLoad(BasicDBList t) {
            Area[] areas = new Area[t.size()];
            for(int i = 0; i < areas.length; i++) {
                areas[i] = EntityBuilder.load((DBObject)t.get(i), Area.class);
            }
            return areas;
        }

        @Override
        public BasicDBList convertSave(Area[] v) {
            BasicDBList bdbl = new BasicDBList();
            for(Area area : v) {
                bdbl.add(EntityBuilder.serialize(area));
            }
            return bdbl;
        }   
    }
}

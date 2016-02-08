/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.game;

import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.events.BattleStartEvent.StartReason;
import com.spleefleague.core.io.DBLoad;
import com.spleefleague.core.io.TypeConverter;
import com.spleefleague.core.utils.Area;
import com.spleefleague.superjump.player.SJPlayer;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;

/**
 *
 * @author Jonas
 */
public class RandomArena extends Arena{
    
    @DBLoad(fieldName = "spawn", typeConverter = TypeConverter.LocationConverter.class)
    private Location spawn1;
    @DBLoad(fieldName = "jumpcount")
    private int jumpCount;
    private Location[] spawns;
    private Area[] goals;
    private Area[] borders;
    private boolean occupied = false;
    
    public RandomArena() {
        super();
        spawns = new Location[2];
    }
    
    @Override
    public Location[] getSpawns() {
        return spawns;
    }
    
    @Override
    public Area[] getGoals() {
        return goals;
    }
    
    @Override
    public Area getArea() {
        return borders[0];
    }
    
    @Override
    public Area[] getBorders() {
        return borders;
    }
    
    @Override
    public int getSize() {
        return spawns.length;
    }
    
    @Override
    public boolean isOccupied() {
        return super.getRunningGamesCount() > 0;
    }
    
    @Override
    public void registerGameEnd() {
        super.registerGameEnd();
        if(borders != null) {
            Area border = borders[0];
            for(int x = border.getLow().getBlockX(); x <= border.getHigh().getBlockX(); x++) {
                for(int y = border.getLow().getBlockY(); y <= border.getHigh().getBlockY(); y++) {
                    for(int z = border.getLow().getBlockZ(); z <= border.getHigh().getBlockZ(); z++) {
                        SpleefLeague.DEFAULT_WORLD.getBlockAt(x, y, z).setType(Material.AIR);
                    }
                }
            }
        }
    }
    
    @Override
    public Battle startBattle(List<SJPlayer> players, StartReason reason) {
        if(!isOccupied()) {
            ArenaData data = generate(spawn1, jumpCount);
            spawns = new Location[2];
            goals = new Area[1];
            spawns[0] = data.spawn1;
            spawns[1] = data.spawn2;
            goals[0] = data.goal;
            borders = data.borders;
            Battle battle = new Battle(this, players);
            battle.start(reason);
            return battle;
        }
        return null;
    }
    
    private static Random random;
    private static int frequencySum;
    
    private static ArenaData generate(Location spawn1, int jumpCount) {
        spawn1.getBlock().setType(Material.GOLD_BLOCK);
        Location goal = null, spawn2 = null, lastLoc = spawn1;
        Jump[] jumps = new Jump[jumpCount];
        Location locSmallest = spawn1.clone(), locHighest = spawn1.clone();
        for(int j = 0; j < jumpCount; j++) {
            Jump next = getNextJump();
            jumps[j] = next;
            lastLoc = next.apply(lastLoc, false);
            locSmallest = getMin(locSmallest, lastLoc);
            locHighest = getMax(locHighest, lastLoc);
            if(j < jumpCount - 1) {
                lastLoc.getBlock().setType(Material.IRON_BLOCK);
            }
            else {
                goal = lastLoc;
                lastLoc.getBlock().setType(Material.DIAMOND_BLOCK);
            }
        }
        for(int j = jumpCount - 1; j >= 0; j--) {
            Jump next = jumps[j];
            lastLoc = next.apply(lastLoc, true);
            locSmallest = getMin(locSmallest, lastLoc);
            locHighest = getMax(locHighest, lastLoc);
            if(j > 0) {
                lastLoc.getBlock().setType(Material.IRON_BLOCK);
            }
            else {
                spawn2 = lastLoc;
                lastLoc.getBlock().setType(Material.GOLD_BLOCK);
            }
        }
        Area goalArea = new Area(goal.clone().add(-0.3, 1, -0.3), goal.clone().add(1.3, 3, 1.3));
        Area border = new Area(locSmallest.add(-3, -3, -3), locHighest.add(3, 3, 3));
        spawn2.setYaw((spawn1.getYaw() + 180) % 360);
        return new ArenaData(spawn1.clone().add(0, 1.2, 0), spawn2.add(0, 1.2, 0), goalArea, new Area[]{border});
    }
    
    private static Location getMin(Location a, Location b) {
        return new Location(a.getWorld(), Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }
    
    private static Location getMax(Location a, Location b) {
        return new Location(a.getWorld(), Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }
    
    private static Jump getNextJump() {
        int id = random.nextInt(frequencySum);
        for(Jump jump : possibleJumps) {
            id -= jump.getFrequency();
            if(id < 0) {
                //y flip
                if(random.nextBoolean()) {
                    return jump;
                }
                else {
                    return new Jump(jump.x, -jump.y, jump.z, jump.frequency);
                }
            }
        }
        return null;
    }
    
    private final static Jump[] possibleJumps = new Jump[]{
        //0 UP
        //2 block
        new Jump(2,0,0,80),
        new Jump(2,0,1,90),
        new Jump(2,0,2,100),
        new Jump(2,0,3,90),
        new Jump(2,0,4,70),
        new Jump(2,0,5,10),
        new Jump(2,0,-1,90),
        new Jump(2,0,-2,100),
        new Jump(2,0,-3,90),
        new Jump(2,0,-4,70),
        new Jump(2,0,-5,10),
        //3 block
        new Jump(3,0,0,100),
        new Jump(3,0,1,100),
        new Jump(3,0,2,100),
        new Jump(3,0,3,80),
        new Jump(3,0,4,70),
        new Jump(3,0,-1,100),
        new Jump(3,0,-2,100),
        new Jump(3,0,-3,80),
        new Jump(3,0,-4,70),
        //4 block
        new Jump(4,0,0,120),
        new Jump(4,0,1,110),
        new Jump(4,0,2,100),
        new Jump(4,0,3,80),
        new Jump(4,0,4,5),
        new Jump(4,0,-1,110),
        new Jump(4,0,-2,100),
        new Jump(4,0,-3,80),
        new Jump(4,0,-4,5),
        //5 block
        new Jump(5,0,0,20),
        new Jump(5,0,1,15),
        new Jump(5,0,2,2),
        new Jump(5,0,-1,15),
        new Jump(5,0,-2,2),
        //1 UP
        //2 block
        new Jump(2,1,0,100),
        new Jump(2,1,1,100),
        new Jump(2,1,2,100),
        new Jump(2,1,3,100),
        new Jump(2,1,4,70),
        new Jump(2,1,-1,100),
        new Jump(2,1,-2,100),
        new Jump(2,1,-3,100),
        new Jump(2,1,-4,70),
        //3 block
        new Jump(3,1,0,100),
        new Jump(3,1,1,100),
        new Jump(3,1,2,100),
        new Jump(3,1,3,80),
        new Jump(3,1,-1,100),
        new Jump(3,1,-2,100),
        new Jump(3,1,-3,80),
        //4 block
        new Jump(4,1,0,50),
        new Jump(4,1,1,40),
        new Jump(4,1,2,30),
        new Jump(4,1,-1,40),
        new Jump(4,1,-2,30),
    };
    
    private static class Jump {
        
        private final int x, y, z, frequency;
        
        public Jump(int x, int y, int z, int frequency) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.frequency = frequency;
        }
        
        public int getFrequency() {
            return frequency;
        }
        
        public Location apply(Location loc, boolean back) {
            return loc.clone().add(x, back ? -y : y, z);
        }
    }
    
    private static class ArenaData {
        
        private final Location spawn1, spawn2;
        private final Area goal, borders[];
        
        public ArenaData(Location spawn1, Location spawn2, Area goal, Area[] borders) {
            this.spawn1 = spawn1;
            this.spawn2 = spawn2;
            this.goal = goal;
            this.borders = borders;
        }
    }
    
    static {
        random = new Random();
        frequencySum = 0;
        for(Jump jump : possibleJumps) {
            frequencySum += jump.getFrequency();
        }
    }
}

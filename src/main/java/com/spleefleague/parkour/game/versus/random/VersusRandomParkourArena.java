/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game.versus.random;

import com.spleefleague.core.utils.Area;
import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.gameapi.events.BattleStartEvent;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.Arena;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.game.ParkourRandomJumps;
import com.spleefleague.parkour.player.ParkourPlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

/**
 *
 * @author Jonas
 */
public class VersusRandomParkourArena extends Arena<VersusRandomParkourBattle> {

    @DBLoad(fieldName = "jumpcount")
    private int jumpCount;
    @DBLoad(fieldName = "difficultyGenerate")
    private int difficultyGen;
    
    private static final Map<String, VersusRandomParkourArena> ARENAS = new HashMap<>();
    
    public static Collection<VersusRandomParkourArena> getAll() {
        return ARENAS.values();
    }
    
    public static VersusRandomParkourArena byName(String arena) {
        return ARENAS.get(arena.toLowerCase());
    }
    
    public int getJumpCount() {
        return jumpCount;
    }
    
    @Override
    public ParkourMode getParkourMode() {
        return ParkourMode.RANDOM;
    }

    @Override
    public boolean isOccupied() {
        return false;
    }
    
    public static void loadArena(Document document) {
        VersusRandomParkourArena arena = EntityBuilder.load(document, VersusRandomParkourArena.class);
        if(ARENAS.containsKey(arena.getName().toLowerCase())) {
            Arena.recursiveCopy(arena, byName(arena.getName()), Arena.class);
        }
        else {
            Parkour.getInstance().getBattleManager(ParkourMode.RANDOM).registerArena(arena);
            ARENAS.put(arena.getName().toLowerCase(), arena);
        }
    }
    
    @Override
    public VersusRandomParkourBattle startBattle(List<ParkourPlayer> players, BattleStartEvent.StartReason reason) {
        if (!isOccupied()) {
            players.get(0).validateEndless();
            ArenaData data = generate(this.spawns[0].clone(), this.jumpCount, this.difficultyGen);
            Location[] spawn1 = data.spawns;
            Area[] goal = new Area[1];
            goal[0] = data.goal;
            Area[] borders = data.borders;
            //Create new arena object since each random arena has different spawns/goals/borders
            VersusRandomParkourArena arena = new VersusRandomParkourArena() {

                @Override
                public Area[] getGoals() {
                    return goal;
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
                public Location[] getSpawns() {
                    return spawn1;
                }
                
                @Override
                public void registerGameEnd() {
                    super.registerGameEnd();
                }

                @Override
                public String getName() {
                    return VersusRandomParkourArena.this.getName();
                }
                
                @Override
                public boolean isRated() {
                    return VersusRandomParkourArena.this.isRated();
                }
            };
            VersusRandomParkourBattle battle = new VersusRandomParkourBattle(arena, players);
            for(BlockPrototype bp : data.fakeBlocks) {
                battle.getFakeWorld().getBlockAt(bp.location).setType(bp.type);
                battle.getFakeWorld().getBlockAt(bp.location).setData(bp.data);
            }
            battle.start(reason);
        }
        return null;
    }

    private static Random random = new Random();
    private static int frequencySum;
    private static ArrayList<ParkourRandomJumps.Jump> possibleJumps;
    
    private ArenaData generate(Location spawn1, int jumpCount, float difficulty) {
        Location[] spawn = new Location[2];
        spawn[0] = spawn1;
        difficulty = difficulty - 0.5f;
        possibleJumps = ParkourRandomJumps.getJumpsByDifficultyB(difficulty);
        frequencySum = 0;
        for(ParkourRandomJumps.Jump j : possibleJumps) {
            frequencySum += j.getFrequency(difficulty);
        }
        
        Collection<BlockPrototype> fakeBlocks = new ArrayList<>();
        Location goal = null, lastLoc = spawn1.clone().add(0, -1, 0);
        Location locSmallest = spawn1.clone(), locHighest = spawn1.clone();
        fakeBlocks.add(new BlockPrototype(lastLoc, Material.REDSTONE_LAMP_ON, (byte)0));
        
        frequencySum = 0;
        for(ParkourRandomJumps.Jump jump : possibleJumps)
            frequencySum += jump.getFrequency(difficulty);
        
        ParkourRandomJumps.Jump[] jumps = new ParkourRandomJumps.Jump[jumpCount];
        for (int j = 0; j < jumpCount; j++) {
            ParkourRandomJumps.Jump next = getNextJump(possibleJumps, difficulty);
            jumps[j] = next;
            lastLoc = next.apply(lastLoc, false);
            locSmallest = getMin(locSmallest, lastLoc);
            locHighest = getMax(locHighest, lastLoc);
            if (j < jumpCount - 1) {
                fakeBlocks.add(new BlockPrototype(lastLoc.clone(), Material.QUARTZ_BLOCK, (byte)0));
            } else {
                goal = lastLoc;
                fakeBlocks.add(new BlockPrototype(lastLoc.clone(), Material.REDSTONE_LAMP_ON, (byte)0));
            }
        }
        for (int j = jumpCount - 1; j >= 0; j--) {
            ParkourRandomJumps.Jump next = jumps[j];
            lastLoc = next.apply(lastLoc, true);
            locSmallest = getMin(locSmallest, lastLoc);
            locHighest = getMax(locHighest, lastLoc);
            if (j > 0) {
                fakeBlocks.add(new BlockPrototype(lastLoc.clone(), Material.QUARTZ_BLOCK, (byte)0));
            } else {
                spawn[1] = lastLoc.clone();
                spawn[1].setYaw(spawn[1].getYaw() + 180);
                spawn[1].add(new Vector(0, 1, 0));
                fakeBlocks.add(new BlockPrototype(lastLoc.clone(), Material.REDSTONE_LAMP_ON, (byte)0));
            }
        }
        
        Area goalArea = new Area(goal.clone().add(-0.3, 1, -0.3), goal.clone().add(1.3, 3, 1.3));
        Area border = new Area(locSmallest.add(-3, -3, -3), locHighest.add(3, 3, 3));
        return new ArenaData(goalArea, fakeBlocks, new Area[]{border}, spawn);
    }

    private static Location getMin(Location a, Location b) {
        return new Location(a.getWorld(), Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    private static Location getMax(Location a, Location b) {
        return new Location(a.getWorld(), Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    private static ParkourRandomJumps.Jump getNextJump(final ArrayList<ParkourRandomJumps.Jump> possibleJumps, float difficulty) {
        int id = random.nextInt(frequencySum);
        for (ParkourRandomJumps.Jump jump : possibleJumps) {
            id -= jump.getFrequency(difficulty);
            if (id < 0) {
                return jump;
            }
        }
        return null;
    }

    private static class ArenaData {

        private final Collection<BlockPrototype> fakeBlocks;
        private final Area goal, borders[];
        private final Location[] spawns;

        public ArenaData(Area goal, Collection<BlockPrototype> fakeBlocks, Area[] borders, Location[] spawns) {
            this.goal = goal;
            this.borders = borders;
            this.fakeBlocks = fakeBlocks;
            this.spawns = spawns;
        }
    }

    private static class BlockPrototype {    
        private Material type;
        private byte data;
        private final Vector location;

        public BlockPrototype(Location location, Material type, byte data) {
            this(location.toVector(), type, data);
        }
        
        public BlockPrototype(Vector location, Material type, byte data) {
            this.type = type;
            this.data = data;
            this.location = location;
        }        
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game.versus.classic;

import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.gameapi.events.BattleStartEvent;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.Arena;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.player.ParkourPlayer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;

/**
 *
 * @author jonas
 */
public class VersusClassicParkourArena extends Arena<VersusClassicParkourBattle> {

    @Override
    public VersusClassicParkourBattle startBattle(List<ParkourPlayer> players, BattleStartEvent.StartReason reason) {
        if (!isOccupied()) { //Shouldn't be necessary
            VersusClassicParkourBattle battle = new VersusClassicParkourBattle(this, players);
            battle.start(reason);
            return battle;
        }
        return null;
    }
    
    private static final Map<String, VersusClassicParkourArena> arenas = new HashMap<>();
    
    public static Collection<VersusClassicParkourArena> getAll() {
        return arenas.values();
    }
    
    public static VersusClassicParkourArena byName(String arena) {
        return arenas.get(arena.toLowerCase());
    }
    
    public static void loadArena(Document document) {
        Class<? extends VersusClassicParkourArena> c = VersusClassicParkourArena.class;
        if (document.containsKey("arenaClass")) {
            try {
                c = (Class<? extends VersusClassicParkourArena>) Class.forName(document.getString("arenaClass"));
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Arena.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        VersusClassicParkourArena arena = EntityBuilder.load(document, c);
        if(arenas.containsKey(arena.getName().toLowerCase())) {
            Arena.recursiveCopy(arena, byName(arena.getName()), Arena.class);
        }
        else {
            Parkour.getInstance().getBattleManager(ParkourMode.CLASSIC).registerArena(arena);
            arenas.put(arena.getName().toLowerCase(), arena);
        }
    }
}

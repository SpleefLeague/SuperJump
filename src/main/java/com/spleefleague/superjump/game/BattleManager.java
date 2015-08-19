/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.game;

import com.spleefleague.core.queue.GameQueue;
import com.spleefleague.superjump.game.signs.GameSign;
import com.spleefleague.superjump.player.SJPlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;


/**
 *
 * @author Jonas
 */
public class BattleManager {
    
    private final HashSet<Battle> activeBattles;
    private final GameQueue<SJPlayer, Arena> gameQueue;
    
    public BattleManager() {
        this.activeBattles = new HashSet<>();
        this.gameQueue = new GameQueue<>();
        for(Arena arena : Arena.getAll()) {
            gameQueue.register(arena);
        }
    }
    
    public GameQueue<SJPlayer, Arena> getGameQueue() {
        return gameQueue;
    }
    
    public void registerArena(Arena arena) {
        gameQueue.register(arena);
    }
    
    public void unregisterArena(Arena arena) {
        gameQueue.unregister(arena);
    }
    
    public void queue(SJPlayer player, Arena queue) {
        gameQueue.queue(player, queue);
        if(!queue.isPaused() && !queue.isOccupied()) {
            Collection<SJPlayer> players = gameQueue.request(queue);
            if(players != null) {
                queue.startBattle(new ArrayList<>(players));
            }
        }
        GameSign.updateGameSigns();
    }
    
    public void queue(SJPlayer player) {
        gameQueue.queue(player);
        HashMap<Arena, Collection<SJPlayer>> requested = gameQueue.request();
        for(Arena arena : requested.keySet()) {
            arena.startBattle(new ArrayList<>(requested.get(arena)));
        }
        GameSign.updateGameSigns();
    }
    
    public void dequeue(SJPlayer sjp) {
        gameQueue.dequeue(sjp);
        GameSign.updateGameSigns();
    }

    public boolean isQueued(SJPlayer sjp) {
        return gameQueue.isQueued(sjp);
    }
    
    public void add(Battle battle) {
        activeBattles.add(battle);
    }
    
    public void remove(Battle battle) {
        activeBattles.remove(battle);
        Collection<SJPlayer> players = gameQueue.request(battle.getArena());
        if(players != null) {
            battle.getArena().startBattle(new ArrayList<>(players));
        }
    }
    
    public Collection<Battle> getAll() {
        return (Collection<Battle>)activeBattles.clone();
    }
    
    public Battle getBattle(SJPlayer sjplayer) {
        for(Battle battle : activeBattles) {
            for(SJPlayer sjp : battle.getActivePlayers()) {
                if(sjp == sjplayer) {
                    return battle;
                }
            }
        }
        return null;
    }
    
    public Battle getBattle(Arena arena) {
        for(Battle battle : activeBattles) {
            if(battle.getArena() == arena) {
                return battle;
            }
        }
        return null;
    }
    
    public boolean isIngame(SJPlayer sjp) {
        return getBattle(sjp) != null;
    }
}
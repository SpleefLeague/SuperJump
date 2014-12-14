/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import net.spleefleague.core.queue.GameQueue;
import net.spleefleague.superjump.player.SJPlayer;

/**
 *
 * @author Jonas
 */
public class BattleManager {
    
    private final Collection<Battle> activeBattles;
    private final GameQueue<SJPlayer, Arena> gameQueue;
    
    public BattleManager(GameQueue<SJPlayer, Arena> gameQueue) {
        this.activeBattles = new HashSet<>();
        this.gameQueue = gameQueue;
    }
    
    public void queue(SJPlayer player, Arena queue) {
        gameQueue.queue(player, queue, queue.isQueued());
        if(!queue.isOccupied()) {
            Collection<SJPlayer> players = gameQueue.request(queue);
            if(players != null) {
                queue.startBattle(new ArrayList<>(players));
            }
        }
    }
    
    public void queue(SJPlayer player) {
        gameQueue.queue(player);
        HashMap<Arena, Collection<SJPlayer>> requested = gameQueue.request();
        for(Arena arena : requested.keySet()) {
            arena.startBattle(new ArrayList<>(requested.get(arena)));
        }
    }
    
    public void dequeue(SJPlayer sjp) {
        gameQueue.dequeue(sjp);
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
        return activeBattles;
    }
    
    public Battle getBattle(SJPlayer sjplayer) {
        for(Battle battle : activeBattles) {
            for(SJPlayer sjp : battle.getPlayers()) {
                if(sjp == sjplayer) {
                    return battle;
                }
            }
        }
        return null;
    }
    
    public boolean isIngame(SJPlayer sjp) {
        return getBattle(sjp) != null;
    }
}
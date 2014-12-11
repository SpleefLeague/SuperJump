/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump.game;

import java.util.Collection;
import java.util.List;
import net.spleefleague.superjump.SuperJump;
import net.spleefleague.superjump.player.SJPlayer;

/**
 *
 * @author Jonas
 */
public class Battle {
    
    private final Arena arena;
    private final List<SJPlayer> players;
    
    protected Battle(Arena arena, List<SJPlayer> players) {
        this.arena = arena;
        this.players = players;
    }
    
    public Arena getArena() {
        return arena;
    }
    
    public Collection<SJPlayer> getPlayers() {
        return players;
    }
    
    public void start() {
        SuperJump.getInstance().getBattleManager().add(this);
        for(int i = 0; i < players.size(); i++) {
            SJPlayer sjp = players.get(i);
            sjp.setIngame(true);
            sjp.setFrozen(true);
            sjp.getPlayer().teleport(arena.getSpawns()[i]);
        }
    }
    
    public void end(boolean rated) {
        SuperJump.getInstance().getBattleManager().remove(this);
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import net.spleefleague.superjump.SuperJump;
import net.spleefleague.superjump.player.SJPlayer;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author Jonas
 */
public class Battle {
    
    private final Arena arena;
    private final List<SJPlayer> players;
    private final HashMap<SJPlayer, PlayerData> data;
    private int ticksPassed = 0;
    private BukkitRunnable clock;
    
    protected Battle(Arena arena, List<SJPlayer> players) {
        this.arena = arena;
        this.players = players;
        this.data = new HashMap<>();
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
            this.data.put(sjp, new PlayerData(sjp, arena.getSpawns()[i]));
        }
        startCountdown();
    }
    
    private void startCountdown() {
        BukkitRunnable br = new BukkitRunnable() {
            private int secondsLeft = 3;
            @Override
            public void run() {
                if(secondsLeft > 0) {
                    for(SJPlayer sjp : players) {
                        sjp.getPlayer().sendMessage(SuperJump.getInstance().getChatPrefix() + " " + secondsLeft-- + "...");
                    }
                }
                else {
                    for(SJPlayer sjp : players) {
                        sjp.getPlayer().sendMessage(SuperJump.getInstance().getChatPrefix() + " GO!");
                    }
                    startClock();
                    super.cancel();
                }
            }
            
            private void startClock() {
                clock = new BukkitRunnable() {
                    @Override
                    public void run() {
                        ticksPassed++;
                    }
                };
                clock.runTaskTimer(SuperJump.getInstance(), 0, 1);
            }
        };
        br.runTaskTimer(SuperJump.getInstance(), 20, 60);
    }
    
    public void end(SJPlayer winner, boolean rated) {
        clock.cancel();
        if(rated) {
            applyRatingChange(winner);
        }
        SuperJump.getInstance().getBattleManager().remove(this);
    }
    
    private void applyRatingChange(SJPlayer winner) {
        int winnerPoints = 0;
        final int MIN_RATING = 1, MAX_RATING = 20;
        for(SJPlayer sjp : players) {
            if(sjp != winner) {
                float elo = (float) (1f / (1f + Math.pow(2f, ((sjp.getRating() - winner.getRating()) / 400f))));
                int rating = (int) Math.round(MAX_RATING * (1f - elo));
                if (rating < MIN_RATING) {
                    rating = MIN_RATING;
                }
                winnerPoints += rating;
                sjp.setRating(sjp.getRating() - rating);
            }
        }
        winner.setRating(winner.getRating() + winnerPoints);
    }

    public void onArenaLeave(SJPlayer sjp) {
        data.get(sjp).increaseFalls();
        sjp.getPlayer().teleport(data.get(sjp).getSpawn());
    }
    
    private static class PlayerData {
        
        private int falls;
        private final Location spawn;
        private final SJPlayer sjp;
        
        public PlayerData(SJPlayer sjp, Location spawn) {
            this.sjp = sjp;
            this.spawn = spawn;
            this.falls = 0;
        }
        
        public Location getSpawn() {
            return spawn;
        }
        
        public int getFalls() {
            return falls;
        }
        
        public void increaseFalls() {
            this.falls++;
        }
        
        public SJPlayer getPlayer() {
            return sjp;
        }
    }
}

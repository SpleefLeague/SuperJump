/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump.game;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import net.spleefleague.core.SpleefLeague;
import net.spleefleague.core.chat.Theme;
import net.spleefleague.core.player.PlayerState;
import net.spleefleague.core.player.SLPlayer;
import net.spleefleague.core.utils.Area;
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
    
    public Location getSpawn(SJPlayer sjp) {
        return data.get(sjp).getSpawn();
    }
    
    public Area getGoal(SJPlayer sjp) {
        return data.get(sjp).getGoal();
    }
    
    public void removePlayer(SJPlayer sjp) {
        players.remove(sjp);
        if(players.size() == 1) {
            end(players.get(0));
        }
        else if(players.size() > 1) {
            //TODO
            //Send message
        }
    }
    
    public void start() {
        SuperJump.getInstance().getBattleManager().add(this);
        for(int i = 0; i < players.size(); i++) {
            SJPlayer sjp = players.get(i);
            sjp.setIngame(true);
            sjp.setFrozen(true);
            sjp.getPlayer().teleport(arena.getSpawns()[i]);
            this.data.put(sjp, new PlayerData(sjp, arena.getSpawns()[i], arena.getGoals()[i % arena.getGoals().length]));
            SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer()).setState(PlayerState.IDLE);
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
    
    public void cancel() {
        for (SJPlayer pl : players) {
            pl.getPlayer().sendMessage(SuperJump.getInstance().getPrefix() + " " + Theme.INCOGNITO.buildTheme(false) + "Your battle has been cancelled by a moderator.");
        }
        resetPlayers();
    }
    
    public void end(SJPlayer winner) {
        end(winner, arena.isRated());
    }
    
    public void end(SJPlayer winner, boolean rated) {
        clock.cancel();
        if(rated) {
            applyRatingChange(winner);
        }
        resetPlayers();
        SuperJump.getInstance().getBattleManager().remove(this);
    }
    
    private void resetPlayers() {
        for(SJPlayer sjp : players) {
            sjp.getPlayer().teleport(SpleefLeague.DEFAULT_WORLD.getSpawnLocation());
            sjp.setIngame(false);
            sjp.setFrozen(false);
            SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer()).setState(PlayerState.IDLE);
        }
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
        private final Area goal;
        
        public PlayerData(SJPlayer sjp, Location spawn, Area goal) {
            this.sjp = sjp;
            this.spawn = spawn;
            this.falls = 0;
            this.goal = goal;
        }
        
        public Location getSpawn() {
            return spawn;
        }
        
        public Area getGoal() {
            return goal;
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

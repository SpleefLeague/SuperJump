/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game.versus.shuffle;

import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.ParkourBattle;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.player.ParkourPlayer;
import java.util.List;
import java.util.Random;

/**
 *
 * @author jonas
 */
public class VersusShuffleParkourBattle extends ParkourBattle<VersusShuffleParkourArena>{

    public VersusShuffleParkourBattle(VersusShuffleParkourArena arena, List<ParkourPlayer> players) {
        super(arena, players);
    }
    
    @Override
    protected void applyRatingChange(ParkourPlayer winner) {
        if(getPlayers().size() != 2) return;
        ParkourPlayer p1;
        ParkourPlayer p2;
        if (getPlayers().get(0).equals(winner)) {
            p1 = getPlayers().get(0);
            p2 = getPlayers().get(1);
        }
        else {
            p1 = getPlayers().get(1);
            p2 = getPlayers().get(0);
        }
        p1.addVersusWin(); p2.addVersusLoss();
        p1.addPoints((int)(10.f * (p1.getPointMultiplier() * VersusShuffleParkourArena.byName(arena.getName()).getMapMultiplier())), " for winning " + Parkour.arenaColor + "Shuffle " + arena.getName());
        p2.addPoints((int)(2.f * (p2.getPointMultiplier() * VersusShuffleParkourArena.byName(arena.getName()).getMapMultiplier())), " for participating in " + Parkour.arenaColor + "Shuffle " + arena.getName());
        float ppChance = (p1.getParagonLevel() * 0.0001f) + (arena.getMapMultiplier() / 1000.f);
        if (new Random().nextFloat() < ppChance) {
            p1.addParagonPoints(1, ", spend it in the " + Parkour.shopColor + "Paragon Shop");
        }
        this.getPlayers().forEach(p -> Parkour.getInstance().getPlayerManager().save(p));
    }

    @Override
    protected void addToBattleManager() {
        Parkour.getInstance().getBattleManager(ParkourMode.SHUFFLE).add(this);
    }

    @Override
    protected void removeFromBattleManager() {
        Parkour.getInstance().getBattleManager(ParkourMode.SHUFFLE).remove(this);
    }
    
}

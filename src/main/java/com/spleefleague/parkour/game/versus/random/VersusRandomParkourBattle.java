/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game.versus.random;

import com.spleefleague.core.chat.ChatManager;
import static com.spleefleague.gameapi.queue.Battle.calculateEloRatingChange;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.ParkourBattle;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.player.ParkourPlayer;
import java.util.List;
import org.bukkit.ChatColor;

/**
 *
 * @author jonas
 */
public class VersusRandomParkourBattle extends ParkourBattle<VersusRandomParkourArena>{

    public VersusRandomParkourBattle(VersusRandomParkourArena arena, List<ParkourPlayer> players) {
        super(arena, players);
    }
    
    @Override
    protected void applyRatingChange(ParkourPlayer winner) {
        if(getPlayers().size() != 2) return;
        ParkourPlayer p1 = getPlayers().get(0);
        ParkourPlayer p2 = getPlayers().get(1);
        if(winner != null && p1 != winner && p2 != winner) return;
        ParkourMode mode = this.getParkourMode();
        int winnerCase = p1 == winner ? -1 : p2 == winner ? 1 : 0;
        int ratingChange = (int)Math.ceil(calculateEloRatingChange(p1.getRating(mode), p2.getRating(mode), winnerCase));
        String playerList = "";
        p1.setRating(mode, p1.getRating(mode) + ratingChange);
        p2.setRating(mode, p2.getRating(mode) - ratingChange);
        playerList += ChatColor.RED + p1.getName() + ChatColor.WHITE + " (" + p1.getRating(mode) + ")" + ChatColor.GREEN + " gets " + ChatColor.GRAY + ratingChange + ChatColor.GREEN + (ratingChange == 1 ? " point. " : " points. ");
        playerList += ChatColor.RED + p2.getName() + ChatColor.WHITE + " (" + p2.getRating(mode) + ")" + ChatColor.GREEN + " gets " + ChatColor.GRAY + -ratingChange + ChatColor.GREEN + (ratingChange == 1 ? " point. " : " points. ");
        ChatManager.sendMessage(mode.getChatPrefix(), ChatColor.GREEN + "Game in arena " + ChatColor.WHITE + getArena().getName() + ChatColor.GREEN + " is over. " + playerList, Parkour.getInstance().getEndMessageChannel());
        p1.addPoints((int)(10 * (p1.getPointMultiplier() + arena.getMapMultiplier())), " for winning SJ Random: " + arena.getName());
        p1.addPoints((int)(2 * (p1.getPointMultiplier() + arena.getMapMultiplier())), " for participating in SJ Random: " + arena.getName());
        this.getPlayers().forEach((p) -> {
            Parkour.getInstance().getPlayerManager().save(p);
        });
    }

    @Override
    protected void addToBattleManager() {
        Parkour.getInstance().getBattleManager(ParkourMode.RANDOM).add(this);
    }

    @Override
    protected void removeFromBattleManager() {
        Parkour.getInstance().getBattleManager(ParkourMode.RANDOM).remove(this);
    }
    
}

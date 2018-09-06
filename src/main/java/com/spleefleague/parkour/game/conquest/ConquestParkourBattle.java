/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game.conquest;

import com.google.common.collect.Lists;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.chat.Theme;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.gameapi.events.BattleEndEvent;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.ParkourBattle;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.player.ParkourPlayer;
import java.util.List;
import org.bukkit.Bukkit;

/**
 *
 * @author jonas
 */
public class ConquestParkourBattle extends ParkourBattle<ConquestParkourArena>{

    protected ConquestParkourBattle(ConquestParkourArena arena, List<ParkourPlayer> players) {
        super(arena, players);
    }
    
    @Override
    protected void applyRatingChange(ParkourPlayer winner) {
        if(winner == null) return;
        winner.checkConquestScore(arena, this.ticksPassed);
        //winner.addPoints((int)(10 * (winner.getPointMultiplier() + arena.getMapMultiplier())), "for winning " + ChatColor.WHITE + arena.getName());
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(this.getPlayers().get(0).getPlayer());
        slp.setCoins(slp.getCoins() + 1);
        this.getPlayers().forEach((p) -> {
            Parkour.getInstance().getPlayerManager().save(p);
        });
    }

    @Override
    protected void addToBattleManager() {
        Parkour.getInstance().getBattleManager(ParkourMode.CONQUEST).add(this);
    }

    @Override
    protected void removeFromBattleManager() {
        Parkour.getInstance().getBattleManager(ParkourMode.CONQUEST).remove(this);
    }
    
    @Override
    public void end(ParkourPlayer winner, BattleEndEvent.EndReason reason) {
        saveGameHistory(winner, reason);
        if (reason == BattleEndEvent.EndReason.CANCEL) {
            if (reason == BattleEndEvent.EndReason.CANCEL) {
                ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(), Theme.INCOGNITO.buildTheme(false) + "The battle has been cancelled by a moderator.", cc);
            }
        } else if (reason != BattleEndEvent.EndReason.ENDGAME) {
            applyRatingChange(winner);
        }
        Lists.newArrayList(getSpectators()).forEach(this::resetPlayer);
        Lists.newArrayList(getActivePlayers()).forEach(this::resetPlayer);
        Bukkit.getPluginManager().callEvent(new BattleEndEvent(this, reason));
        cleanup();
    }
}

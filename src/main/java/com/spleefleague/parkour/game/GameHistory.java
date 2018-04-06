/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game;

import com.spleefleague.gameapi.events.BattleEndEvent.EndReason;
import com.spleefleague.entitybuilder.DBEntity;
import com.spleefleague.entitybuilder.DBSave;
import com.spleefleague.entitybuilder.DBSaveable;
import com.spleefleague.entitybuilder.TypeConverter;
import com.spleefleague.parkour.player.ParkourPlayer;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author Jonas
 */
public class GameHistory extends DBEntity implements DBSaveable {

    @DBSave(fieldName = "players")
    private final PlayerData[] players;
    @DBSave(fieldName = "date")
    private final Date startDate;
    @DBSave(fieldName = "duration")
    private final int duration; //In ticks
    @DBSave(fieldName = "endReason")
    private final EndReason endReason;
    @DBSave(fieldName = "superjumpMode")
    private final ParkourMode superjumpMode;
    @DBSave(fieldName = "arena")
    private final String arena;

    protected GameHistory(ParkourBattle<?> battle, ParkourPlayer winner, EndReason endReason) {
        this.endReason = endReason;
        players = new PlayerData[battle.getPlayers().size()];
        Collection<ParkourPlayer> activePlayers = battle.getActivePlayers();
        int i = 0;
        for (ParkourPlayer sjp : battle.getPlayers()) {
            players[i++] = new PlayerData(sjp.getUniqueId(), battle.getData(sjp).getFalls(), sjp == winner, !activePlayers.contains(sjp));
        }
        this.duration = battle.getDuration();
        startDate = new Date(System.currentTimeMillis() - this.duration * 50);
        this.arena = battle.getArena().getName();
        this.superjumpMode = battle.getArena().getParkourMode();
    }

    public static class PlayerData extends DBEntity implements DBSaveable {

        @DBSave(fieldName = "uuid", typeConverter = TypeConverter.UUIDStringConverter.class)
        private final UUID uuid;
        @DBSave(fieldName = "falls")
        private final int falls;
        @DBSave(fieldName = "winner")
        private final Boolean winner;
        @DBSave(fieldName = "surrendered")
        private final Boolean surrendered;

        public PlayerData(UUID uuid, int falls, boolean winner, boolean surrendered) {
            this.uuid = uuid;
            this.falls = falls;
            this.winner = winner;
            this.surrendered = surrendered;
        }
    }
}

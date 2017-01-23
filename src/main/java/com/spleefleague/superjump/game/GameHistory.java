/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.game;

import com.spleefleague.core.io.DBEntity;
import com.spleefleague.core.io.DBSave;
import com.spleefleague.core.io.DBSaveable;
import com.spleefleague.core.io.TypeConverter;
import com.spleefleague.superjump.player.SJPlayer;
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
    @DBSave(fieldName = "cancelled")
    private final boolean cancelled;

    protected GameHistory(AbstractBattle battle, SJPlayer winner) {
        this.cancelled = winner == null;
        players = new PlayerData[battle.getPlayers().size()];
        Collection<SJPlayer> activePlayers = battle.getActivePlayers();
        int i = 0;
        for (SJPlayer sjp : battle.getPlayers()) {
            players[i++] = new PlayerData(sjp.getUniqueId(), battle.getData(sjp).getFalls(), sjp == winner, !activePlayers.contains(sjp));
        }
        this.duration = battle.getDuration();
        startDate = new Date(System.currentTimeMillis() - this.duration * 50);
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game;

import com.spleefleague.entitybuilder.DBEntity;
import com.spleefleague.entitybuilder.DBSave;
import com.spleefleague.entitybuilder.DBSaveable;
import com.spleefleague.entitybuilder.TypeConverter;
import com.spleefleague.gameapi.events.BattleEndEvent.EndReason;
import com.spleefleague.parkour.game.classic.SinglePlayerParkourBattle;
import com.spleefleague.parkour.player.ParkourPlayer;
import org.bson.types.ObjectId;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Jonas
 */
public class SinglePlayerGameHistory extends GameHistory {

    @DBSave(fieldName = "adjustedDuration")
    private Integer adjustedDuration;
    @DBSave(fieldName = "pingStart")
    private Integer pingStart;
    @DBSave(fieldName = "pingEnd")
    private Integer pingEnd;
    @DBSave(fieldName = "pingLog")
    private List<Integer> pingLog;
    @DBSave(fieldName = "recordingId")
    private ObjectId recordingId;

    public SinglePlayerGameHistory(SinglePlayerParkourBattle battle, ParkourPlayer winner, EndReason endReason) {
        super(battle, winner, endReason);
        this.pingStart = battle.getPingStart();
        this.pingEnd = battle.getPingEnd();
        this.pingLog = battle.getPingLog();
        this.adjustedDuration = battle.getAdjustedDuration();
    }

    public void setRecordingId(ObjectId recordingId) {
        this.recordingId = recordingId;
    }
}

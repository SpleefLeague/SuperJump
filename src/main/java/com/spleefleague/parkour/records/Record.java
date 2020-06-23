package com.spleefleague.parkour.records;

import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.DBLoadable;
import com.spleefleague.entitybuilder.TypeConverter;
import com.spleefleague.parkour.game.ParkourMode;

import java.util.Date;
import java.util.UUID;

public class Record implements DBLoadable {

    @DBLoad(fieldName = "date")
    private Date date;
    @DBLoad(fieldName = "duration")
    private int duration;
    @DBLoad(fieldName = "player", typeConverter = TypeConverter.UUIDStringConverter.class)
    private UUID player;
    @DBLoad(fieldName = "superjumpMode")
    private ParkourMode parkourMode;

    public Record() {
    }

    public Record(UUID player, int duration, Date date, ParkourMode parkourMode) {
        this.date = date;
        this.duration = duration;
        this.player = player;
        this.parkourMode = parkourMode;
    }

    public Date getDate() {
        return date;
    }

    public int getDuration() {
        return duration;
    }

    public UUID getPlayer() {
        return player;
    }

    public ParkourMode getParkourMode() {
        return parkourMode;
    }
}

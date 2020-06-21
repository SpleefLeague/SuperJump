package com.spleefleague.parkour.records;

import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.DBLoadable;

import java.util.List;

public class ArenaRecords implements DBLoadable {

    @DBLoad(fieldName = "_id")
    private String arena;
    @DBLoad(fieldName = "records")
    private List<Record> records;

    public String getArena() {
        return arena;
    }

    public List<Record> getRecords() {
        return records;
    }
}

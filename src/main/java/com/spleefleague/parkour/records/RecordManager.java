package com.spleefleague.parkour.records;

import com.mongodb.client.FindIterable;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.Arena;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RecordManager {

    private static final String RECORDS_VIEW = "Records";

    private final Map<String, List<Record>> recordCache;
    private final Map<String, Integer> cacheSize;

    public RecordManager() {
        recordCache = new ConcurrentHashMap<>();
        cacheSize = new ConcurrentHashMap<>();
        Bukkit.getScheduler().runTaskTimerAsynchronously(Parkour.getInstance(), this::refresh, 0, 20*10);
    }

    private int getCacheSize(String arena) {
        return cacheSize.getOrDefault(arena, 0);
    }

    public void submitRecord(String arena, Record record) {
        List<Record> records = recordCache.getOrDefault(arena, Collections.emptyList());
        int index = Collections.binarySearch(records, record, Comparator.comparingInt(Record::getDuration));
        if(index >= 0) {
            index++;
        }
        else {
            index = -index - 1;
        }
        if(index < records.size()) {
            records.add(index, record);
        }
    }

    public Optional<Record> getRecord(String arena, int place) {
        cacheSize.compute(arena, (k, v) -> v == null || v < place ? place : v);
        List<Record> records = recordCache.getOrDefault(arena, Collections.emptyList());
        if(place < records.size()) {
            return Optional.ofNullable(records.get(place));
        }
        return Optional.empty();
    }

    public void refreshRecords(String arena, List<Record> records) {
        int cacheSize = getCacheSize(arena);
        if(cacheSize == 0) return;
        recordCache.put(arena, records.subList(0, Math.min(cacheSize, records.size())));
    }

    public void refresh() {
        FindIterable<Document> documents = Parkour.getInstance().getPluginDB().getCollection(RECORDS_VIEW).find();
        for(Document document : documents) {
            ArenaRecords arenaRecords = EntityBuilder.deserialize(document, ArenaRecords.class);
            refreshRecords(arenaRecords.getArena(), arenaRecords.getRecords());
        }
    }
}

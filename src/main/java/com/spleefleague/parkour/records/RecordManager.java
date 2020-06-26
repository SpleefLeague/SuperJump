package com.spleefleague.parkour.records;

import com.mongodb.client.FindIterable;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.parkour.Parkour;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RecordManager implements Listener {

    private static final String RECORDS_VIEW = "Records";
    private static final String TOTAL_HIGHSCORE_NAME = "Combined";

    private final Map<String, List<Record>> arenaRecordCache;
    private final Map<UUID, Map<String, Record>> playerRecordCache;
    private final Map<String, Integer> cacheSize;

    public RecordManager() {
        arenaRecordCache = new ConcurrentHashMap<>();
        cacheSize = new ConcurrentHashMap<>();
        playerRecordCache = new HashMap<>();
        for(Player p : Bukkit.getOnlinePlayers()) {
            playerRecordCache.put(p.getUniqueId(), null);
        }
        Bukkit.getScheduler().runTaskTimerAsynchronously(Parkour.getInstance(), this::refresh, 1, 20*60);
        Bukkit.getScheduler().runTaskTimerAsynchronously(Parkour.getInstance(), () -> {
            cacheSize.clear();
            arenaRecordCache.clear();
        }, 0, 20*60*60);
        Bukkit.getPluginManager().registerEvents(this, Parkour.getInstance());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerRecordCache.put(event.getPlayer().getUniqueId(), new HashMap<>());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerRecordCache.remove(event.getPlayer().getUniqueId());
    }

    public int getCacheSize(String arena) {
        return cacheSize.getOrDefault(arena, -1);
    }

    private void updatePlayerRecord(String arena, Record record) {
        Map<String, Record> playerCache = playerRecordCache.get(record.getPlayer());
        if(playerCache != null) {
            playerCache.compute(arena, (k, v) -> v == null || v.getDuration() > record.getDuration() ? record : v);
        }
    }

    private void updateArenaRecord(String arena, Record record) {
        List<Record> records = arenaRecordCache.getOrDefault(arena, Collections.emptyList());
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

    public void submitRecord(String arena, Record record) {
        updatePlayerRecord(arena, record);
        updateArenaRecord(arena, record);
    }

    public Optional<Record> getPlayerRecord(UUID player, String arena) {
        return Optional.ofNullable(playerRecordCache.get(player)).map(m -> m.get(arena));
    }

    public Optional<Record> getArenaRecord(String arena, int place) {
        cacheSize.compute(arena, (k, v) -> v == null || v < place ? place : v);
        List<Record> records = arenaRecordCache.getOrDefault(arena, Collections.emptyList());
        if(place < records.size()) {
            return Optional.ofNullable(records.get(place));
        }
        return Optional.empty();
    }

    public void refreshRecords(String arena, List<Record> records) {
        for(Record record : records) {
            Map<String, Record> playerRecords = playerRecordCache.get(record.getPlayer());
            if(playerRecords == null) continue;
            playerRecords.put(arena, record);
        }
        int cacheSize = getCacheSize(arena) + 1;
        if(cacheSize == 0) return;
        arenaRecordCache.put(arena, records.subList(0, Math.min(cacheSize, records.size())));
    }

    public void refresh() {
        FindIterable<Document> documents = Parkour.getInstance().getPluginDB().getCollection(RECORDS_VIEW).find();
        for(Document document : documents) {
            ArenaRecords arenaRecords = EntityBuilder.deserialize(document, ArenaRecords.class);
            refreshRecords(arenaRecords.getArena(), arenaRecords.getRecords());
        }
        Document cmdResult = Parkour.getInstance().getPluginDB().runCommand(new Document("$eval", "recordTotal()"));
        List<Document> totalDocuments = cmdResult.get("retval", Document.class).get("_batch", List.class);
        List<Record> totalRecords = totalDocuments.parallelStream()
            .map(doc -> EntityBuilder.deserialize(doc, Record.class))
            .collect(Collectors.toList());
        refreshRecords(TOTAL_HIGHSCORE_NAME, totalRecords);
    }
}

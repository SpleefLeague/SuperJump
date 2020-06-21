package com.spleefleague.parkour.signs;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.spleefleague.core.utils.DatabaseConnection;
import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.ParkourMode;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

public class GameRecordSign extends GameSign {

    @DBLoad(fieldName = "template")
    private String[] template;
    @DBLoad(fieldName = "arena")
    private String arena;
    @DBLoad(fieldName = "rank")
    private int rank;

    public static final String PLACEHOLDER_PLAYER = "$PLAYER";
    public static final String PLACEHOLDER_TIME = "$TIME";
    public static final String PLACEHOLDER_ARENA = "$ARENA";
    public static final String PLACEHOLDER_SJMODE = "$MODE";
    public static final String PLACEHOLDER_PLACE = "$PLACE";
    public static final String PLACEHOLDER_DATE = "$DATE";

    @Override
    public void done() {
        if(getSign() == null) {
            System.err.println("Unable to find sign at " + this.getLocation() + ". Found instead: " + this.getLocation().getBlock().getType());
        }
    }

    private Sign getSign() {
        BlockState block = this.getLocation().getBlock().getState();
        if(block instanceof Sign) {
            return (Sign)block;
        }
        return null;
    }

    protected void clear() {
        Sign sign = getSign();
        if(sign == null) return;
        for(int i = 0; i < sign.getLines().length; i++) {
            sign.setLine(i, "");
        }
    }

    protected void refresh() {
        Sign sign = getSign();
        if(sign == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(Parkour.getInstance(), () -> {
            MongoCollection<Document> collection = Parkour.getInstance().getPluginDB().getCollection("Records");
            if(collection == null) return;
            Document doc = collection.aggregate(Arrays.asList(
                    new Document("$match", new Document("_id", arena)),
                    new Document("$unwind", "$records"),
                    new Document("$skip", rank - 1))
            ).first();
            if(doc == null) return;
            Document recordDoc = doc.get("records", Document.class);
            String username = DatabaseConnection.getUsername(UUID.fromString(recordDoc.getString("player")));
            String date = new SimpleDateFormat("YYYY-MM-dd").format(recordDoc.getDate("date"));
            String mode = ParkourMode.valueOf(recordDoc.getString("superjumpMode")).getName();
            Duration duration = Duration.ofMillis(recordDoc.getInteger("duration") * 50);
            String time = String.format("%02d:%02d.%02d", (duration.getSeconds() % 3600) / 60, (duration.getSeconds() % 60), (duration.toMillis() % 1000) / 10);
            Bukkit.getScheduler().runTask(Parkour.getInstance(), () -> {
                for(int i = 0; i < 4; i++) {
                    String line = getLine(i, username, time, mode, date);
                    if(line == null) line = sign.getLine(i);
                    sign.setLine(i, line);
                }
                sign.update();
            });
        });
    }

    private String getLine(int lineId, String player, String time, String mode, String date) {
        if(template[lineId] == null) return null;
        String text = template[lineId].replace(PLACEHOLDER_PLAYER, player)
                .replace(PLACEHOLDER_TIME, time)
                .replace(PLACEHOLDER_ARENA, arena)
                .replace(PLACEHOLDER_SJMODE, mode)
                .replace(PLACEHOLDER_PLACE, Integer.toString(rank))
                .replace(PLACEHOLDER_DATE, date);
        return ChatColor.translateAlternateColorCodes('&', text);

    }

    private static final List<GameRecordSign> gameSigns = new ArrayList<>();
    private static BukkitTask refreshTask;

    public static List<GameRecordSign> initRecordSigns() {
        if(refreshTask != null) {
            refreshTask.cancel();
        }
        gameSigns.forEach(GameRecordSign::clear);
        gameSigns.clear();
        FindIterable<Document> docs = Parkour.getInstance().getPluginDB().getCollection("GameSigns").find(new Document("type", GameSignType.RECORD.name()));
        for(Document doc : docs) {
            gameSigns.add(EntityBuilder.load(doc, GameRecordSign.class));
        }
        Parkour.getInstance().log("Loaded " + gameSigns.size() + " game record signs.");
        refreshTask = Bukkit.getScheduler().runTaskTimer(Parkour.getInstance(), () -> {
            gameSigns.forEach(GameRecordSign::refresh);
        },0,20*30);
        return gameSigns;
    }
}

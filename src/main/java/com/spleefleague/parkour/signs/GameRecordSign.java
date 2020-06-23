package com.spleefleague.parkour.signs;

import com.mongodb.client.FindIterable;
import com.spleefleague.core.utils.DatabaseConnection;
import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.records.RecordManager;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
        if (getSign() == null) return;
        RecordManager rm = Parkour.getInstance().getRecordManager();
        rm.getRecord(this.arena, this.rank - 1).ifPresent(record -> {
            Bukkit.getScheduler().runTaskAsynchronously(Parkour.getInstance(), () -> {
                String username = DatabaseConnection.getUsername(record.getPlayer());
                String date = new SimpleDateFormat("YYYY-MM-dd").format(record.getDate());
                Duration duration = Duration.ofMillis(record.getDuration() * 50);
                String time = String.format("%02d:%02d.%02d", (duration.getSeconds() % 3600) / 60, (duration.getSeconds() % 60), (duration.toMillis() % 1000) / 10);
                Bukkit.getScheduler().runTask(Parkour.getInstance(), () -> {
                    Sign sign = getSign();
                    for (int i = 0; i < 4; i++) {
                        String line = getLine(i, username, time, record.getParkourMode().name(), date);
                        if (line == null) {
                            line = sign.getLine(i);
                        }
                        sign.setLine(i, line);
                    }
                    sign.update();
                });
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
        },0,20*5);
        return gameSigns;
    }
}

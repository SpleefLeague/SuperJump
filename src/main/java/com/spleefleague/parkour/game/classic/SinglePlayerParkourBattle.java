package com.spleefleague.parkour.game.classic;

import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.player.PlayerOptions;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.utils.recording.Recording;
import com.spleefleague.core.utils.recording.RecordingManager;
import com.spleefleague.core.utils.recording.Replay;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.gameapi.events.BattleEndEvent;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.GameHistory;
import com.spleefleague.parkour.game.SinglePlayerGameHistory;
import com.spleefleague.parkour.player.ParkourPlayer;
import com.spleefleague.parkour.records.Record;
import com.spleefleague.parkour.records.RecordManager;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class SinglePlayerParkourBattle extends ClassicParkourBattle {

    private int pingStart;
    private int pingEnd;
    private final ParkourPlayer player;
    private Recording recording;
    private List<Integer> pingLog;
    private BukkitTask pingLoggingTask;
    private Replay recordReplay;
    private CompletableFuture<Recording> recordingFuture;
    private int replayOffset = 0;

    protected SinglePlayerParkourBattle(ClassicParkourArena arena, List<ParkourPlayer> players) {
        super(arena, players);
        player = players.get(0);
        pingLog = new ArrayList<>();
        recordingFuture = initRecording();
    }

    private CompletableFuture<Recording> initRecording() {
        return initRecord()
                .flatMap(record -> record.getRecordingId())
                .map(recordingId -> {
                    CompletableFuture<Recording> recordingFuture = new CompletableFuture<>();
                    Bukkit.getScheduler().runTaskAsynchronously(Parkour.getInstance(), () -> {
                        Recording recording = SpleefLeague.getInstance().getRecordingManager().loadRecording(recordingId);
                        recordingFuture.complete(recording);
                    });
                    return recordingFuture;
                })
                .orElse(CompletableFuture.completedFuture(null));
    }

    private Optional<Record> initRecord() {
        SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(player);
        this.replayOffset = slp.getOptions().getParkourRecordGhostOffset();
        PlayerOptions.ParkourRecordGhostType ghostType = slp.getOptions().getParkourRecordGhostType();
        if(ghostType == null) {
            ghostType = PlayerOptions.ParkourRecordGhostType.NONE;
        }
        RecordManager recordManager = Parkour.getInstance().getRecordManager();
        switch (ghostType) {
            case NONE: return Optional.empty();
            case OWN: return recordManager.getPlayerRecord(slp.getUniqueId(), this.getArena().getName());
            case BEST: return recordManager.getArenaRecord(this.getArena().getName(), 0);
        }
        return Optional.empty();
    }

    public int getPingStart() {
        return pingStart;
    }

    public int getPingEnd() {
        return pingEnd;
    }

    @Override
    protected GameHistory getGameHistory(ParkourPlayer winner, BattleEndEvent.EndReason reason) {
        return new SinglePlayerGameHistory(this, winner, reason);
    }

    public List<Integer> getPingLog() {
        return pingLog;
    }

    @Override
    protected void applyRatingChange(ParkourPlayer winner) {
        int millis = (getDuration() % 20) * 5;
        String s = DurationFormatUtils.formatDuration(getDuration() * 50, "HH:mm:ss", true) + "." + (millis < 10 ? "0" + millis : millis);
        ChatManager.sendMessage(getParkourMode().getChatPrefix(),
                ChatColor.GREEN + "Game in arena "
                        + ChatColor.WHITE + getArena().getName()
                        + ChatColor.GREEN + " is over. " + ChatColor.RED + winner.getName() + ChatColor.GREEN + " has finished in " + s + ".", Parkour.getInstance().getEndMessageChannel());
        Bukkit.getScheduler().runTaskAsynchronously(Parkour.getInstance(), () -> this.getPlayers().forEach((p) -> Parkour.getInstance().getPlayerManager().save(p)));
    }

    @Override
    public void end(ParkourPlayer winner, BattleEndEvent.EndReason reason) {
        if(reason == BattleEndEvent.EndReason.NORMAL && winner != null) {
            Record record = new Record(winner.getUniqueId(), getDuration(), new Date(), arena.getParkourMode());
            Parkour.getInstance().getRecordManager().submitRecord(this.arena.getName(), record);
            recording = SpleefLeague.getInstance().getRecordingManager().stopRecording(player.getUniqueId());
        }
        if(recordReplay != null) {
            recordReplay.cancel();
        }
        pingLoggingTask.cancel();
        pingLog.add(player.getPing());
        this.pingEnd = player.getPing();
        super.end(winner, reason);
    }

    @Override
    protected void saveGameHistory(ParkourPlayer winner, BattleEndEvent.EndReason reason) {
        GameHistory gh = getGameHistory(winner, reason);
        Bukkit.getScheduler().runTaskAsynchronously(Parkour.getInstance(), () -> {
            try {
                if(recording != null && gh instanceof SinglePlayerGameHistory) {
                    ObjectId recordingId = SpleefLeague.getInstance().getRecordingManager().saveRecording(recording);
                    ((SinglePlayerGameHistory) gh).setRecordingId(recordingId);
                }
                EntityBuilder.save(gh, Parkour.getInstance().getPluginDB().getCollection("GameHistory"));
            } catch(Exception e) {
                Parkour.LOG.log(Level.WARNING, "Could not save GameHistory!");
                Document doc = EntityBuilder.serialize(gh).get("$set", Document.class);
                Parkour.LOG.log(Level.WARNING, doc.toJson());
                e.printStackTrace();
            }
        });
    }

    public Integer getAdjustedDuration() {
        if(startTime == null || endTime == null) return 0;
        int minPing = pingLog.stream().mapToInt(i -> i).min().orElse(0);
        if(minPing == 0) return 0;
        Instant endTime = this.endTime == null ? Instant.now() : this.endTime;
        long millis = Duration.between(startTime, endTime).toMillis() - minPing;
        return ((int)millis + 49) / 50; //Get ticks instead of ms
    }

    @Override
    protected void startClock() {
        super.startClock();
        pingLog.clear();
        if(recordReplay != null) {
            recordReplay.cancel();
        }
        Recording recordingToPlay = recordingFuture.getNow(null);
        if(recordingToPlay != null) {
            RecordingManager recordingManager = SpleefLeague.getInstance().getRecordingManager();
            recordReplay = recordingManager.playRecording(recordingToPlay, player, true);
            recordReplay.adjustTicks(replayOffset);
        }
        if(pingLoggingTask == null) {
            pingLoggingTask = Bukkit.getScheduler().runTaskTimer(Parkour.getInstance(), () -> pingLog.add(player.getPing()), 0, 20);
        }
        pingStart = player.getPing();
        SpleefLeague.getInstance().getRecordingManager().startRecording(player);
    }

    @Override
    public void onArenaLeave(ParkourPlayer sjp) {
        super.onArenaLeave(sjp);
        if (inCountdown) return;
        this.startCountdown(1);
    }
}

package com.spleefleague.parkour.game.classic;

import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.utils.recording.Recording;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.gameapi.events.BattleEndEvent;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.*;
import com.spleefleague.parkour.player.ParkourPlayer;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.time.DurationFormatUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;

public class SinglePlayerParkourBattle extends ClassicParkourBattle {

    private int pingStart;
    private int pingEnd;
    private final ParkourPlayer player;
    private Recording recording;

    protected SinglePlayerParkourBattle(ClassicParkourArena arena, List<ParkourPlayer> players) {
        super(arena, players);
        player = players.get(0);
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

    @Override
    protected void applyRatingChange(ParkourPlayer winner) {
        String s = DurationFormatUtils.formatDuration(getDuration() * 50, "HH:mm:ss", true) + "." + (getDuration() % 20) * 5;
        ChatManager.sendMessage(getParkourMode().getChatPrefix(),
                ChatColor.GREEN + "Game in arena "
                        + ChatColor.WHITE + getArena().getName()
                        + ChatColor.GREEN + " is over. " + ChatColor.RED + winner.getName() + ChatColor.GREEN + " has finished in " + s + ".", Parkour.getInstance().getEndMessageChannel());
        Bukkit.getScheduler().runTaskAsynchronously(Parkour.getInstance(), () -> this.getPlayers().forEach((p) -> Parkour.getInstance().getPlayerManager().save(p)));
    }

    @Override
    public void end(ParkourPlayer winner, BattleEndEvent.EndReason reason) {
        if(reason == BattleEndEvent.EndReason.NORMAL && winner != null) {
            recording = SpleefLeague.getInstance().getRecordingManager().stopRecording(player.getUniqueId());
        }
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
        if(startTime == null) return 0;
        Instant endTime = this.endTime == null ? Instant.now() : this.endTime;
        long millis = Duration.between(startTime, endTime).toMillis() - pingStart / 2 - pingEnd / 2;
        return ((int)millis + 49) / 50; //Get ticks instead of ms
    }

    @Override
    protected void startClock() {
        super.startClock();
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

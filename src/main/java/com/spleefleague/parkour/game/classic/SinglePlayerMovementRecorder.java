package com.spleefleague.parkour.game.classic;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.spleefleague.parkour.Parkour;
import org.bukkit.entity.Player;

public class SinglePlayerMovementRecorder extends PacketAdapter {

    public SinglePlayerMovementRecorder() {
        super(Parkour.getInstance(), ListenerPriority.HIGH, new PacketType[]{PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK});
    }

    public void startRecording(Player player) {

    }
}

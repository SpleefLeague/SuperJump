package com.spleefleague.parkour.signs;

import com.spleefleague.core.io.typeconverters.LocationConverter;
import com.spleefleague.entitybuilder.*;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.game.classic.ClassicParkourArena;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public abstract class GameSign extends DBEntity implements DBLoadable {

    @DBLoad(fieldName = "type")
    private GameSignType type;
    @DBLoad(fieldName = "location", typeConverter = LocationConverter.class)
    private Location location;

    public GameSignType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    private static final List<GameSign> gameSigns = new ArrayList<>();

    public static List<GameSign> init() {
        gameSigns.addAll(GameRecordSign.initRecordSigns());
        return gameSigns;
    }
}

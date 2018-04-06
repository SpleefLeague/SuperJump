/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.player;

import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.DBSave;
import com.spleefleague.gameapi.player.RatedPlayer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.ParkourBattle;
import com.spleefleague.parkour.game.Arena;
import com.spleefleague.parkour.game.ParkourMode;
import org.bson.Document;

/**
 *
 * @author Jonas
 */
public class ParkourPlayer extends RatedPlayer<ParkourMode> {
    
    private boolean ingame, frozen, requestingEndgame;
    private Set<Arena> visitedArenas;

    public ParkourPlayer() {
        super(ParkourMode.class, Parkour.getInstance().getPluginDB().getCollection("Players"));
        this.visitedArenas = new HashSet<>();
        setDefaults();
    }

    @DBSave(fieldName = "visitedArenas")
    private List<Document> saveVisitedArenas() {
        List<Document> arenaNames = new ArrayList<>();
        for (Arena arena : visitedArenas) {
            arenaNames.add(new Document("name", arena.getName()).append("type", arena.getParkourMode().name()));
        }
        return arenaNames;
    }

    @DBLoad(fieldName = "visitedArenas")
    private void loadVisitedArenas(List<Document> arenas) {
        for (Document arenaDoc : arenas) {
            try {
                ParkourMode mode = ParkourMode.valueOf(arenaDoc.get("type", String.class));
                Arena arena = Arena.byName(arenaDoc.get("name", String.class), mode);
                if (arena != null) {
                    visitedArenas.add(arena);
                }
            } catch(IllegalArgumentException e) {}
        }
    }

    public Set<Arena> getVisitedArenas() {
        return visitedArenas;
    }

    public void setRequestingEndgame(boolean requestingEndgame) {
        this.requestingEndgame = requestingEndgame;
    }

    public boolean isRequestingEndgame() {
        return requestingEndgame;
    }

    public void setIngame(boolean ingame) {
        this.ingame = ingame;
    }

    public boolean isIngame() {
        return ingame;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public ParkourBattle<?> getCurrentBattle() {
        return Parkour.getInstance().getClassicBattleManager().getBattle(this);
    }
    
    @Override
    public void setDefaults() {
        super.setDefaults();
        this.frozen = false;
        this.ingame = false;
    }
}

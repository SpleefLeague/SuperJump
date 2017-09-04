/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.player;

import com.spleefleague.core.queue.RatedPlayer;
import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.DBSave;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.spleefleague.superjump.SuperJump;
import com.spleefleague.superjump.game.AbstractBattle;
import com.spleefleague.superjump.game.Arena;
import org.bson.Document;

/**
 *
 * @author Jonas
 */
public class SJPlayer extends RatedPlayer {

    private int rating;
    private boolean ingame, frozen, requestingEndgame;
    private Set<Arena> visitedArenas;

    public SJPlayer() {
        visitedArenas = new HashSet<>();
        setDefaults();
    }

    @DBLoad(fieldName = "rating")
    public void setRating(int rating) {
        this.rating = (rating > 0) ? rating : 0;
    }

    @Override
    @DBSave(fieldName = "rating")
    public int getRating() {
        return rating;
    }

    public int getRank() {
        return (int) SuperJump.getInstance().getPluginDB().getCollection("Players").count(new Document("rating", new Document("$gt", rating))) + 1;
    }

    @DBSave(fieldName = "visitedArenas")
    private List<String> saveVisitedArenas() {
        List<String> arenaNames = new ArrayList<>();
        for (Arena arena : visitedArenas) {
            arenaNames.add(arena.getName());
        }
        return arenaNames;
    }

    @DBLoad(fieldName = "visitedArenas")
    private void loadVisitedArenas(List<String> arenaNames) {
        for (String name : arenaNames) {
            Arena arena = Arena.byName(name);
            if (arena != null) {
                visitedArenas.add(arena);
            }
        }
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

    public AbstractBattle getCurrentBattle() {
        return SuperJump.getInstance().getBattleManager().getBattle(this);
    }

    public Set<Arena> getVisitedArenas() {
        return visitedArenas;
    }

    @Override
    public void setDefaults() {
        super.setDefaults();
        this.rating = 1000;
        this.frozen = false;
        this.ingame = false;
    }
}

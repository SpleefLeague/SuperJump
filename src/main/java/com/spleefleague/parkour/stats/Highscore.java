/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.stats;

import com.spleefleague.entitybuilder.DBEntity;
import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.DBLoadable;
import com.spleefleague.entitybuilder.DBSave;
import com.spleefleague.entitybuilder.DBSaveable;
import com.spleefleague.parkour.player.ParkourPlayer;
import com.spleefleague.parkour.variable.SimpleDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;

/**
 *
 * @author NickM13
 */
    
public class Highscore extends DBEntity implements DBLoadable, DBSaveable {
    public SimpleDate date;
    private Map<String, Integer> leaderboard = new HashMap<>();
    private List<String> placements = new ArrayList<>();
    
    public void setPlayerScore(String player, int score) {
        leaderboard.put(player, score);
        for(int i = 0; i < placements.size(); i++) {
            if(placements.get(i).equals(player)) {
                placements.remove(i);
                i--;
            }
        }
        boolean placed = false;
        for(int i = 0; i < placements.size(); i++) {
            if(leaderboard.get(placements.get(i)).intValue() < score) {
                placements.add(i, player);
                placed = true;
                break;
            }
        }
        if(!placed) {
            placements.add(player);
        }
    }
    
    @DBSave(fieldName = "scores")
    public Document saveScores() {
        Document doc = new Document();
        Document docStat = new Document();
        for(int i = 0; i < placements.size(); i++) {
            docStat = new Document("name", placements.get(i)).append("score", leaderboard.get(placements.get(i)).intValue());
            doc.append(String.valueOf(i), docStat);
        }
        return doc;
    }
    
    @DBLoad(fieldName = "scores")
    public void loadScores(Document doc) {
        placements.clear();
        doc.forEach((k, v) -> {
            Document docStats = (Document)v;
            String name = docStats.get("name", String.class);
            int score = docStats.get("score", Integer.class);
            setPlayerScore(name, score);
        });
    }
    
    public LPlayer getTopPlayer() {
        if(placements.size() > 0) {
            return new LPlayer(placements.get(0), leaderboard.get(placements.get(0)).intValue());
        }
        else {
            return new LPlayer("", 0);
        }
    }
    
    public int getPlayerPlacement(String player) {
        for(int i = 0; i < placements.size(); i++) {
            if(placements.get(i).equals(player)) {
                return i + 1;
            }
        }
        return 0;
    }
    
    public class LPlayer {
        public String player;
        public int score;
        
        public LPlayer(String player, int score) {
            this.player = player;
            this.score = score;
        }
    }
    
    @DBLoad(fieldName = "date")
    private void loadDate(Document docDate) {
        int year, month, day;
        try {
            year = docDate.get("year", Integer.class);
            month = docDate.get("month", Integer.class);
            day = docDate.get("day", Integer.class);
            date = new SimpleDate(year, month, day);
        } catch(Exception e) {
            System.out.println("Failed to load highscore data:\n" + docDate);
            e.printStackTrace();
        }
    }
    
    @DBSave(fieldName = "date")
    private Document saveDate() {
        return (new Document("year", date.year).append("month", date.month).append("day", date.day));
    }

    public Highscore() {
        this.date = new SimpleDate();
    }

    public Highscore(SimpleDate date) {
        this.date = date;
    }
}

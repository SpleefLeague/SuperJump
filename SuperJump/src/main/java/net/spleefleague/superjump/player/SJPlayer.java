/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.spleefleague.superjump.player;

import net.spleefleague.core.io.DBLoad;
import net.spleefleague.core.io.DBSave;
import net.spleefleague.core.player.GeneralPlayer;

/**
 *
 * @author Jonas
 */
public class SJPlayer extends GeneralPlayer {
    
    private int rating;
    private boolean ingame, frozen;
    
    @DBLoad(fieldName = "rating")
    public void setRating(int rating) {
        this.rating = rating;
    }
    
    @DBSave(fieldName = "rating")
    public int getRating() {
        return rating;
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
    
    @Override
    public void setDefaults() {
        super.setDefaults();
        this.rating = 1000;
        this.frozen = false;
        this.ingame = false;
    }
}
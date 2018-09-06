/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game;

import com.spleefleague.parkour.Parkour;
import org.bukkit.ChatColor;

/**
 *
 * @author jonas
 */
public enum ParkourMode {
    NONE        ("None"),
    CONQUEST    ("Conquest"),
    ENDLESS     ("Endless"),
    MEMORY      ("Memory"),
    PARTY       ("Party"),
    PRACTICE    ("Practice"),
    PRO         ("Pro"),
    CLASSIC     ("Classic"),
    SHUFFLE     ("Shuffle"),
    REQUEUE     ("Requeueing");
    
    private final String name;
    
    private ParkourMode(String chatPrefixName) {
        this.name = chatPrefixName;
    }
    
    public String getChatPrefix() {
        return Parkour.fillColor + "[" + Parkour.modeColor + name + Parkour.fillColor + "]" + ChatColor.RESET;
    }
    
    public String getName() {
        return name;
    }
}

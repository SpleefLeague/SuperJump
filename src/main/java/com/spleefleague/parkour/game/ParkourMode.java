/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game;

import org.bukkit.ChatColor;

/**
 *
 * @author jonas
 */
public enum ParkourMode {
    CLASSIC("SuperJump");
    
    private final String name;
    
    private ParkourMode(String chatPrefixName) {
        this.name = chatPrefixName;
    }
    
    public String getChatPrefix() {
        return ChatColor.GRAY + "[" + ChatColor.GOLD + name + ChatColor.GRAY + "]" + ChatColor.RESET;
    }
    
    public String getName() {
        return name;
    }
}

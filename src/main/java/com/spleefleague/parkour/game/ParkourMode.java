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
    CLASSIC("ClassicSJ");
    
    private final String chatPrefixName;
    
    private ParkourMode(String chatPrefixName) {
        this.chatPrefixName = chatPrefixName;
    }
    
    public String getChatPrefix() {
        return ChatColor.GRAY + "[" + ChatColor.GOLD + chatPrefixName + ChatColor.GRAY + "]" + ChatColor.RESET;
    }
}

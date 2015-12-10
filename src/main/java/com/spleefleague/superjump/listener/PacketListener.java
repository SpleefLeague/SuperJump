/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.superjump.listener;

import com.comphenix.packetwrapper.WrapperPlayClientBlockDig;
import com.comphenix.packetwrapper.WrapperPlayServerMapChunk;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerDigType;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.player.PlayerState;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.superjump.SuperJump;
import com.spleefleague.superjump.game.Battle;
import com.spleefleague.superjump.game.Battle.PlayerData;
import com.spleefleague.superjump.player.SJPlayer;
import org.bukkit.entity.Player;

/**
 *
 * @author Jonas
 */
public class PacketListener {
    
    private final ProtocolManager manager;
    
    private static PacketListener instance;

    public static void init() {
        if (instance == null) {
            instance = new PacketListener();
        }
    }

    private PacketListener() {
        manager = ProtocolLibrary.getProtocolManager();
        initPacketListeners();
    }
    
    private void initPacketListeners() {
        this.manager.addPacketListener(
            new PacketAdapter(SuperJump.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Client.BLOCK_DIG) {

                @Override
                public void onPacketReceiving(PacketEvent event) {
                    WrapperPlayClientBlockDig blockChange = new WrapperPlayClientBlockDig(event.getPacket());
                    Player player = event.getPlayer();
                    SJPlayer sjp = SuperJump.getInstance().getPlayerManager().get(player);
                    if(sjp.isIngame()){
                        if(blockChange.getStatus() == PlayerDigType.STOP_DESTROY_BLOCK) {
                            event.setCancelled(true);
                        }
                    }
                }
                
                @Override
                public void onPacketSending(PacketEvent event) {
                    
                }
            }
        );
        this.manager.addPacketListener(
            new PacketAdapter(SuperJump.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Client.BLOCK_PLACE) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    SJPlayer player = SuperJump.getInstance().getPlayerManager().get(event.getPlayer());
                    if(player.isIngame()) {
                        event.setCancelled(true);
                    }
                }
                
                @Override
                public void onPacketSending(PacketEvent event) {
                    
                }
            }
        );
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.ParkourBattle;
import com.spleefleague.parkour.game.classic.ClassicParkourBattle;
import com.spleefleague.parkour.player.ParkourPlayer;
import java.lang.reflect.InvocationTargetException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;

/**
 *
 * @author Jonas
 */
public class ConnectionListener implements Listener {

    private static Listener instance;

    public static void init() {
        if (instance == null) {
            instance = new ConnectionListener();
            Bukkit.getPluginManager().registerEvents(instance, Parkour.getInstance());
        }
    }

    private ConnectionListener() {

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
        if (sjp == null) {
            return;
        }
        if (sjp.isIngame()) {
            Parkour.getInstance().getClassicBattleManager().getBattle(sjp).removePlayer(sjp, false);
        } else {
            Parkour.getInstance().getClassicBattleManager().dequeue(sjp);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        List<Player> ingamePlayers = new ArrayList<>();
        List<ParkourBattle<?>> toCancel = new ArrayList<>();//Workaround
        for (ParkourBattle<?> battle : Parkour.getInstance().getClassicBattleManager().getAll()) {
            for (ParkourPlayer p : battle.getActivePlayers()) {
                if (p.getPlayer() != null) {
                    event.getPlayer().hidePlayer(p.getPlayer());
                    p.getPlayer().hidePlayer(event.getPlayer());
                    ingamePlayers.add(p.getPlayer());
                } else {
                    toCancel.add(battle);
                    break;
                }
            }
        }
        for (ParkourBattle<?> battle : toCancel) {
            for (ParkourPlayer p : battle.getActivePlayers()) {
                if (p.getPlayer() != null) {
                    p.kickPlayer("An error has occured. Please reconnect");
                }
            }
            if(battle instanceof ClassicParkourBattle) {
                Parkour.getInstance().getClassicBattleManager().remove((ClassicParkourBattle)battle);
            }
        }
        Bukkit.getScheduler().runTaskLater(Parkour.getInstance(), () -> {
            List<PlayerInfoData> list = new ArrayList<>();
            SpleefLeague.getInstance().getPlayerManager().getAll().forEach((SLPlayer slPlayer) -> list.add(new PlayerInfoData(WrappedGameProfile.fromPlayer(slPlayer.getPlayer()), ((CraftPlayer) slPlayer.getPlayer()).getHandle().ping, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(slPlayer.getRank().getColor() + slPlayer.getName()))));
            PacketContainer packetContainer = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
            packetContainer.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            packetContainer.getPlayerInfoDataLists().write(0, list);
            for (Player p : ingamePlayers) {
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(p, packetContainer);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(ConnectionListener.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            list.clear();
            ingamePlayers.forEach((Player p) -> {
                SLPlayer generalPlayer = SpleefLeague.getInstance().getPlayerManager().get(p);
                list.add(new PlayerInfoData(WrappedGameProfile.fromPlayer(p), ((CraftPlayer) p).getHandle().ping, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(generalPlayer.getRank().getColor() + generalPlayer.getName())));
            });
            packetContainer.getPlayerInfoDataLists().write(0, list);
            for (Player p : SpleefLeague.getInstance().getPlayerManager().getAll()) {
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(p, packetContainer);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(ConnectionListener.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }, 10);
    }
}

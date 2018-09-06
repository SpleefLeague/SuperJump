/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.shop;

import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.player.SLPlayer;
import static com.spleefleague.core.utils.inventorymenu.InventoryMenuAPI.item;
import com.spleefleague.core.utils.inventorymenu.InventoryMenuClickEvent;
import com.spleefleague.core.utils.inventorymenu.InventoryMenuClickListener;
import com.spleefleague.core.utils.inventorymenu.InventoryMenuItemTemplateBuilder;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.player.ParkourPlayer;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;

/**
 *
 * @author NickM13
 */
public class ShopItem {
    
    private static final List<ShopItem> ITEMS = new ArrayList<>();
    
    private int menuLoc;
    private String itemName;
    private int paragonCost;
    private InventoryMenuItemTemplateBuilder item;
    private InventoryMenuClickListener clickListener;
    
    static {
        ITEMS.add(new ShopItem(0, Parkour.arenaColor + "1000 SL Coins", 1, item()
                .displayIcon(Material.GOLDEN_APPLE),
                ((InventoryMenuClickEvent event) -> {
                    ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
                    SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer());
                    slp.setCoins(slp.getCoins() + 1000);
                })));
    }
    
    public static List<ShopItem> getItemList() {
        return ITEMS;
    }
    
    public ShopItem(int menuLoc, String itemName, int paragonCost, InventoryMenuItemTemplateBuilder item, InventoryMenuClickListener clickListener) {
        this.menuLoc = menuLoc;
        this.itemName = itemName;
        this.paragonCost = paragonCost;
        this.item = item;
        this.item.displayName(itemName);
        this.item.description(((SLPlayer slp) -> {
                    List<String> description = new ArrayList<>();
                    ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
                    description.add(Parkour.fillColor + "");
                    description.add(Parkour.fillColor + "Paragon Points: " 
                                  + Parkour.pointColor + sjp.getParagonPoints()
                                  + Parkour.fillColor + " out of "
                                  + Parkour.pointColor + paragonCost);
                    return description;
                }));
        this.item.onClick((InventoryMenuClickEvent event) -> {
                ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
                if(sjp.buyWithParagonPoints(1)) {
                    clickListener.onClick(event);
                    SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer());
                    ChatManager.sendMessagePlayer(slp, Parkour.getInstance().getChatPrefix()
                            + Parkour.fillColor + " You purchased "
                            + Parkour.arenaColor + this.getItemName()
                            + Parkour.fillColor + " for "
                            + Parkour.pointColor + this.getParagonCost()
                            + Parkour.fillColor + " Paragon Point" + (this.getParagonCost() != 1 ? "s" : ""));
                }
                });
    }
    
    public int getMenuLoc() {
        return menuLoc;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public int getParagonCost() {
        return paragonCost;
    }
    
    public InventoryMenuItemTemplateBuilder getItem() {
        return item;
    }
    
}

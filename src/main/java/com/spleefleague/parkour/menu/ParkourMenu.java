/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.menu;

import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.chat.Theme;
import com.spleefleague.core.menus.SLMenu;
import com.spleefleague.core.player.PlayerState;
import com.spleefleague.core.player.SLPlayer;
import static com.spleefleague.core.utils.inventorymenu.InventoryMenuAPI.dialog;
import static com.spleefleague.core.utils.inventorymenu.InventoryMenuAPI.dialogButton;
import static com.spleefleague.core.utils.inventorymenu.InventoryMenuAPI.dialogMenu;
import static com.spleefleague.core.utils.inventorymenu.InventoryMenuAPI.item;
import static com.spleefleague.core.utils.inventorymenu.InventoryMenuAPI.menu;
import com.spleefleague.core.utils.inventorymenu.InventoryMenuComponentFlag;
import com.spleefleague.core.utils.inventorymenu.InventoryMenuFlag;
import com.spleefleague.core.utils.inventorymenu.InventoryMenuItemTemplateBuilder;
import com.spleefleague.core.utils.inventorymenu.InventoryMenuTemplate;
import com.spleefleague.core.utils.inventorymenu.InventoryMenuTemplateBuilder;
import com.spleefleague.core.utils.inventorymenu.dialog.InventoryMenuDialogButtonTemplateBuilder;
import com.spleefleague.core.utils.inventorymenu.dialog.InventoryMenuDialogFlag;
import com.spleefleague.core.utils.inventorymenu.dialog.InventoryMenuDialogHolderTemplateBuilder;
import com.spleefleague.core.utils.inventorymenu.dialog.InventoryMenuDialogTemplateBuilder;
import com.spleefleague.gameapi.events.BattleStartEvent;
import com.spleefleague.gameapi.queue.Challenge;
import com.spleefleague.gameapi.queue.GameQueue;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.Arena;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.game.conquest.ConquestParkourArena;
import com.spleefleague.parkour.game.endless.EndlessParkourArena;
import com.spleefleague.parkour.game.memory.MemoryParkourArena;
import com.spleefleague.parkour.game.versus.classic.VersusClassicParkourArena;
import com.spleefleague.parkour.game.versus.classic.VersusClassicParkourBattle;
import com.spleefleague.parkour.game.versus.shuffle.VersusShuffleParkourArena;
import com.spleefleague.parkour.game.versus.shuffle.VersusShuffleParkourBattle;
import com.spleefleague.parkour.player.ParkourPlayer;
import com.spleefleague.parkour.shop.ShopItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 *
 * @author NickM13
 */
public class ParkourMenu {
    
    private static final int WRAP_SIZE = 40;
    
    private static void wrapDescription(List<String> description, String line) {
        String l = Parkour.fillColor + "";
        int c = 0;
        for(String word : line.split(" ")) {
            word = word.concat(" ");
            if(c + word.length() > WRAP_SIZE) {
                description.add(l);
                l = Parkour.fillColor + word;
                c = word.length();
            }
            else {
                l = l.concat(word);
                c += word.length();
            }
        }
        description.add(l);
    }
    
    private static Function<SLPlayer, List<String>> getSuperJumpDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            wrapDescription(description, "Jump and run your way to this finish line in this fast paced parkour gamemode.");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Currently Playing: " + Parkour.pointColor 
                    + (Parkour.getInstance().getBattleManager(ParkourMode.CLASSIC).getAll().size() * 2
                            + Parkour.getInstance().getBattleManager(ParkourMode.CONQUEST).getAll().size()
                            + Parkour.getInstance().getBattleManager(ParkourMode.ENDLESS).getAll().size()
                            + Parkour.getInstance().getBattleManager(ParkourMode.PARTY).getAll().size()
                            + Parkour.getInstance().getBattleManager(ParkourMode.PRACTICE).getAll().size()
                            + Parkour.getInstance().getBattleManager(ParkourMode.PRO).getAll().size()
                            + Parkour.getInstance().getBattleManager(ParkourMode.SHUFFLE).getAll().size() * 2));
            return description;
        };
    }
    
    private static Function<SLPlayer, List<String>> getSoloDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            wrapDescription(description, "Relax and play a variety of different parkour gametypes in this singleplayer take on SuperJump.");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Currently Playing: " + Parkour.pointColor 
                    + (Parkour.getInstance().getBattleManager(ParkourMode.ENDLESS).getAll().size()
                            + Parkour.getInstance().getBattleManager(ParkourMode.CONQUEST).getAll().size()));
            return description;
        };
    }
    
    private static Function<SLPlayer, List<String>> getVersusDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            wrapDescription(description, "Clash against other players in a race to the finish line in this competitive take on SuperJump.");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Currently Playing: " + Parkour.pointColor 
                    + (Parkour.getInstance().getBattleManager(ParkourMode.CLASSIC).getAll().size() 
                            + Parkour.getInstance().getBattleManager(ParkourMode.SHUFFLE).getAll().size()));
            return description;
        };
    }
    
    private static Function<SLPlayer, List<String>> getChallengeDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            wrapDescription(description, "Challenge another player to a match of SuperJump!");
            return description;
        };
    }
    
    private static Function<SLPlayer, List<String>> getClassicDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            wrapDescription(description, "Play against another player on a preset field of jumps.  The first to reach the finish line wins!");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Currently Playing: " + Parkour.pointColor 
                    + (Parkour.getInstance().getBattleManager(ParkourMode.CLASSIC).getAll().size()));
            return description;
        };
    }
    
    private static Function<SLPlayer, List<String>> getShuffleDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            wrapDescription(description, "Compete against another player on a field of randomly generated jumps of four different difficulties.");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Currently Playing: " 
                    + Parkour.pointColor + (Parkour.getInstance().getBattleManager(ParkourMode.SHUFFLE).getAll().size()));
            return description;
        };
    }
    
    private static Function<SLPlayer, List<String>> getRandomDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.GRAY + "Queue for all Versus arenas.");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.GRAY + "Bonus multiplier: "
                            + Parkour.pointColor + "x0.40");
            return description;
        };
    }
    
    private static Function<SLPlayer, List<String>> getConquestDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            wrapDescription(description, "Crawl your way through a diverse cast of parkour series in SpleefLeague's premier solo parkour mode.");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Currently Playing: " + Parkour.pointColor 
                    + (Parkour.getInstance().getBattleManager(ParkourMode.CONQUEST).getAll().size()));
            return description;
        };
    }
    
    private static Function<SLPlayer, List<String>> getEndlessDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            wrapDescription(description, "In this singleplayer mode, move on endlessly through random, progressively more challenging levels!");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Currently Playing: " + Parkour.pointColor 
                    + (Parkour.getInstance().getBattleManager(ParkourMode.ENDLESS).getAll().size()));
            return description;
        };
    }
    
    private static Function<SLPlayer, List<String>> getPackDescription(ConquestParkourArena.Pack pack) {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            wrapDescription(description, "");
            description.add(ChatColor.GRAY + "This is a description");
            description.add(ChatColor.GRAY + "for a pack!");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.GRAY + "Stars Collected: " 
                    + Parkour.pointColor + sjp.getConquestStarsPack(pack)
                    + ChatColor.GRAY + "/"
                    + Parkour.pointColor + (pack.getArenas().size() * 3));
            int played = sjp.getConquestPlayedPack(pack);
            description.add(ChatColor.GRAY + "Maps Completed: " 
                    + (played == pack.getArenas().size() ? Parkour.pointColor : ChatColor.GRAY) + sjp.getConquestPlayedPack(pack)
                    + ChatColor.GRAY + "/"
                    + Parkour.pointColor + (pack.getArenas().size()));
            return description;
        };
    }
    
    private static Function<SLPlayer, List<String>> getProfileDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.GRAY + "Point Multiplier: " + Parkour.pointColor + "x" + String.format("%.2f", sjp.getPointMultiplier()));
            description.add(ChatColor.GRAY + "Total Points: " + Parkour.pointColor + sjp.getPoints());
            description.add(ChatColor.GRAY + "Paragon Level: " + Parkour.pointColor + sjp.getParagonLevel());
            description.add(ChatColor.GRAY + "Paragon Points: " + Parkour.pointColor + sjp.getParagonPoints());
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.GRAY + "Versus Wins: " + Parkour.pointColor + sjp.getVersusWins());
            description.add(ChatColor.GRAY + "Versus Games: " + Parkour.pointColor + (sjp.getVersusWins() + sjp.getVersusLosses()));
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.GRAY + "Conquest Stars Earned: " + Parkour.pointColor + sjp.getConquestStarsTotal());
            description.add(ChatColor.GRAY + "Endless Levels Completed: " + Parkour.pointColor + sjp.getEndlessLevelsTotal());
            return description;
        };
    }
    
    private static Function<SLPlayer, List<String>> getShopDescription() {
        return (SLPlayer slp) -> {
            List<String> description = new ArrayList<>();
            ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(slp.getUniqueId());
            description.add(ChatColor.GRAY + "");
            wrapDescription(description, "This is a description for the Paragon shop.");
            description.add(ChatColor.GRAY + "");
            description.add(ChatColor.WHITE + "" + ChatColor.BOLD + "Paragon Points: " 
                    + Parkour.pointColor + sjp.getParagonPoints());
            return description;
        };
    }
    
    private static void addBackButton(InventoryMenuTemplateBuilder from, InventoryMenuTemplate to) {
        from.component(8, item()
                .displayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Back")
                .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short) 9))
                .onClick(event -> {
                    to.construct(null, SpleefLeague.getInstance().getPlayerManager().get(event.getPlayer())).open();
                }));
    }
    
    private static void addBackButtonArenas(InventoryMenuTemplateBuilder from, InventoryMenuTemplate to) {
        from.component(26, item()
                .displayName(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Back")
                .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short) 9))
                .onClick(event -> {
                    to.construct(null, SpleefLeague.getInstance().getPlayerManager().get(event.getPlayer())).open();
                }));
    }
    
    private static void addGamemodeMenu(InventoryMenuTemplateBuilder menu, ParkourMode parkourMode) {
        switch(parkourMode) {
            case CLASSIC:
                menu.description(getClassicDescription());
                VersusClassicParkourArena.getAll().stream().forEach(arena -> {
                    menu.component(arena.getMenuPos(), item()
                            .displayName(Parkour.arenaColor + "" + ChatColor.BOLD + arena.getName() + " " + arena.getDifficultyStars())
                            .description(arena.getDynamicDescription())
                            .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short)23))
                            .onClick((event) -> {
                                ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
                                if (arena.isAvailable(sjp)) {
                                    if (arena.isOccupied()) {
                                        ((VersusClassicParkourBattle) Parkour.getInstance().getBattleManager(ParkourMode.CLASSIC).getBattle(arena)).addSpectator(sjp, null);
                                    } else if (!arena.isPaused()) {
                                        Parkour.getInstance().queuePlayer(sjp, arena, true);
                                        event.getItem().getParent().update();
                                    }
                                }
                            })
                    );
                });
                break;
            case SHUFFLE:
                menu.description(getShuffleDescription());
                VersusShuffleParkourArena.getAll().stream().forEach(arena -> {
                    menu.component(arena.getMenuPos(), item()
                            .displayName(Parkour.arenaColor + "" + ChatColor.BOLD + arena.getName() + " " + arena.getDifficultyStars())
                            .description(arena.getDynamicDescription())
                            .displayItem(arena.getMenuItem())
                            .onClick((event) -> {
                                ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
                                if (arena.isAvailable(sjp)) {
                                    if (arena.isOccupied()) {
                                        ((VersusShuffleParkourBattle) Parkour.getInstance().getBattleManager(ParkourMode.SHUFFLE).getBattle(arena)).addSpectator(sjp, null);
                                    } else if (!arena.isPaused()) {
                                        Parkour.getInstance().queuePlayer(sjp, arena, true);
                                        event.getItem().getParent().update();
                                    }
                                }
                            })
                    );
                });
                break;
            case CONQUEST:
                Map<String, InventoryMenuTemplateBuilder> packMenu = new HashMap<>();
                ConquestParkourArena.getAllPacks().stream().forEach(pack -> {
                    packMenu.put(pack.getName(), menu()
                            .title(pack.getName())
                            .displayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + pack.getName())
                            .description(getPackDescription(pack))
                            .visibilityController(slp -> {
                                return Parkour.getInstance().getPlayerManager().get(slp.getPlayer()).hasConquestPack(pack);
                            })
                            .displayItem(new ItemStack(Material.CONCRETE, 1, (short)5)));
                    menu.component(pack.getMenuLoc(), packMenu.get(pack.getName()));
                    addBackButton(packMenu.get(pack.getName()), menu.build());
                });
                menu.description(getConquestDescription());
                ConquestParkourArena.getAll().stream().forEach(arena -> {
                    if(packMenu.containsKey(arena.getPack())) {
                        packMenu.get(arena.getPack()).component(arena.getMenuPos(), item()
                                .displayName(Parkour.arenaColor + "" + ChatColor.BOLD + arena.getName() + " " + arena.getDifficultyStars())
                                .description(arena.getDynamicDescription())
                                .displayIcon((slp) -> (arena.isAvailable(Parkour.getInstance().getPlayerManager().get(slp)) ? Material.MAP : Material.EMPTY_MAP))
                                .onClick((event) -> {
                                    ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
                                    if (arena.isAvailable(sjp)) {
                                        if (arena.isOccupied()) {
                                            ((VersusShuffleParkourBattle) Parkour.getInstance().getBattleManager(ParkourMode.CONQUEST).getBattle(arena)).addSpectator(sjp, null);
                                        } else if (!arena.isPaused()) {
                                            Parkour.getInstance().queuePlayer(sjp, arena, true);
                                            event.getItem().getParent().update();
                                        }
                                    }
                                })
                        );
                    }
                });
                break;
            default: break;
        }
    }
    
    private static ParkourPlayer getSJP(Player p) {
        return Parkour.getInstance().getPlayerManager().get(p);
    }
    
    private static InventoryMenuDialogTemplateBuilder<MenuChallenge> createChallengeDialog(InventoryMenuDialogHolderTemplateBuilder<MenuChallenge> arenaSelector, ParkourMode mode) {
        InventoryMenuDialogHolderTemplateBuilder<MenuChallenge> challengeSelectPlayerMenu = dialogMenu(MenuChallenge.class)
                .title("Select a player")
                .unsetFlags(InventoryMenuComponentFlag.EXIT_ON_NO_PERMISSION);
        Parkour.getInstance().getPlayerManager()
                .getAll()
                .stream()
                .sorted((p1, p2) -> p1.getName().compareTo(p2.getName()))
                .forEach(p -> {
                    ItemStack skull = new ItemStack(Material.SKULL_ITEM);
                    skull.setDurability((short) 3);
                    SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                    skullMeta.setOwner(p.getName());
                    skull.setItemMeta(skullMeta);
                    challengeSelectPlayerMenu.component(dialogButton(MenuChallenge.class)
                            .displayItem(skull)
                            .visibilityController(slp -> getSJP(slp) != p)
                            .displayName(ChatColor.RED + "" + ChatColor.BOLD + p.getName())
                            .description(x -> {
                                List<String> lines = new ArrayList<>();
                                SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(p);
                                if(slp.getState() == PlayerState.INGAME) {
                                    lines.add(ChatColor.RED + "" + ChatColor.ITALIC + "Ingame");
                                }
                                lines.add(ChatColor.GRAY + "Paragon: " + Parkour.pointColor + p.getParagonLevel());
                                return lines;
                            })
                            .accessController(slp -> SpleefLeague.getInstance().getPlayerManager().get(p).getState() != PlayerState.INGAME)
                            .onClick(e -> {
                                e.getBuilder().setTarget(p);
                            })
                    );
                });
        challengeSelectPlayerMenu.next(arenaSelector);
        return dialog(MenuChallenge.class)
                .start(challengeSelectPlayerMenu)
                .flags(InventoryMenuDialogFlag.EXIT_ON_COMPLETE_DIALOG)
                .builder(slp -> new MenuChallenge(getSJP(slp)));
                
    }
    
    private static InventoryMenuDialogHolderTemplateBuilder<MenuChallenge> createArenaChallengeDialog(ParkourMode mode) {
        InventoryMenuDialogHolderTemplateBuilder<MenuChallenge> builder = dialogMenu(MenuChallenge.class);
        GameQueue<? extends Arena, ParkourPlayer> queue = Parkour.getInstance().getBattleManager(mode).getGameQueue();
        List<Arena<?>> arenasSorted = null;
        switch(mode) {
            case CLASSIC:
                arenasSorted = new ArrayList<>(VersusClassicParkourArena.getAll());
                Collections.sort(arenasSorted, (a1, a2) -> a1.getName().compareTo(a2.getName()));
                for(Arena arena : arenasSorted) {
                    if(arena.getParkourMode() == mode) {
                        InventoryMenuDialogButtonTemplateBuilder<MenuChallenge> itemBuilder = dialogButton(MenuChallenge.class)
                                .displayName(Parkour.arenaColor + "" + ChatColor.BOLD + arena.getName() + " " + arena.getDifficultyStars())
                                .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short)23))
                                .description(arena.getDynamicDescription());
                        itemBuilder.onClick(b -> b.getBuilder().setArena(arena));
                        builder.component(arena.getMenuPos(), itemBuilder);
                    }
                }
                break;
            case SHUFFLE:
                arenasSorted = new ArrayList<>(VersusShuffleParkourArena.getAll());
                Collections.sort(arenasSorted, (a1, a2) -> a1.getName().compareTo(a2.getName()));
                for(Arena arena : arenasSorted) {
                    if(arena.getParkourMode() == mode) {
                        InventoryMenuDialogButtonTemplateBuilder<MenuChallenge> itemBuilder = dialogButton(MenuChallenge.class)
                                .displayName(Parkour.arenaColor + "" + ChatColor.BOLD + arena.getName() + " " + arena.getDifficultyStars())
                                .displayItem(arena.getMenuItem())
                                .description(arena.getDynamicDescription());
                        itemBuilder.onClick(b -> b.getBuilder().setArena(arena));
                        builder.component(arena.getMenuPos(), itemBuilder);
                    }
                }
                break;
        }
        return builder;
    }
    
    private static void performChallenge(MenuChallenge builder, ParkourMode mode) {
        Collection<SLPlayer> targets = Arrays.asList(builder.getTarget());
        ParkourPlayer spSource = getSJP(builder.getSource());
        ParkourPlayer spTarget = getSJP(builder.getTarget());
        Challenge<ParkourPlayer> challenge = new Challenge<ParkourPlayer>(spSource, Arrays.asList(spTarget)) {
            @Override
            public void start(List<ParkourPlayer> players) {
                Arena arena = builder.getArena();
                if (arena == null) {
                    List<Arena> potentialArenas = Arena.getAll()
                            .stream()
                            .filter(a -> a.getParkourMode() == mode)
                            .filter(a -> !a.isOccupied() && !a.isPaused() && a.isRated() && a.isQueued())
                            .filter(a -> a.getRequiredPlayers() <= players.size())
                            .filter(a -> {
                                for (ParkourPlayer sjp : players) {
                                    if (!a.isAvailable(sjp)) {
                                        return false;
                                    }
                                }
                                return true;
                            })
                            .collect(Collectors.toList());
                    if (potentialArenas.isEmpty()) {
                        builder.getSource().sendMessage(Parkour.getInstance().getChatPrefix() + net.md_5.bungee.api.ChatColor.RED + " There are currently no arenas available.");
                    }
                    Collections.shuffle(potentialArenas);
                    arena = potentialArenas.get(0);
                }
                arena.startBattle(players, BattleStartEvent.StartReason.CHALLENGE);
                for (ParkourPlayer sjp : players) {
                    sjp.setParkourMode(mode);
                    sjp.clearLastArenas();
                    sjp.addLastArena(arena);
                }
            }
        };
        challenge.sendMessages(Parkour.getInstance().getChatPrefix(), builder.getArena() == null ? "a random arena" : builder.getArena().getName(), targets);
    }
    
    
    
    
    
    public static InventoryMenuTemplateBuilder createSuperJumpMenu(InventoryMenuTemplateBuilder parent) {
        InventoryMenuTemplateBuilder menu = SLMenu.getNewGamemodeMenu()
                .title("SuperJump")
                .displayName(ChatColor.GOLD + "" + ChatColor.BOLD + "SuperJump")
                .description(getSuperJumpDescription())
                .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short) 5))
                .flags(InventoryMenuFlag.EXIT_ON_CLICK_OUTSIDE);
        menu.component(2, createShopMenu(menu));
        menu.component(3, () -> createVersusMenu(menu).build());
        menu.component(4, createProfileItem());
        menu.component(5, createSoloMenu(menu));
        addBackButton(menu, parent.build());
        return menu;
    }
    
    public static InventoryMenuTemplateBuilder createVersusMenu(InventoryMenuTemplateBuilder parent) {
        InventoryMenuTemplateBuilder menu = new InventoryMenuTemplateBuilder()
                .title("SuperJump: Versus")
                .displayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Versus")
                .description(getVersusDescription())
                .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short) 22))
                .flags(InventoryMenuFlag.EXIT_ON_CLICK_OUTSIDE);
        menu.component(1, createChallengeMenu(menu));
        menu.component(3, createClassicMenu(menu));
        menu.component(4, createRandomItem());
        menu.component(5, createShuffleMenu(menu));
        addBackButton(menu, parent.build());
        return menu;
    }
    
    public static InventoryMenuDialogTemplateBuilder createClassicChallenge() {
        return createChallengeDialog(createArenaChallengeDialog(ParkourMode.CLASSIC), ParkourMode.CLASSIC)
                        .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short) 6))
                        .displayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Classic")
                        .onDone((slp, builder) -> performChallenge(builder, ParkourMode.CLASSIC));
    }
    
    public static InventoryMenuDialogTemplateBuilder createShuffleChallenge() {
        return createChallengeDialog(createArenaChallengeDialog(ParkourMode.SHUFFLE), ParkourMode.SHUFFLE)
                        .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short) 24))
                        .displayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Shuffle")
                        .onDone((slp, builder) -> performChallenge(builder, ParkourMode.SHUFFLE));
    }
    
    public static InventoryMenuTemplateBuilder createChallengeMenu(InventoryMenuTemplateBuilder parent) {
        InventoryMenuTemplateBuilder menu = new InventoryMenuTemplateBuilder()
                .title("SuperJump: Challenge")
                .displayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Challenge")
                .description(getChallengeDescription())
                .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short) 10))
                .flags(InventoryMenuFlag.EXIT_ON_CLICK_OUTSIDE);
        menu.component(3, createClassicChallenge());
        menu.component(5, createShuffleChallenge());
        addBackButton(menu, parent.build());
        return menu;
    }
    
    public static InventoryMenuTemplateBuilder createClassicMenu(InventoryMenuTemplateBuilder parent) {
        InventoryMenuTemplateBuilder menu = new InventoryMenuTemplateBuilder()
                .title("SuperJump: Classic")
                .displayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Classic")
                .description(getClassicDescription())
                .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short) 6))
                .flags(InventoryMenuFlag.EXIT_ON_CLICK_OUTSIDE);
        addGamemodeMenu(menu, ParkourMode.CLASSIC);
        addBackButtonArenas(menu, parent.build());
        return menu;
    }
    
    public static InventoryMenuItemTemplateBuilder createRandomItem() {
        InventoryMenuItemTemplateBuilder item = item()
                .displayName(Parkour.arenaColor + "" + ChatColor.BOLD + "Random")
                .description(getRandomDescription())
                .displayItem(new ItemStack(Material.CONCRETE, 1, (short) 2))
                .onClick(event -> {
                    ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
                    Parkour.getInstance().dequeuePlayer(sjp);
                    Parkour.getInstance().queuePlayerRandom(sjp);
                    event.getItem().getParent().update();
                });
        return item;
    }
    
    public static InventoryMenuTemplateBuilder createShuffleMenu(InventoryMenuTemplateBuilder parent) {
        InventoryMenuTemplateBuilder menu = new InventoryMenuTemplateBuilder()
                .title("SuperJump: Shuffle")
                .displayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Shuffle")
                .description(getShuffleDescription())
                .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short) 24))
                .flags(InventoryMenuFlag.EXIT_ON_CLICK_OUTSIDE);
        addGamemodeMenu(menu, ParkourMode.SHUFFLE);
        addBackButton(menu, parent.build());
        return menu;
    }
    
    public static InventoryMenuTemplateBuilder createSoloMenu(InventoryMenuTemplateBuilder parent) {
        InventoryMenuTemplateBuilder menu = new InventoryMenuTemplateBuilder()
                .title("SuperJump: Solo")
                .displayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Solo")
                .description(getSoloDescription())
                .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short) 5))
                .flags(InventoryMenuFlag.EXIT_ON_CLICK_OUTSIDE);
        menu.component(5, createConquestMenu(menu));
        menu.component(4, createMemoryItem());
        menu.component(3, createEndlessItem());
        addBackButton(menu, parent.build());
        return menu;
    }
    
    public static InventoryMenuTemplateBuilder createConquestMenu(InventoryMenuTemplateBuilder parent) {
        InventoryMenuTemplateBuilder menu = new InventoryMenuTemplateBuilder()
                .title("SuperJump: Conquest")
                .displayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Conquest")
                .description(getConquestDescription())
                .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short) 21))
                .flags(InventoryMenuFlag.EXIT_ON_CLICK_OUTSIDE);
        addGamemodeMenu(menu, ParkourMode.CONQUEST);
        addBackButton(menu, parent.build());
        return menu;
    }
    
    public static InventoryMenuTemplateBuilder createShopMenu(InventoryMenuTemplateBuilder parent) {
        InventoryMenuTemplateBuilder menu = new InventoryMenuTemplateBuilder()
                .title("Paragon Shop")
                .displayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Paragon Shop")
                .description(getShopDescription())
                .displayIcon(Material.APPLE)
                .flags(InventoryMenuFlag.EXIT_ON_CLICK_OUTSIDE);
        ShopItem.getItemList().forEach(item -> {
            menu.component(item.getMenuLoc(), item.getItem());
        });
        addBackButton(menu, parent.build());
        return menu;
    }
    
    public static InventoryMenuItemTemplateBuilder createProfileItem() {
        InventoryMenuItemTemplateBuilder item = item()
                .displayName(ChatColor.AQUA + "" + ChatColor.BOLD + "SuperJump Profile")
                .description(getProfileDescription())
                .displayItem(slp -> {
                    ItemStack playerHead = new ItemStack(Material.SKULL_ITEM, 1, (short)3);
                    SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
                    skullMeta.setOwner(slp.getName());
                    playerHead.setItemMeta(skullMeta);
                    return playerHead;
                });
        return item;
    }
    
    public static InventoryMenuItemTemplateBuilder createMemoryItem() {
        MemoryParkourArena arena = MemoryParkourArena.byName("Memory");
        InventoryMenuItemTemplateBuilder item = item()
                    .displayName(ChatColor.GOLD + "" + ChatColor.BOLD + arena.getName())
                    .description(getEndlessDescription())
                    .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short) 20))
                    .onClick((event) -> {
                        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
                        if (MemoryParkourArena.isDisabled()) {
                            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(event.getPlayer())
                                    , Parkour.getInstance().getChatPrefix()
                                    + Theme.ERROR.buildTheme(false) + " Memory is currently disabled for scheduled maintenance (11:55pm - 12:00am PST)");
                        }
                        else {
                            Parkour.getInstance().queuePlayer(sjp, arena, true);
                            event.getItem().getParent().update();
                        }
                    });
        return item;
    }
    
    public static InventoryMenuItemTemplateBuilder createEndlessItem() {
        EndlessParkourArena arena = EndlessParkourArena.byName("Endless");
        InventoryMenuItemTemplateBuilder item = item()
                    .displayName(ChatColor.GOLD + "" + ChatColor.BOLD + arena.getName())
                    .description(getEndlessDescription())
                    .displayItem(new ItemStack(Material.DIAMOND_AXE, 1, (short) 20))
                    .onClick((event) -> {
                        ParkourPlayer sjp = Parkour.getInstance().getPlayerManager().get(event.getPlayer());
                        if (EndlessParkourArena.isDisabled()) {
                            ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(event.getPlayer())
                                    , Parkour.getInstance().getChatPrefix()
                                    + Theme.ERROR.buildTheme(false) + " Endless is currently disabled for scheduled maintenance (11:55pm - 12:00am PST)");
                        }
                        else {
                            Parkour.getInstance().queuePlayer(sjp, arena, true);
                            event.getItem().getParent().update();
                        }
                    });
        return item;
    }
    
    private static class MenuChallenge {
        
        private SLPlayer target;
        private final SLPlayer source;
        private Arena arena;

        public MenuChallenge(Player source) {
            this.source = SpleefLeague.getInstance().getPlayerManager().get(source);
        }

        public SLPlayer getSource() {
            return source;
        }
        
        public SLPlayer getTarget() {
            return target;
        }

        public void setTarget(Player player) {
            this.target = SpleefLeague.getInstance().getPlayerManager().get(player);
        }

        public Arena getArena() {
            return arena;
        }

        public void setArena(Arena arena) {
            this.arena = arena;
        }
    }
    
}

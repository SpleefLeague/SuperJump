package com.spleefleague.superjump.game;

import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.chat.Theme;
import com.spleefleague.core.events.BattleStartEvent;
import com.spleefleague.core.listeners.FakeBlockHandler;
import com.spleefleague.core.player.GeneralPlayer;
import com.spleefleague.core.player.PlayerState;
import com.spleefleague.core.player.SLPlayer;
import com.spleefleague.core.plugin.GamePlugin;
import com.spleefleague.core.utils.fakeblock.FakeArea;
import com.spleefleague.core.utils.fakeblock.FakeBlock;
import com.spleefleague.core.utils.RuntimeCompiler;
import com.spleefleague.superjump.SuperJump;
import com.spleefleague.superjump.game.signs.GameSign;
import com.spleefleague.superjump.player.SJPlayer;
import java.util.List;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

/**
 *
 * @author Jonas
 */
public class Battle extends AbstractBattle {

    protected Battle(Arena arena, List<SJPlayer> players) {
        super(arena, players);
    }
    
    @Override
    public void start(BattleStartEvent.StartReason reason) {
        players.forEach(p -> {
            GamePlugin.unspectateGlobal(p);
            GamePlugin.dequeueGlobal(p);
        });
        BattleStartEvent event = new BattleStartEvent(this, reason);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            if (arena.getStartDebugger() != null) {
                RuntimeCompiler.debugFromHastebin(arena.getStartDebugger(), null);
            }
            arena.registerGameStart();
            GameSign.updateGameSigns(arena);
            ChatManager.registerChannel(cc);
            SuperJump.getInstance().getBattleManager().add(this);
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective("rounds", "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName(ChatColor.GRAY + "0:0:0 | " + ChatColor.RED + "Times Fallen:");
            String playerNames = "";
            for (int i = 0; i < players.size(); i++) {
                SJPlayer sjp = players.get(i);
                if (i == 0) {
                    playerNames = sjp.getName();
                } else if (i == players.size() - 1) {
                    playerNames += " and " + sjp.getName();
                } else {
                    playerNames += ", " + sjp.getName();
                }
                SLPlayer slp = SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer());
                slp.addChatChannel(cc);
                slp.setState(PlayerState.INGAME);
                Player p = sjp.getPlayer();
                p.setHealth(p.getMaxHealth());
                p.setFoodLevel(20);
                sjp.setIngame(true);
                sjp.setFrozen(true);
                PlayerData pdata = new PlayerData(sjp, arena.getSpawns()[i], arena.getGoals()[i % arena.getGoals().length]);
                this.data.put(sjp, pdata);
                p.setGameMode(GameMode.ADVENTURE);
                p.setFlying(false);
                p.setAllowFlight(false);
                p.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(p::removePotionEffect);
                players.stream().filter(sjpt -> sjp != sjpt).forEach(sjpt -> sjp.showPlayer(sjpt.getPlayer()));
                p.eject();
                p.teleport(arena.getSpawns()[i]);
                p.closeInventory();
                p.getInventory().clear();
                p.setScoreboard(scoreboard);
                scoreboard.getObjective("rounds").getScore(sjp.getName()).setScore(pdata.getFalls());
            }
            hidePlayers();
            getSpawnCageBlocks();
            FakeBlockHandler.addArea(fakeBlocks, false, GeneralPlayer.toBukkitPlayer(players.toArray(new SJPlayer[players.size()])));
            ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), Theme.SUCCESS.buildTheme(false) + "Beginning match on " + ChatColor.WHITE + arena.getName() + ChatColor.GREEN + " between " + ChatColor.RED + playerNames + "!", SuperJump.getInstance().getStartMessageChannel());
            startCountdown();
        }
    }
    
    @Override
    protected void getSpawnCageBlocks() {
        for (Location spawn : arena.getSpawns()) {
            fakeBlocks.add(getCageBlocks(spawn, Material.AIR));
        }
    }
    
    protected FakeArea getCageBlocks(Location loc, Material m) {
        loc = loc.getBlock().getLocation();
        FakeArea area = new FakeArea();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    area.addBlock(new FakeBlock(loc.clone().add(x, 2, z), m));
                } else {
                    for (int y = 0; y <= 2; y++) {
                        area.addBlock(new FakeBlock(loc.clone().add(x, y, z), m));
                    }
                }
            }
        }
        return area;
    }
    
    @Override
    protected void applyRatingChange(SJPlayer winner) {
        int winnerPoints = 0;
        final int MIN_RATING = 1, MAX_RATING = 40;
        String playerList = "";
        for (SJPlayer sjp : players) {
            if (sjp != winner) {
                float elo = (float) (1f / (1f + Math.pow(2f, ((sjp.getRating() - winner.getRating()) / 250f))));
                int rating = (int) Math.round(MAX_RATING * (1f - elo));
                if (rating < MIN_RATING) {
                    rating = MIN_RATING;
                }
                winnerPoints += rating;
                sjp.setRating(sjp.getRating() - rating);
                SpleefLeague.getInstance().getPlayerManager().get(sjp).changeCoins(1);
                playerList += ChatColor.RED + sjp.getName() + ChatColor.WHITE + " (" + sjp.getRating() + ")" + ChatColor.GREEN + " loses " + ChatColor.GRAY + (rating) + ChatColor.WHITE + (-rating == 1 ? " point." : " points.");
            }
        }
        winner.setRating(winner.getRating() + winnerPoints);
        SpleefLeague.getInstance().getPlayerManager().get(winner).changeCoins(2);
        playerList += ChatColor.RED + winner.getName() + ChatColor.WHITE + " (" + winner.getRating() + ")" + ChatColor.GREEN + " gets " + ChatColor.GRAY + winnerPoints + ChatColor.WHITE + (winnerPoints == 1 ? " point." : " points. ");
        ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(), ChatColor.GREEN + "Game in arena " + ChatColor.WHITE + arena.getName() + ChatColor.GREEN + " is over " + ChatColor.WHITE + "(" + DurationFormatUtils.formatDuration(ticksPassed * 50, "HH:mm:ss", true) + ")" + ChatColor.GREEN + ". " + playerList, SuperJump.getInstance().getEndMessageChannel());
        this.players.forEach((p) -> {
            SuperJump.getInstance().getPlayerManager().save(p);
        });
    }
    
}

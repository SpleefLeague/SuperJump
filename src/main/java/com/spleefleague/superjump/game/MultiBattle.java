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
import com.spleefleague.core.utils.UtilChat;
import com.spleefleague.superjump.SuperJump;
import com.spleefleague.superjump.game.signs.GameSign;
import com.spleefleague.superjump.player.SJPlayer;
import java.util.List;
import java.util.stream.Collectors;
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
 * @author RINES <iam@kostya.sexy>
 */
public class MultiBattle extends AbstractBattle {

    public MultiBattle(Arena arena, List<SJPlayer> players) {
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
            Location spawn = arena.getSpawns()[0];
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
                PlayerData pdata = new PlayerData(sjp, spawn, arena.getGoals()[0]);
                this.data.put(sjp, pdata);
                p.setGameMode(GameMode.ADVENTURE);
                p.setFlying(false);
                p.setAllowFlight(false);
                p.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(p::removePotionEffect);
                players.stream().filter(sjpt -> sjp != sjpt).forEach(sjpt -> sjp.showPlayer(sjpt.getPlayer()));
                p.eject();
                p.teleport(spawn);
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
//        fakeBlocks.add(getCageBlocks(arena.getSpawns()[0], Material.AIR));
    }
    
    @Override
    protected void applyRatingChange(SJPlayer winner) {
        String players = this.players.stream().filter(sjp -> sjp != winner).map(SJPlayer::getName).collect(Collectors.joining("&a, &c"));
        ChatManager.sendMessage(SuperJump.getInstance().getChatPrefix(),
                UtilChat.c("&aGame in arena &f%s &ais over &f(%s)&a. &c%s &awins over &c%s&a.",
                        arena.getName(),
                        DurationFormatUtils.formatDuration(ticksPassed * 50, "HH:mm:ss", true),
                        winner.getName(),
                        players),
                SuperJump.getInstance().getEndMessageChannel());
        SpleefLeague.getInstance().getPlayerManager().get(winner).changeCoins(2);
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.game.memory;

import com.mongodb.client.MongoCursor;
import com.spleefleague.core.SpleefLeague;
import com.spleefleague.core.chat.ChatManager;
import com.spleefleague.core.chat.Theme;
import com.spleefleague.core.utils.Area;
import com.spleefleague.entitybuilder.DBLoad;
import com.spleefleague.entitybuilder.DBSave;
import com.spleefleague.entitybuilder.EntityBuilder;
import com.spleefleague.gameapi.events.BattleEndEvent;
import com.spleefleague.gameapi.events.BattleStartEvent;
import com.spleefleague.parkour.Parkour;
import com.spleefleague.parkour.game.Arena;
import com.spleefleague.parkour.game.ParkourMode;
import com.spleefleague.parkour.game.ParkourRandomJumps;
import com.spleefleague.parkour.game.ParkourRandomJumps.Jump;
import com.spleefleague.parkour.player.ParkourPlayer;
import com.spleefleague.parkour.stats.Highscore;
import com.spleefleague.parkour.variable.SimpleDate;
import static java.lang.Math.floor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 *
 * @author NickM13
 */
public class MemoryParkourArena extends Arena<MemoryParkourBattle> {

    @DBLoad(fieldName = "jumpcount")
    private int jumpCount;
    protected static class HSAT {
        public SimpleDate date;
        public String player;
        public int score;
        
        public HSAT(SimpleDate date, String player, int score) {
            this.date = date;
            this.player = player;
            this.score = score;
        }
    };
    static private HSAT highscoreAlltime;
    static private int highscoreDate = -1;
    static private Map<Integer, Highscore> highscoreMap = new HashMap<>();
    
    static private BukkitTask disableTask;
    static private boolean disabled = false; // Disables 5 minutes before reset, then re-enables after reset
    
    static {
        disableTask = new BukkitRunnable() {
            public void run() {
                long min = SimpleDate.getNextDayMillis() / 1000L / 60L;
                long sec = (SimpleDate.getNextDayMillis() / 1000L) % 60;
                if(min < 5) {
                    if(!disabled) {
                        ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(), Theme.SUCCESS.buildTheme(false)
                                + Parkour.arenaColor + "Endless"
                                + Parkour.fillColor + " has been disabled for scheduled maintenance "
                                + Parkour.timeColor + "(11:55pm - 12:00am PST)"
                                + Parkour.fillColor + "."
                                , Parkour.getInstance().getStartMessageChannel());
                        Parkour.getInstance().getPlayerManager().getAll().forEach(sjp -> {
                            Parkour.getInstance().getBattleManager(ParkourMode.ENDLESS).getGameQueue().dequeuePlayer(sjp);
                        });
                        Parkour.getInstance().getBattleManager(ParkourMode.ENDLESS).getAll().forEach(b -> {
                            MemoryParkourBattle battle = (MemoryParkourBattle)b;
                            if(battle != null) {
                                battle.getActivePlayers().forEach(sjp -> {
                                    ChatManager.sendMessagePlayer(SpleefLeague.getInstance().getPlayerManager().get(sjp.getPlayer())
                                            , Parkour.getInstance().getChatPrefix()
                                            + Parkour.fillColor + " Your battle was ended for scheduled maintenance.");
                                });
                                battle.end(null, BattleEndEvent.EndReason.ENDGAME);
                            }
                        });
                        disabled = true;
                    }
                }
                else if(disabled) {
                    ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(), Theme.SUCCESS.buildTheme(false)
                            + Parkour.arenaColor + "Endless"
                            + Parkour.fillColor + " is now enabled, thank you for your patience."
                            , Parkour.getInstance().getStartMessageChannel());
                    disabled = false;
                }
                // Warning messages preceeding disable
                if(sec == 0) {
                    if(min == 15) {
                        ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(), Theme.SUCCESS.buildTheme(false)
                                + Parkour.arenaColor + "Endless"
                                + Parkour.fillColor + " will be disabled in "
                                + Parkour.timeColor + "10"
                                + Parkour.fillColor + " minutes for scheduled maintenance."
                                , Parkour.getInstance().getStartMessageChannel());
                    }
                    else if(min == 10) {
                        ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(), Theme.SUCCESS.buildTheme(false)
                                + Parkour.arenaColor + "Endless"
                                + Parkour.fillColor + " will be disabled in "
                                + Parkour.timeColor + "5"
                                + Parkour.fillColor + " minutes for scheduled maintenance."
                                , Parkour.getInstance().getStartMessageChannel());
                    }
                    else if(min == 6) {
                        ChatManager.sendMessage(Parkour.getInstance().getChatPrefix(), Theme.SUCCESS.buildTheme(false)
                                + Parkour.arenaColor + "Endless"
                                + Parkour.fillColor + " will be disabled in "
                                + Parkour.timeColor + "1"
                                + Parkour.fillColor + " minute for scheduled maintenance."
                                , Parkour.getInstance().getStartMessageChannel());
                    }
                }
            }
        }.runTaskTimer(Parkour.getInstance(), 0L, 20L);
    }
    
    public static boolean isDisabled() {
        return disabled;
    }
    
    public static void initPlayer(ParkourPlayer sjp) {
        highscoreMap.get(highscoreDate).setPlayerScore(sjp.getName(), sjp.getEndlessLevel());
    }
    
    public static int getLeaderboardPlacement(ParkourPlayer player) {
        validateHighscore();
        return highscoreMap.get(highscoreDate).getPlayerPlacement(player.getName());
    }
    
    private static final Map<String, MemoryParkourArena> ARENAS = new HashMap<>();
    
    public static void initHighscores() {
        highscoreMap.clear();
        highscoreAlltime = new HSAT(null, "", 0);
        MongoCursor<Document> dbc = Parkour.getInstance().getPluginDB()
                .getCollection("EndlessScores")
                .find()
                .iterator();
        Document docScore;
        Highscore highscore;
        while (dbc.hasNext()) {
            docScore = dbc.next();
            highscore = EntityBuilder.load(docScore, Highscore.class);
            highscoreMap.put(highscore.date.asInt(), highscore);
        }
        highscoreMap.forEach((d, hs) -> {
            Highscore.LPlayer top = hs.getTopPlayer();
            if(top.score > highscoreAlltime.score) {
                highscoreAlltime = new HSAT(hs.date, top.player, top.score);
            }
        });
    }
    
    public static Collection<MemoryParkourArena> getAll() {
        return ARENAS.values();
    }
    
    public static MemoryParkourArena byName(String arena) {
        return ARENAS.get(arena.toLowerCase());
    }
    
    public static boolean validateHighscore() {
        SimpleDate today = new SimpleDate();
        boolean exists = highscoreMap.containsKey(today.asInt());
        if(!exists)
            highscoreMap.put(today.asInt(), new Highscore());
        if(!exists || highscoreDate == -1 || highscoreDate == today.asInt()) {
            highscoreDate = today.asInt();
            return false;
        }
        return true;
    }
    
    // Checks if score is a high score
    public void checkHighscore(ParkourPlayer sjp) {
        validateHighscore();
        highscoreMap.get(highscoreDate).setPlayerScore(sjp.getName(), sjp.getEndlessLevel());
        Highscore.LPlayer top = highscoreMap.get(highscoreDate).getTopPlayer();
        if(top.score > highscoreAlltime.score) {
            highscoreAlltime = new HSAT(highscoreMap.get(highscoreDate).date, top.player, top.score);
        }
        try {
            EntityBuilder.save(highscoreMap.get(highscoreDate), Parkour.getInstance().getPluginDB().getCollection("EndlessScores"));
        } catch(Exception e) {
            Parkour.LOG.log(Level.WARNING, "Could not save ParkourPlayer!");
            Document doc = EntityBuilder.serialize(highscoreMap.get(highscoreDate)).get("$set", Document.class);
            Parkour.LOG.log(Level.WARNING, doc.toJson());
            e.printStackTrace();
        }
    }
    
    public Highscore getHighscoreToday() {
        validateHighscore();
        return highscoreMap.get(highscoreDate);
    }
    
    public HSAT getHighscoreAlltime() {
        return highscoreAlltime;
    }
    
    @DBLoad(fieldName = "highscores")
    private void loadHighscores(List<Document> endlessStats) {
        Document docDate;
        List<Document> docScores;
        int year, month, day;
        SimpleDate date;
        String player = "";
        int score = 0;
        for(Document doc : endlessStats) {
            try {
                docDate = doc.get("date", Document.class);
                year = docDate.get("year", Integer.class);
                month = docDate.get("month", Integer.class);
                day = docDate.get("day", Integer.class);
                date = new SimpleDate(year, month, day);
                highscoreMap.put(date.asInt(), new Highscore(date));
                docScores = doc.get("scores", List.class);
                for(Document sDoc : docScores) {
                    player = sDoc.get("player", String.class);
                    score = sDoc.get("score", Integer.class);
                    highscoreMap.get(date.asInt()).setPlayerScore(player, score);
                }
                if(highscoreMap.get(date.asInt()).getTopPlayer().score > highscoreAlltime.score) {
                    highscoreAlltime = new HSAT(date, player, score);
                }
            } catch(Exception e) {
                System.out.println("Failed to load endless data:\n" + doc);
                e.printStackTrace();
            }
        }
        for(Map.Entry<Integer, Highscore> hs : highscoreMap.entrySet()) {
            if(hs.getValue().getTopPlayer().score > highscoreAlltime.score) {
                highscoreAlltime = new HSAT(hs.getValue().date, hs.getValue().getTopPlayer().player, hs.getValue().getTopPlayer().score);
            }
        }
    }
    
    @DBSave(fieldName = "highscores")
    private List<Document> saveHighscores() {
        List<Document> documents = new ArrayList<>();
        Document docDate, docScores;
        for(Map.Entry<Integer, Highscore> highscore : highscoreMap.entrySet()) {
            docDate = new Document("year", highscore.getValue().date.year).append("month", highscore.getValue().date.month).append("day", highscore.getValue().date.day);
            docScores = highscore.getValue().saveScores();
            documents.add(new Document("date", docDate).append("scores", docScores));
        }
        return documents;
    }
    
    public int getJumpCount() {
        return jumpCount;
    }
    
    
    public Location getSpawn() {
        return spawns[0];
    }

    public Collection<BlockPrototype> getFakeBlocks() {
        return null;
    }
    
    @Override
    public boolean isOccupied() {
        return false;
    }
    
    @Override
    public int getSize() {
        return 1;
    }
    
    @Override
    public int getRequiredPlayers() {
        return 1;
    }
    
    public static void loadArena(Document document) {
        MemoryParkourArena arena = EntityBuilder.load(document, MemoryParkourArena.class);
        if(ARENAS.containsKey(arena.getName().toLowerCase())) {
            Arena.recursiveCopy(arena, byName(arena.getName()), Arena.class);
        }
        else {
            Parkour.getInstance().getBattleManager(ParkourMode.ENDLESS).registerArena(arena);
            ARENAS.put(arena.getName().toLowerCase(), arena);
        }
    }
    
    @Override
    public MemoryParkourBattle startBattle(List<ParkourPlayer> players, BattleStartEvent.StartReason reason) {
        if (!isOccupied()) {
            players.get(0).validateEndless();
            ArenaData data = generate(this.spawns[0].clone(), this.jumpCount, players.get(0).getEndlessLevel());
            Location[] spawn1 = new Location[1];
            Area[] goal = new Area[1];
            spawn1[0] = data.spawn1;
            goal[0] = data.goal;
            Area[] borders = data.borders;
            int jumpCount = this.jumpCount;
            Collection<BlockPrototype> fakeBlocks = data.fakeBlocks;
            //Create new arena object since each random arena has different spawns/goals/borders
            MemoryParkourArena arena = new MemoryParkourArena() {
                @Override
                public int getJumpCount() {
                    return jumpCount;
                }
                
                @Override
                public Location[] getSpawns() {
                    return spawn1;
                }

                @Override
                public Area[] getGoals() {
                    return goal;
                }

                @Override
                public Area getArea() {
                    return borders[0];
                }

                @Override
                public Area[] getBorders() {
                    return borders;
                }
                
                @Override
                public Collection<BlockPrototype> getFakeBlocks() {
                    return fakeBlocks;
                }
                
                @Override
                public void registerGameEnd() {
                    super.registerGameEnd();
                }

                @Override
                public String getName() {
                    return MemoryParkourArena.this.getName();
                }
                
                @Override
                public boolean isRated() {
                    return MemoryParkourArena.this.isRated();
                }
                
                @Override
                public ParkourMode getParkourMode() {
                    return MemoryParkourArena.this.getParkourMode();
                }
            };
            MemoryParkourBattle battle = new MemoryParkourBattle(arena, players);
            for(BlockPrototype bp : arena.getFakeBlocks()) {
                battle.getFakeWorld().getBlockAt(bp.location).setType(bp.type);
                battle.getFakeWorld().getBlockAt(bp.location).setData(bp.data);
            }
            battle.start(reason);
        }
        return null;
    }
    
    public MemoryParkourArena nextLevel(MemoryParkourArena arena, MemoryParkourBattle battle) {
        battle.getFakeWorld().getUsedBlocks().forEach(fb -> {
            battle.getFakeWorld().getBlockAt(fb.getLocation()).setType(Material.AIR);
            //battle.getFakeWorld().getHandle().getBlockAt(fb.getLocation()).setType(Material.AIR);
        });
        /*
        for(BlockPrototype bp : arena.getFakeBlocks()) {
            battle.getFakeWorld().getBlockAt(bp.location).setType(Material.AIR);
        }
        */
        battle.getFakeWorld().getUsedBlocks().clear();
        ArenaData data = generate(arena.getSpawns()[0].clone(), arena.getJumpCount(), battle.getLevel());
        Location[] spawn1 = new Location[1];
        Area[] goal = new Area[1];
        spawn1[0] = data.spawn1;
        goal[0] = data.goal;
        Area[] borders = data.borders;
        int jumpCount = arena.getJumpCount();
        Collection<BlockPrototype> fakeBlocks = data.fakeBlocks;
        //Create new arena object since each random arena has different spawns/goals/borders
        arena = new MemoryParkourArena() {
            @Override
            public int getJumpCount() {
                return jumpCount;
            }
            
            @Override
            public Location[] getSpawns() {
                return spawn1;
            }

            @Override
            public Area[] getGoals() {
                return goal;
            }

            @Override
            public Area getArea() {
                return borders[0];
            }

            @Override
            public Area[] getBorders() {
                return borders;
            }

            @Override
            public Collection<BlockPrototype> getFakeBlocks() {
                return fakeBlocks;
            }

            @Override
            public void registerGameEnd() {
                super.registerGameEnd();
            }

            @Override
            public String getName() {
                return MemoryParkourArena.this.getName();
            }

            @Override
            public boolean isRated() {
                return MemoryParkourArena.this.isRated();
            }

            @Override
            public ParkourMode getParkourMode() {
                return MemoryParkourArena.this.getParkourMode();
            }
        };
        for(BlockPrototype bp : arena.getFakeBlocks()) {
            battle.getFakeWorld().getBlockAt(bp.location).setType(bp.type);
            battle.getFakeWorld().getBlockAt(bp.location).setData(bp.data);
        }
        return arena;
    }

    private static Random random;
    private static int frequencySumBB;
    private static ArrayList<Jump> possibleJumpsBB;
    
    private static ArenaData generate(Location spawn1, int jumpCount, int level) {
        random = new Random(new SimpleDate().asInt());
        random = new Random(level + random.nextInt());
        float difficulty;
        if(level <= 200) {
            difficulty = 0.f + (float)floor(level / 10.f) * 0.15f;
        } else {
            difficulty = Math.min(3.f + (float)floor((level - 200) / 10.f) * 0.2f, 4.f);
        }
        possibleJumpsBB = ParkourRandomJumps.getJumpsByDifficultyBB(difficulty);
        frequencySumBB = 0;
        for(Jump j : possibleJumpsBB) {
            frequencySumBB += j.getFrequency(difficulty);
        }
        
        Collection<BlockPrototype> fakeBlocks = new ArrayList<>();
        Location goal = null, lastLoc = spawn1.clone().add(0, -1, 0);
        Location locSmallest = spawn1.clone(), locHighest = spawn1.clone();
        for (int i = 0; i < 9; i++) {
            fakeBlocks.add(new BlockPrototype(lastLoc.clone().add(new Vector(-1 + (i % 3), 0, -1 + Math.floorDiv(i, 3))), Material.QUARTZ_BLOCK, (byte)0));
        }
        fakeBlocks.add(new BlockPrototype(lastLoc, Material.REDSTONE_LAMP_ON, (byte)0));
        
        Jump next;
        char last = 'b';
        for (int j = 0; j < jumpCount; j++) {
            switch(last) {
                case 'b':
                default:
                    next = getNextJump(possibleJumpsBB, frequencySumBB, difficulty);
                    break;
            }
            lastLoc = next.apply(lastLoc, false);
            locSmallest = getMin(locSmallest, lastLoc);
            locHighest = getMax(locHighest, lastLoc);
            if (j < jumpCount - 1) {
                switch(next.getTo()) {
                    case 'b':
                    default:
                        fakeBlocks.add(new BlockPrototype(lastLoc.clone(), Material.QUARTZ_BLOCK, (byte)0));
                        break;
                }
            } else {
                goal = lastLoc;
                fakeBlocks.add(new BlockPrototype(lastLoc.clone(), Material.REDSTONE_LAMP_ON, (byte)0));
            }
            last = next.getTo();
        }
        
        Area goalArea = new Area(goal.clone().add(-0.5, 1, -0.5), goal.clone().add(0.5, 3, 0.5));
        Area border = new Area(locSmallest.add(-3, -3, -3), locHighest.add(3, 3, 3));
        return new ArenaData(spawn1.clone(), goalArea, fakeBlocks, new Area[]{border});
    }

    private static Location getMin(Location a, Location b) {
        return new Location(a.getWorld(), Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    private static Location getMax(Location a, Location b) {
        return new Location(a.getWorld(), Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    private static Jump getNextJump(final ArrayList<Jump> possibleJumps, final int frequencySum, float difficulty) {
        int id = random.nextInt(frequencySum);
        for (Jump jump : possibleJumps) {
            id -= jump.getFrequency(difficulty);
            if (id < 0) {
                return jump;
            }
        }
        return null;
    }

    private static class ArenaData {

        private final Collection<BlockPrototype> fakeBlocks;
        private final Location spawn1;
        private final Area goal, borders[];

        public ArenaData(Location spawn1, Area goal, Collection<BlockPrototype> fakeBlocks, Area[] borders) {
            this.spawn1 = spawn1;
            this.goal = goal;
            this.borders = borders;
            this.fakeBlocks = fakeBlocks;
        }
    }

    private static class BlockPrototype {    
        private Material type;
        private byte data;
        private final Vector location;

        public BlockPrototype(Location location, Material type, byte data) {
            this(location.toVector(), type, data);
        }
        
        public BlockPrototype(Vector location, Material type, byte data) {
            this.type = type;
            this.data = data;
            this.location = location;
        }        
    }
}

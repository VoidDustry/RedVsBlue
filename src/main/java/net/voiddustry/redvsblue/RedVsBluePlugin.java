package net.voiddustry.redvsblue;

import arc.Events;

import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.ai.Pathfinder;
import mindustry.content.Blocks;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.mod.Plugin;
import mindustry.type.UnitType;
import mindustry.ui.Menus;
import mindustry.world.Tile;
import net.voiddustry.redvsblue.admin.Admin;
import net.voiddustry.redvsblue.ai.AirAI;
import net.voiddustry.redvsblue.ai.BluePlayerTarget;
import net.voiddustry.redvsblue.ai.StalkerGroundAI;
import net.voiddustry.redvsblue.ai.StalkerSuicideAI;
import net.voiddustry.redvsblue.evolution.Evolution;
import net.voiddustry.redvsblue.evolution.Evolutions;
import net.voiddustry.redvsblue.game.crux.*;
import net.voiddustry.redvsblue.game.stations.StationsMenu;
import net.voiddustry.redvsblue.game.building.BuildBlock;
import net.voiddustry.redvsblue.game.building.BuildMenu;
import net.voiddustry.redvsblue.game.starting_menu.StartingMenu;
import net.voiddustry.redvsblue.game.stations.*;
import net.voiddustry.redvsblue.player.Hud;
import net.voiddustry.redvsblue.player.Premium;
import net.voiddustry.redvsblue.util.MapVote;
import net.voiddustry.redvsblue.util.UnitsConfig;
import net.voiddustry.redvsblue.util.Utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import static net.voiddustry.redvsblue.util.MapVote.callMapVoting;
import static net.voiddustry.redvsblue.util.Utils.*;

public class RedVsBluePlugin extends Plugin {
    public static final int bluePlayerTargeting;

    static {
        Pathfinder.fieldTypes.add(BluePlayerTarget::new);
        bluePlayerTargeting = Pathfinder.fieldTypes.size - 1;
    }

    public static final HashMap<String, PlayerData> players = new HashMap<>();

    public float blueSpawnX;
    public float blueSpawnY;

    public static Seq<Tile> redSpawns = new Seq<>();

    public static int stage = 0;
    public static float stageTimer = 0;
    public static boolean playing = false;



    static Timer.Task task = new Timer.Task() {
        @Override
        public void run() {
            stage++;
            stageTimer = 300;
            spawnBoss();
            announceBundled("game.new-stage", 15, stage);
            ClassChooseMenu.updateUnitsMap();
            if (stage >= 11) {
                gameOver(Team.blue);
            }
        }
    };

    @Override
    public void init() {

        Log.info("&gRedVsBlue Plugin &rStarted!");

        Utils.initStats();
        Utils.initRules();
        Utils.initTimers();
        Utils.loadContent();
        SpawnEffect.initEffect();
        Boss.forEachBoss();
        Premium.init();

        for (UnitType unit : Vars.content.units()) {
            if (unit == UnitTypes.crawler) {
                unit.aiController = StalkerSuicideAI::new;
            }
            if (!unit.flying) {
                unit.aiController = StalkerGroundAI::new;
            }
            if (unit.flying) {
                unit.aiController = AirAI::new;
            }
            if (unit.naval) {
                unit.flying = true;
            }
        }


        Events.on(EventType.PlayerJoin.class, event -> {
            Player player = event.player;
            if (playing) {
                if (players.containsKey(player.uuid())) {
                    PlayerData data = players.get(player.uuid());
                    player.team(data.getTeam());
                    if (player.team() == Team.blue) {
                        player.unit(data.getUnit());
                    }

                } else {
                    players.put(player.uuid(), new PlayerData(player));
                    PlayerData data = players.get(player.uuid());
                    Unit unit = getStartingUnit().spawn(Team.blue, blueSpawnX, blueSpawnY);

                    data.setUnit(unit);
                    player.unit(unit);
                }
                if (playing && player.team() == Team.crux) {
                    CruxUnit.callSpawn(player);
                }
            }

            int menu = Menus.registerMenu(((player1, option) -> {}));

            Call.menu(player.con, menu, Bundle.get("welcome", player.locale), Bundle.get("welcome.text", player.locale), new String[][]{{Bundle.get("stations.buttons.close", player.locale)}});
        });

        Events.on(EventType.PlayerChatEvent.class, event -> {
            Call.sound(Sounds.chatMessage, 2, 2, 1);
            if (Utils.voting) {
                if (Strings.canParseInt(event.message)) {
                    MapVote.registerVote(event.player, Strings.parseInt(event.message));
                }
            }
        });

        Events.on(EventType.UnitBulletDestroyEvent.class, event -> {
            if (event.unit != null && event.bullet.owner() instanceof Unit killer) {
                if (killer.isPlayer() && killer.team == Team.blue) {
                    PlayerData data = players.get(killer.getPlayer().uuid());
                    players.get(killer.getPlayer().uuid()).addScore(data.getLevel());
                    Call.label(killer.getPlayer().con, "[lime]+" + data.getLevel(), 2, event.unit.x, event.unit.y);
                    data.addExp(1);
                    processLevel(killer.getPlayer(), data);
                } else if (killer.isPlayer() && killer.team == Team.crux) {
                    PlayerData data = players.get(killer.getPlayer().uuid());
                    data.addKill();
                    Call.label(killer.getPlayer().con, "[scarlet]+1", 2, event.unit.x, event.unit.y);
                    if (data.getKills() >= 2) {
                        Boss.spawnBoss(killer.getPlayer());
                    }

                }
            }
        });

        Events.on(EventType.BlockDestroyEvent.class, event -> {
        });


        Events.on(EventType.UnitDestroyEvent.class, event -> {
            if (event.unit.isPlayer()) {
                if (event.unit.team() == Team.blue) {
                    event.unit.getPlayer().team(Team.crux);
                    PlayerData data = players.get(event.unit.getPlayer().uuid());
                    data.setTeam(Team.crux);
                    data.setScore(0);
                    data.setKills(0);

                    UnitTypes.renale.spawn(Team.malis, event.unit.x, event.unit.y).kill();

                    ClassChooseMenu.selectedUnit.put(event.unit.getPlayer().uuid(), UnitTypes.dagger);
                    CruxUnit.callSpawn(event.unit.getPlayer());
                } else if (event.unit.getPlayer().team() == Team.crux) {
                  CruxUnit.callSpawn(event.unit.getPlayer());
                  Player player = event.unit.getPlayer();
                  Boss.bosses.forEach(boss -> {
                      if (player == boss.player) {
                          Boss.bosses.remove(boss);
                      }
                  });
                }
            }
            gameOverCheck();
        });



        Events.on(EventType.WorldLoadEvent.class, event -> {
            Miner.clearMiners();
            RepairPoint.clearPoints();
            Laboratory.clearLabs();
            Booster.clearBoosters();
            ArmorWorkbench.clearWorkbenches();
            Recycler.clearRecyclers();
            SuppressorTower.clearTowers();
            BuildBlock.clear();
            Boss.bosses.clear();

            initRules();

            StartingMenu.canOpenMenu = true;

            Groups.player.each(p -> {
                PlayerData data = players.get(p.uuid());
                data.setUnit(null);
                data.setExp(0);
                data.setLevel(1);
            });
        });

        Events.on(EventType.WorldLoadEvent.class, event -> Timer.schedule(() -> {

            players.clear();

            Vars.state.rules.canGameOver = false;
            Vars.state.rules.unitCap = 32;

            Building core = Vars.state.teams.cores(Team.blue).first();

            redSpawns.clear();

            blueSpawnX = core.x();
            blueSpawnY = core.y();

            Vars.state.teams.cores(Team.blue).each(Building::kill);

            for (int x = 0; x < Vars.state.map.width; x++) {
                for (int y = 0; y < Vars.state.map.height; y++) {
                    Tile tile = Vars.world.tile(x, y);
                    if (tile.overlay() == Blocks.spawn) {
                        redSpawns.add(tile);
                        break;
                    }
                }
            }

            Timer timer = new Timer();
            timer.scheduleTask(task, 300, 300);

            Groups.player.each(player -> {
                if (player != null) {
                    player.team(Team.blue);
                    players.put(player.uuid(), new PlayerData(player));

                    Unit unit = getStartingUnit().spawn(Team.blue, blueSpawnX, blueSpawnY);
                    PlayerData data = players.get(player.uuid());
                    if(unit.type() == UnitTypes.elude) {
                        unit.apply(StatusEffects.sporeSlowed, Float.MAX_VALUE);
                    }
                    data.setUnit(unit);

                }
            });

            playing = true;
            gameRun = false;
            gameover = false;
            hardcore = false;
            stage = 1;
            stageTimer = 420;
            voting = false;
            StartingMenu.canOpenMenu = true;

            ClassChooseMenu.selectedUnit.clear();
            ClassChooseMenu.updateUnitsMap();

            initRules();
            launchGameStartTimer();

            Call.setRules(Vars.state.rules);

        }, 10));

        Events.run(EventType.Trigger.update, () -> {
            if (playing) {

                Hud.update();
            }
        });

    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("m", "Open Evolve menu, you must stand near lab", (args, player) -> {
            if (players.containsKey(player.uuid()) && players.get(player.uuid()).isCanEvolve()) {
                Locale locale = Bundle.findLocale(player.locale());

                Evolution evolution = Evolutions.evolutions.get(player.unit().type().name);

                String[][] buttons = new String[evolution.evolutions.length][1];

                for (int i = 0; i < evolution.evolutions.length; i++) {
                    buttons[i][0] = Bundle.format("menu.evolution.evolve", locale, evolution.evolutions[i], Evolutions.evolutions.get(evolution.evolutions[i]).cost);
                }

                Call.menu(player.con, Laboratory.evolutionMenu, Bundle.get("menu.evolution.title", locale), Bundle.format("menu.evolution.message", locale, players.get(player.uuid()).getEvolutionStage(), Bundle.get("evolution.branch.initial", locale)), buttons);
            }
        });
//        handler.<Player>register("sm", "Starting menu, you can open it only util 1 stage", (args, player) -> {
//            Log.info(StartingMenu.canOpenMenu);
//            StartingMenu.openMenu(player, 0);
//        });
        handler.<Player>register("b", "Open Building menu", (args, player) -> {
            if (playing && player.team() == Team.blue) {
                BuildMenu.openMenu(player);
            }
        });

        handler.<Player>register("bc", "Boss select menu", (args, player) -> {
            if (playing) {
                BossChooseMenu.openMenu(player);
            }
        });

        handler.<Player>register("c", "Open Class Choose menu, only for crux", (args, player) -> {
            if (player.team() == Team.crux) {
                ClassChooseMenu.openMenu(player);
            } else {
                player.sendMessage(Bundle.get("", player.locale));
            }
        });

        handler.<Player>register("s", "Open stations selecting menu", (args, player) -> StationsMenu.openMenu(player));

        handler.<Player>register("vote-map", "<map-number>", "Vote for specific map", (args, player) -> {
            if (Strings.canParseInt(args[0])) {
                MapVote.registerVote(player, Integer.valueOf(args[0]));
            } else {
                player.sendMessage(Bundle.get("not-a-number", player.locale));
            }
        });

        handler.<Player>register("ap", "Open a Admin Panel, only for admins", (args, player) -> {
            if (player.admin) {
                Admin.openAdminPanelMenu(player);
            }

        });

        handler.<Player>register("gameover", "Only for admins", (args, player) -> {
            if (player.admin) {
                callMapVoting();
                task.cancel();
            }
        });

        handler.<Player>register("blue", "Makes you blue, works only before stage 3", (args, player) -> {
            if (stage <= 3 && playing && player.team() != Team.blue) {
                Unit oldUnit = player.unit();

                if (player.unit() != null) oldUnit.kill();

                player.team(Team.blue);
                players.put(player.uuid(), new PlayerData(player));
                PlayerData data = players.get(player.uuid());
                Unit unit = getStartingUnit().spawn(Team.blue, blueSpawnX, blueSpawnY);

                data.setUnit(unit);
                player.unit(unit);
                sendBundled("game.redeemed", player.name);
            } else {
                player.sendMessage(Bundle.get("game.late", player.locale));
            }
        });

//        handler.<Player>register("reset-data", "Use that if you blue and dont have unit.", (args, player) -> {
//            if (player.team() == Team.blue) {
//                players.put(player.uuid(), new PlayerData(player));
//            }
//        });
    }
    @Override
    public void registerServerCommands(CommandHandler handler) {
//        handler.register("reload", "<config-name>", "Reload config", (args) -> {
//            if (Objects.equals(args[0], "units")) {
//                int multipler = UnitsConfig.getPrices_multipler();
//                Log.info(multipler);
//            } else if (Objects.equals(args[0], "premium")) {
//                Premium.init();
//            }
//        });

        handler.register("restart", "ae", (args) -> Groups.player.each(p -> p.kick("[scarlet]Server is going to restart")));
    }

    public static void gameOverCheck() {
        if (playerCount(Team.blue) == 0) {
            gameOver(Team.crux);
        }
    }

    public static void gameOver(Team winner) {
        if (winner == Team.crux) {
            if (playing) {
                RedVsBluePlugin.playing = false;
                if (!gameover) {
                    gameover = true;
                    callMapVoting();

                    task.cancel();

                    Groups.player.each(p -> {
                        int randomInt = getRandomInt(1, 255);
                        players.get(p.uuid()).setTeam(Team.get(randomInt));
                        p.team(Team.get(randomInt));
                    });
                }
            }
        } else if (winner == Team.blue) {
            if (!gameover) {
                gameover = true;
                callMapVoting();

                task.cancel();

                Groups.player.each(p -> {
                    int randomInt = getRandomInt(1, 255);
                    players.get(p.uuid()).setTeam(Team.get(randomInt));
                    p.team(Team.get(randomInt));
                });
            }
        }
    }
}

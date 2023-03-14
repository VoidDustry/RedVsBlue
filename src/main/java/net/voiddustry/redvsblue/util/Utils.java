package net.voiddustry.redvsblue.util;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.ui.Menus;

import java.awt.*;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;

import static net.voiddustry.redvsblue.RedVsBluePlugin.players;
import static net.voiddustry.redvsblue.RedVsBluePlugin.selectedBuildBlock;
import net.voiddustry.redvsblue.Bundle;
import net.voiddustry.redvsblue.PlayerData;
import net.voiddustry.redvsblue.RedVsBluePlugin;

public class Utils {

    public void initRules() {
        Vars.state.rules.hideBannedBlocks = true;
        Vars.state.rules.bannedBlocks.addAll();
    }

    public static int getRandomInt(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    public static Player getRandomPlayer(Team team) {
        Player[] teamPlayers = new Player[playerCount(team)];
        final int[] i = { 0 };
        Groups.player.each(player -> {
            if (player.team() == team) {
                teamPlayers[i[0]] = player;
                i[0]++;
            }
        });

        return teamPlayers[getRandomInt(0, teamPlayers.length - 1)];
    }

    private static char randomChar() {
        Random r = new Random();
        return (char) (r.nextInt(26) + 'A');
    }

    public static Player getRandomPlayer() {
        return Groups.player.index(getRandomInt(0, Groups.player.size() - 1));
    }

    public static void spawnBoss() {
        Unit boss = UnitTypes.antumbra.spawn(Team.crux, RedVsBluePlugin.redSpawnX, RedVsBluePlugin.redSpawnY);
        boss.health(14000);

        Player player = getRandomPlayer(Team.crux);

        if (!boss.dead()) {
            Call.unitControl(player, boss);

            sendBundled("game.boss.spawn", player.name());
        }
    }

    public static int playerCount(Team team) {
        final int[] i = { 0 };
        Groups.player.each(p -> {
            if (p.team() == team)
                i[0]++;
        });
        return i[0];
    }

    public static int playerCount() {
        return Groups.player.size();
    }

    public static void openBlockSelectMenu(Player player) {
        int menu = Menus.registerMenu((playerInMenu, option) -> {
            switch (option) {
                case 0 -> selectedBuildBlock.put(player.uuid(), Blocks.titaniumWall);
                case 1 -> selectedBuildBlock.put(player.uuid(), Blocks.door);
                case 2 -> selectedBuildBlock.put(player.uuid(), Blocks.powerNode);

                case 3 -> selectedBuildBlock.put(player.uuid(), Blocks.combustionGenerator);
                case 4 -> selectedBuildBlock.put(player.uuid(), Blocks.mender);
                case 5 -> selectedBuildBlock.put(player.uuid(), Blocks.battery);

                case 6 -> {
                    PlayerData data = players.get(player.uuid());
                    if (data.getScore() < 3) {
                        player.sendMessage(Bundle.get("build.not-enough-money", player.locale));
                    } else {
                        player.unit().addItem(Items.coal, 5);
                        data.setScore(data.getScore() - 3);
                    }
                }
                case 7 -> selectedBuildBlock.put(player.uuid(), Blocks.air);
            }
        });
        String[][] buttonsRow = {
                {
                        "\uF8AC", // titanium-wall
                        "\uF8A2", // door
                        "\uF87E", // power-node
                },
                {
                        "\uF879", // combustionGenerator
                        "\uF89B", // mender
                        "\uF87B" // battery
                },
                {
                        "[gray]Buy 5 Coal"
                },
                {
                        "[scarlet]Destroy Wall"
                }
        };
        Call.menu(player.con, menu, "[cyan]Select Block To Build", "", buttonsRow);

    }

    public static void sendBundled(String key, Object... format) {
        Groups.player.forEach(p -> {
            Locale locale = Bundle.findLocale(p.locale());
            p.sendMessage(Bundle.format(key, locale, format));
        });
    }

    public void sendBundled(String key) {
        Groups.player.forEach(p -> {
            Locale locale = Bundle.findLocale(p.locale());
            p.sendMessage(Bundle.get(key, locale));
        });
    }

}

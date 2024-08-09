package net.voiddustry.redvsblue.game.stations;

import arc.graphics.Color;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.world.Tile;
import net.voiddustry.redvsblue.Bundle;
import net.voiddustry.redvsblue.game.stations.stationData.StationData;
import net.voiddustry.redvsblue.util.Utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.voiddustry.redvsblue.RedVsBluePlugin.players;

public class Recycler {
    private static final Map<String, StationData> recyclersmap = new ConcurrentHashMap<>();

    public static void initTimer() {
        Timer.schedule(() -> recyclersmap.forEach((owner, pointData) -> {
            int centerX = pointData.tileOn().x * 8;
            int centerY = pointData.tileOn().y * 8;

            for (int i = 0; i < 19; i++) {
                Call.effect(Fx.vaporSmall, (float) (centerX + Math.sin(i) * 32), (float) (centerY + Math.cos(i) * 32), 1, Color.gray);
            }

            String text = pointData.owner().name + "[gold]'s\n[#023919]Recycler";
            StationUtils.drawStationName(pointData.tileOn(), text, 0.8F);

            Groups.player.each(p -> {
                if (p.team() == Team.blue && p.unit() != null) {
                    if (p.dst(centerX, centerY) <= 32) {
                        if (p.unit().stack.amount >= 20) {
                            int add = p.unit().stack.amount/20;
                            Utils.label(p.x, p.y, "[#023919]+" + add, 3, 0.8F);
                            p.unit().stack.amount -= add * 20;

                            players.get(p.uuid()).addScore(add);
                        }
                    }
                }
            });
        }), 0, 0.7F);

        Timer.schedule(Recycler::renderRecycler, 0, 1);
    }

    public static void buyRecycler(Player player) {
        if (players.get(player.uuid()).getScore() < 6) {
            player.sendMessage(Bundle.format("station.not-enough-money", Bundle.findLocale(player.locale), 6));
        } else {
            if (!recyclersmap.containsKey(player.uuid())) {
                if (!player.dead()) {
                    Tile playerTileOn = player.tileOn();
                    Tile tileUnderPlayer = Vars.world.tile(playerTileOn.x, playerTileOn.y - 1);

                    if (!player.dead() && player.team() == Team.blue && tileUnderPlayer.block().isAir()) {
                        StationData recyclerData = new StationData(player, tileUnderPlayer);
                        recyclersmap.put(player.uuid(), recyclerData);
                        Call.constructFinish(tileUnderPlayer, Blocks.slagIncinerator, null, (byte) 0, Team.blue, null);
                        Call.effect(Fx.regenParticle, tileUnderPlayer.x*8, tileUnderPlayer.y*8, 0, Color.red);
                        players.get(player.uuid()).subtractScore(6);
                    }
                }
            }
        }
    }

    public static void renderRecycler() {
        recyclersmap.forEach((owner, recycler) -> {
            if (recycler != null) {
                if (recycler.tileOn().block() == Blocks.air || recycler.owner().team() != Team.blue) {
                    recyclersmap.remove(owner);
                    if (recycler.tileOn().block() == Blocks.slagIncinerator) {
                        recycler.tileOn().build.kill();
                    }
                }
            }
        });
    }

    public static void clearRecyclers() {
        recyclersmap.clear();
    }
}

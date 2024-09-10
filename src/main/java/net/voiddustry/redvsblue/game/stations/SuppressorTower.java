package net.voiddustry.redvsblue.game.stations;

import arc.graphics.Color;

import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.world.Tile;
import net.voiddustry.redvsblue.Bundle;
import net.voiddustry.redvsblue.game.stations.stationData.StationData;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.voiddustry.redvsblue.RedVsBluePlugin.players;

public class SuppressorTower {
    private static final Map<String, StationData> suppressorTowerMap = new ConcurrentHashMap<>();

    public static void initTimer() {
        Timer.schedule(() -> suppressorTowerMap.forEach((owner, pointData) -> {
            int centerX = pointData.tileOn().x * 8;
            int centerY = pointData.tileOn().y * 8;

            for (int i = 0; i < 19; i++) {
                Call.effect(Fx.fire, (float) (centerX + Math.sin(i) * 128), (float) (centerY + Math.cos(i) * 128), 0, Color.red);
            }


            String text = pointData.owner().name + "[gold]'s\n[accent]Suppressor Tower";
            StationUtils.drawStationName(pointData.tileOn(), text, 0.8F);

            Groups.unit.each(u -> {
               if (u.team() == Team.crux) {
                    if (u.dst(centerX, centerY) <= 128) {
                        float removedHp = u.maxHealth / 125;

                        u.health -= removedHp;
                        u.apply(StatusEffects.sapped, 50);

                        String removedHpText = "[#55557F]-" + Math.round(removedHp*10)/10;
                        Call.label(removedHpText, 0.5F, u.x, u.y);
                    }

                }
            });
        }), 0, 0.5F);
        Timer.schedule(SuppressorTower::renderSuppressorTowers, 0, 0.5F);
    }

    public static void buyTower(Player player) {
        if (players.get(player.uuid()).getScore() < 20) {
            player.sendMessage(Bundle.format("station.not-enough-money", Bundle.findLocale(player.locale), 20));
        } else {
            if (!suppressorTowerMap.containsKey(player.uuid())) {
                if (!player.dead()) {
                    Tile playerTileOn = player.tileOn();
                    Tile tileUnderPlayer = Vars.world.tile(playerTileOn.x, playerTileOn.y - 1);

                    if (!player.dead() && player.team() == Team.blue && tileUnderPlayer.block().isAir()) {
                        StationData towerData = new StationData(player, tileUnderPlayer);
                        suppressorTowerMap.put(player.uuid(), towerData);
                        Call.constructFinish(tileUnderPlayer, Blocks.phaseWall, null, (byte) 0, Team.blue, null);
                        Call.effect(Fx.regenParticle, tileUnderPlayer.x*8, tileUnderPlayer.y*8, 0, Color.red);
                        players.get(player.uuid()).subtractScore(20);
                    }
                }
            }
        }
    }

    public static void renderSuppressorTowers() {
        suppressorTowerMap.forEach((owner, tower) -> {
            if (tower != null) {
                if (tower.tileOn().block() == Blocks.air || tower.owner().team() != Team.blue) {
                    suppressorTowerMap.remove(owner);
                    if (tower.tileOn().block() == Blocks.phaseWall) {
                        tower.tileOn().build.kill();
                    }
                }
            }
        });
    }

    public static void clearTowers() {
        suppressorTowerMap.clear();
    }
}

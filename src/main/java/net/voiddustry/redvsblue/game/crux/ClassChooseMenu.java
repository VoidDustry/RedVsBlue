package net.voiddustry.redvsblue.game.crux;

import arc.struct.ObjectMap;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.type.UnitType;
import mindustry.ui.Menus;
import net.voiddustry.redvsblue.Bundle;
import net.voiddustry.redvsblue.RedVsBluePlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class ClassChooseMenu {

    public static final ObjectMap<UnitType, String> units = new ObjectMap<>();
    public static final Map<String, UnitType> selectedUnit = new HashMap<>();

    public static void openMenu(Player player) {
        String[][] unitsBundleKeys = new String[units.size][1];
        UnitType[] unitsNumbers = new UnitType[units.size];

        AtomicInteger i = new AtomicInteger();

        units.forEach((object) -> {
            unitsBundleKeys[i.get()][0] = Bundle.get(object.value, player.locale);
            unitsNumbers[i.get()] = object.key;
            i.addAndGet(1);
        });

        int menu = Menus.registerMenu((playerInMenu, option) -> {
            selectedUnit.put(player.uuid(), unitsNumbers[option]);
            CruxUnit.callSpawn(player);
        });

        Call.menu(player.con, menu, Bundle.get("units.crux.menu.title", player.locale), "", unitsBundleKeys);
    }

    public static void updateUnitsMap() {
        units.clear();
        switch (RedVsBluePlugin.stage) {
            case 1 -> units.putAll(StageUnits.firstStage);
            case 2 -> units.putAll(StageUnits.secondStage);
            case 3 -> units.putAll(StageUnits.thirdStage);
            case 4,5 -> units.putAll(StageUnits.fourthAndFifrhStage);
            case 6 -> units.putAll(StageUnits.sixthStage);
        }
    }
}


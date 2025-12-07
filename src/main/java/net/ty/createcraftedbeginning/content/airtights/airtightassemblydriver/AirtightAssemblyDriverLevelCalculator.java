package net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

import static net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver.AirtightAssemblyDriverCore.MAX_LEVEL;

public class AirtightAssemblyDriverLevelCalculator {
    private static final String COMPOUND_KEY_CURRENT_LEVEL = "CurrentLevel";
    private static final String COMPOUND_KEY_MAX_LEVEL_FOR_RESIDUE = "MaxLevelForResidue";
    private static final String COMPOUND_KEY_MAX_LEVEL_FOR_SUPPLY = "MaxLevelForSupply";
    private static final String COMPOUND_KEY_MAX_LEVEL_FOR_WIND_CHARGING = "MaxLevelForWindCharging";
    private static final String COMPOUND_KEY_MAX_VALUE = "MaxValue";
    private static final String COMPOUND_KEY_MIN_VALUE = "MinValue";
    private static final String COMPOUND_KEY_RESIDUE_LEVEL = "ResidueLevel";
    private static final String COMPOUND_KEY_SUPPLY_LEVEL = "SupplyLevel";
    private static final String COMPOUND_KEY_WIND_CHARGING_LEVEL = "WindChargingLevel";

    private int windChargingLevel;
    private int residueLevel;
    private int currentLevel;
    private int maxLevelForResidue;
    private int maxLevelForSupply;
    private int maxLevelForWindCharging;
    private int maxValue;
    private int minValue;
    private int supplyLevel;

    public void updateWindChargingLevel(int newLevel) {
        windChargingLevel = newLevel;
    }

    public void updateSupplyLevel(int newLevel) {
        supplyLevel = newLevel;
    }

    public int getResidueLevel() {
        return residueLevel;
    }

    public void updateResidueLevel(int newLevel) {
        residueLevel = newLevel;
    }

    public Map<LevelKey, Integer> getLevels() {
        Map<LevelKey, Integer> levels = new EnumMap<>(LevelKey.class);
        levels.put(LevelKey.SUPPLY, maxLevelForSupply);
        levels.put(LevelKey.WIND_CHARGING, maxLevelForWindCharging);
        levels.put(LevelKey.RESIDUE, maxLevelForResidue);
        levels.put(LevelKey.MIN_VALUE, minValue);
        levels.put(LevelKey.MAX_VALUE, maxValue);
        return levels;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void reset() {
        currentLevel = 0;
        maxLevelForResidue = 0;
        maxLevelForSupply = 0;
        maxLevelForWindCharging = 0;
        maxValue = 0;
        minValue = 0;
        residueLevel = 0;
        supplyLevel = 0;
        windChargingLevel = 0;
    }

    public CompoundTag write() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt(COMPOUND_KEY_CURRENT_LEVEL, currentLevel);
        compoundTag.putInt(COMPOUND_KEY_MAX_LEVEL_FOR_RESIDUE, maxLevelForResidue);
        compoundTag.putInt(COMPOUND_KEY_MAX_LEVEL_FOR_SUPPLY, maxLevelForSupply);
        compoundTag.putInt(COMPOUND_KEY_MAX_LEVEL_FOR_WIND_CHARGING, maxLevelForWindCharging);
        compoundTag.putInt(COMPOUND_KEY_MAX_VALUE, maxValue);
        compoundTag.putInt(COMPOUND_KEY_MIN_VALUE, minValue);
        compoundTag.putInt(COMPOUND_KEY_RESIDUE_LEVEL, residueLevel);
        compoundTag.putInt(COMPOUND_KEY_SUPPLY_LEVEL, supplyLevel);
        compoundTag.putInt(COMPOUND_KEY_WIND_CHARGING_LEVEL, windChargingLevel);
        return compoundTag;
    }

    public void read(@NotNull CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_CURRENT_LEVEL)) {
            currentLevel = compoundTag.getInt(COMPOUND_KEY_CURRENT_LEVEL);
        }
        if (compoundTag.contains(COMPOUND_KEY_MAX_LEVEL_FOR_RESIDUE)) {
            maxLevelForResidue = compoundTag.getInt(COMPOUND_KEY_MAX_LEVEL_FOR_RESIDUE);
        }
        if (compoundTag.contains(COMPOUND_KEY_MAX_LEVEL_FOR_SUPPLY)) {
            maxLevelForSupply = compoundTag.getInt(COMPOUND_KEY_MAX_LEVEL_FOR_SUPPLY);
        }
        if (compoundTag.contains(COMPOUND_KEY_MAX_LEVEL_FOR_WIND_CHARGING)) {
            maxLevelForWindCharging = compoundTag.getInt(COMPOUND_KEY_MAX_LEVEL_FOR_WIND_CHARGING);
        }
        if (compoundTag.contains(COMPOUND_KEY_MAX_VALUE)) {
            maxValue = compoundTag.getInt(COMPOUND_KEY_MAX_VALUE);
        }
        if (compoundTag.contains(COMPOUND_KEY_MIN_VALUE)) {
            minValue = compoundTag.getInt(COMPOUND_KEY_MIN_VALUE);
        }
        if (compoundTag.contains(COMPOUND_KEY_RESIDUE_LEVEL)) {
            residueLevel = compoundTag.getInt(COMPOUND_KEY_RESIDUE_LEVEL);
        }
        if (compoundTag.contains(COMPOUND_KEY_SUPPLY_LEVEL)) {
            supplyLevel = compoundTag.getInt(COMPOUND_KEY_SUPPLY_LEVEL);
        }
        if (compoundTag.contains(COMPOUND_KEY_WIND_CHARGING_LEVEL)) {
            windChargingLevel = compoundTag.getInt(COMPOUND_KEY_WIND_CHARGING_LEVEL);
        }
        update();
    }

    public void update() {
        maxLevelForWindCharging = Math.min(MAX_LEVEL, windChargingLevel);
        maxLevelForSupply = Math.min(MAX_LEVEL, supplyLevel);
        maxLevelForResidue = Math.min(MAX_LEVEL, residueLevel);
        minValue = Math.min(Math.min(maxLevelForWindCharging, maxLevelForSupply), maxLevelForResidue);
        maxValue = Math.max(Math.max(maxLevelForWindCharging, maxLevelForSupply), maxLevelForResidue);
        currentLevel = minValue;
    }

    public enum LevelKey {
        SUPPLY,
        WIND_CHARGING,
        RESIDUE,
        MIN_VALUE,
        MAX_VALUE
    }
}

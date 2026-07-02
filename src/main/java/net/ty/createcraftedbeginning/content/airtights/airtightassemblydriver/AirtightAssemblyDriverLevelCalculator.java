package net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;
import java.util.Map;

import static net.ty.createcraftedbeginning.content.airtights.airtightassemblydriver.AirtightAssemblyDriverCore.MAX_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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

    private final AirtightAssemblyDriverCore driverCore;
    private int windChargingLevel;
    private int residueLevel;
    private int currentLevel;
    private int maxLevelForResidue;
    private int maxLevelForSupply;
    private int maxLevelForWindCharging;
    private int maxValue;
    private int minValue;
    private int supplyLevel;

    public AirtightAssemblyDriverLevelCalculator(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    public void updateWindChargingLevel(int newLevel) {
        windChargingLevel = Math.max(0, newLevel);
    }

    public void updateSupplyLevel(int newLevel) {
        supplyLevel = Math.max(0, newLevel);
    }

    public int getResidueLevel() {
        return residueLevel;
    }

    public void updateResidueLevel(int newLevel) {
        residueLevel = Math.max(0, newLevel);
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
        boolean changed = currentLevel != 0 || maxLevelForResidue != 0 || maxLevelForSupply != 0 || maxLevelForWindCharging != 0 || maxValue != 0 || minValue != 0 || residueLevel != 0 || supplyLevel != 0 || windChargingLevel != 0;
        currentLevel = 0;
        maxLevelForResidue = 0;
        maxLevelForSupply = 0;
        maxLevelForWindCharging = 0;
        maxValue = 0;
        minValue = 0;
        residueLevel = 0;
        supplyLevel = 0;
        windChargingLevel = 0;
        if (!changed) {
            return;
        }

        driverCore.markDirty();
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

    public void read(CompoundTag compoundTag) {
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
        update(false);
    }

    public void update(boolean dirty) {
        int previousMaxLevelForWindCharging = maxLevelForWindCharging;
        int previousMaxLevelForSupply = maxLevelForSupply;
        int previousMaxLevelForResidue = maxLevelForResidue;
        int previousMaxValue = maxValue;
        int previousMinValue = minValue;
        int previousCurrentLevel = currentLevel;

        int newMaxLevelForWindCharging = Math.min(MAX_LEVEL, windChargingLevel);
        int newMaxLevelForSupply = Math.min(MAX_LEVEL, supplyLevel);
        int newMaxLevelForResidue = Math.min(MAX_LEVEL, residueLevel);
        int newMaxValue = Math.max(Math.max(newMaxLevelForWindCharging, newMaxLevelForSupply), newMaxLevelForResidue);
        int newMinValue = Math.min(Math.min(newMaxLevelForWindCharging, newMaxLevelForSupply), newMaxLevelForResidue);

        maxLevelForWindCharging = newMaxLevelForWindCharging;
        maxLevelForSupply = newMaxLevelForSupply;
        maxLevelForResidue = newMaxLevelForResidue;
        maxValue = newMaxValue;
        minValue = newMinValue;
        currentLevel = newMinValue;
        boolean changed = previousMaxLevelForWindCharging != maxLevelForWindCharging || previousMaxLevelForSupply != maxLevelForSupply || previousMaxLevelForResidue != maxLevelForResidue || previousMaxValue != maxValue || previousMinValue != minValue || previousCurrentLevel != currentLevel;
        if (!changed || !dirty) {
            return;
        }

        driverCore.markDirty();
    }

    public enum LevelKey {
        SUPPLY,
        WIND_CHARGING,
        RESIDUE,
        MIN_VALUE,
        MAX_VALUE
    }
}

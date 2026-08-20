package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;
import java.util.Map;

import static net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore.MAX_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AirtightAssemblyDriverLevelCalculator {
    private static final String COMPOUND_KEY_RESIDUE_LEVEL = "ResidueLevel";
    private static final String COMPOUND_KEY_SUPPLY_LEVEL = "SupplyLevel";
    private static final String COMPOUND_KEY_WIND_CHARGING_LEVEL = "WindChargingLevel";

    private final AirtightAssemblyDriverCore driverCore;

    private int windChargingLevel;
    private int residueLevel;
    private int supplyLevel;

    AirtightAssemblyDriverLevelCalculator(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    private static int readLevel(CompoundTag compoundTag, String key) {
        return compoundTag.contains(key) ? clampLevel(compoundTag.getInt(key)) : 0;
    }

    private static int clampLevel(int level) {
        return Mth.clamp(level, 0, MAX_LEVEL);
    }

    void updateWindChargingLevel(int newLevel) {
        if (!setWindChargingLevel(newLevel)) {
            return;
        }

        driverCore.markForClientSync();
    }

    void updateSupplyLevel(int newLevel) {
        if (!setSupplyLevel(newLevel)) {
            return;
        }

        driverCore.markForSaveAndClientSync();
    }

    void updateResidueLevel(int newLevel) {
        if (!setResidueLevel(newLevel)) {
            return;
        }

        driverCore.markForSaveAndClientSync();
    }

    void loadWindChargingLevel() {
        setWindChargingLevel(0);
    }

    void loadSupplyLevel(int newLevel) {
        setSupplyLevel(newLevel);
    }

    int getResidueLevel() {
        return residueLevel;
    }

    int getSupplyLevel() {
        return supplyLevel;
    }

    Map<LevelKey, Integer> getLevels() {
        Map<LevelKey, Integer> levels = new EnumMap<>(LevelKey.class);
        levels.put(LevelKey.SUPPLY, supplyLevel);
        levels.put(LevelKey.WIND_CHARGING, windChargingLevel);
        levels.put(LevelKey.RESIDUE, residueLevel);
        levels.put(LevelKey.MIN_VALUE, getMinimumLevel());
        levels.put(LevelKey.MAX_VALUE, getMaximumLevel());
        return levels;
    }

    int getCurrentLevel() {
        return driverCore.getStructureManager().isActive() ? getMinimumLevel() : 0;
    }

    void reset() {
        boolean hadLevels = windChargingLevel != 0 || residueLevel != 0 || supplyLevel != 0;
        windChargingLevel = 0;
        residueLevel = 0;
        supplyLevel = 0;
        if (!hadLevels) {
            return;
        }

        driverCore.markForSaveAndClientSync();
    }

    CompoundTag write(boolean clientPacket) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(COMPOUND_KEY_RESIDUE_LEVEL, residueLevel);
        if (!clientPacket) {
            return tag;
        }

        tag.putInt(COMPOUND_KEY_SUPPLY_LEVEL, supplyLevel);
        tag.putInt(COMPOUND_KEY_WIND_CHARGING_LEVEL, windChargingLevel);
        return tag;
    }

    void read(CompoundTag compoundTag, boolean clientPacket) {
        supplyLevel = clientPacket ? readLevel(compoundTag, COMPOUND_KEY_SUPPLY_LEVEL) : 0;
        windChargingLevel = clientPacket ? readLevel(compoundTag, COMPOUND_KEY_WIND_CHARGING_LEVEL) : 0;
        residueLevel = readLevel(compoundTag, COMPOUND_KEY_RESIDUE_LEVEL);
    }

    private int getMinimumLevel() {
        return Math.min(supplyLevel, Math.min(windChargingLevel, residueLevel));
    }

    private int getMaximumLevel() {
        return Math.max(supplyLevel, Math.max(windChargingLevel, residueLevel));
    }

    private boolean setWindChargingLevel(int newLevel) {
        int clampedLevel = clampLevel(newLevel);
        if (windChargingLevel == clampedLevel) {
            return false;
        }

        windChargingLevel = clampedLevel;
        return true;
    }

    private boolean setSupplyLevel(int newLevel) {
        int clampedLevel = clampLevel(newLevel);
        if (supplyLevel == clampedLevel) {
            return false;
        }

        supplyLevel = clampedLevel;
        return true;
    }

    private boolean setResidueLevel(int newLevel) {
        int clampedLevel = clampLevel(newLevel);
        if (residueLevel == clampedLevel) {
            return false;
        }

        residueLevel = clampedLevel;
        return true;
    }

    enum LevelKey {
        SUPPLY,
        WIND_CHARGING,
        RESIDUE,
        MIN_VALUE,
        MAX_VALUE
    }
}

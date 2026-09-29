package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.foundation.NbtValues;

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
    private double supplyLevel;

    AirtightAssemblyDriverLevelCalculator(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    void updateWindChargingLevel(int newLevel) {
        if (!setWindChargingLevel(newLevel)) {
            return;
        }

        driverCore.markForClientSync();
    }

    void updateSupplyLevel(double newLevel) {
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

    void loadSupplyLevel(double newLevel) {
        setSupplyLevel(newLevel);
    }

    int getResidueLevel() {
        return residueLevel;
    }

    int getSupplyLevel() {
        return Mth.floor(supplyLevel);
    }

    Map<LevelKey, Double> getLevels() {
        Map<LevelKey, Double> levels = new EnumMap<>(LevelKey.class);
        levels.put(LevelKey.SUPPLY, supplyLevel);
        levels.put(LevelKey.WIND_CHARGING, (double) windChargingLevel);
        levels.put(LevelKey.RESIDUE, (double) residueLevel);
        levels.put(LevelKey.MIN_VALUE, getMinimumLevel());
        levels.put(LevelKey.MAX_VALUE, getMaximumLevel());
        return levels;
    }

    int getCurrentLevel() {
        if (!driverCore.getStructureManager().isActive()) {
            return 0;
        }

        return Mth.floor(getMinimumLevel());
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
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt(COMPOUND_KEY_RESIDUE_LEVEL, residueLevel);
        if (!clientPacket) {
            return compoundTag;
        }

        compoundTag.putDouble(COMPOUND_KEY_SUPPLY_LEVEL, supplyLevel);
        compoundTag.putInt(COMPOUND_KEY_WIND_CHARGING_LEVEL, windChargingLevel);
        return compoundTag;
    }

    void read(CompoundTag compoundTag, boolean clientPacket) {
        double storedSupplyLevel = clientPacket ? NbtValues.getDoubleOrDefault(compoundTag, COMPOUND_KEY_SUPPLY_LEVEL, 0) : 0;
        setSupplyLevel(storedSupplyLevel);
        windChargingLevel = clientPacket ? readLevel(compoundTag, COMPOUND_KEY_WIND_CHARGING_LEVEL) : 0;
        residueLevel = readLevel(compoundTag, COMPOUND_KEY_RESIDUE_LEVEL);
    }

    private static int readLevel(CompoundTag compoundTag, String key) {
        return Mth.clamp(NbtValues.getIntOrDefault(compoundTag, key, 0), 0, MAX_LEVEL);
    }

    private double getMinimumLevel() {
        return Math.min(supplyLevel, Math.min(windChargingLevel, residueLevel));
    }

    private double getMaximumLevel() {
        return Math.max(supplyLevel, Math.max(windChargingLevel, residueLevel));
    }

    private boolean setWindChargingLevel(int newLevel) {
        int clampedLevel = Mth.clamp(newLevel, 0, MAX_LEVEL);
        if (windChargingLevel == clampedLevel) {
            return false;
        }

        windChargingLevel = clampedLevel;
        return true;
    }

    private boolean setSupplyLevel(double newLevel) {
        double clampedLevel = Double.isFinite(newLevel) ? Mth.clamp(newLevel, 0, MAX_LEVEL) : 0;
        if (supplyLevel == clampedLevel) {
            return false;
        }

        supplyLevel = clampedLevel;
        return true;
    }

    private boolean setResidueLevel(int newLevel) {
        int clampedLevel = Mth.clamp(newLevel, 0, MAX_LEVEL);
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

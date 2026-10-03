package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.foundation.NbtValues;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;
import java.util.Map;

import static net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock.MAX_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class TeslaTurbineLevelCalculator {
    private static final int LEVELS_PER_ROTOR = 2;
    private static final String COMPOUND_KEY_SUPPLY_LEVEL = "SupplyLevel";
    private static final String COMPOUND_KEY_TYPE_LEVEL = "TypeLevel";
    private final TeslaTurbineCore core;
    private final TeslaTurbineBlockEntity turbine;

    private float supplyLevel;
    private float typeLevel;

    TeslaTurbineLevelCalculator(TeslaTurbineCore core, TeslaTurbineBlockEntity turbine) {
        this.core = core;
        this.turbine = turbine;
    }

    private static float readLevel(CompoundTag compoundTag, String key) {
        float level = NbtValues.getFloatOrDefault(compoundTag, key, 0);
        if (!Float.isFinite(level)) {
            return 0;
        }

        return Mth.clamp(level, 0, MAX_LEVEL);
    }

    void updateSupplyLevel(float newLevel) {
        if (!setSupplyLevel(newLevel)) {
            return;
        }

        core.markForClientSync();
    }

    void loadSupplyLevel(float newLevel) {
        setSupplyLevel(newLevel);
    }

    void updateTypeLevel(float newLevel) {
        if (!setTypeLevel(newLevel)) {
            return;
        }

        core.markForClientSync();
    }

    void loadTypeLevel(float newLevel) {
        setTypeLevel(newLevel);
    }

    Map<LevelKey, Float> getLevels() {
        float rotorLevel = getRotorLevel();
        float minimumLevel = Math.min(supplyLevel, Math.min(rotorLevel, typeLevel));
        float maximumLevel = Math.max(supplyLevel, Math.max(rotorLevel, typeLevel));
        Map<LevelKey, Float> levels = new EnumMap<>(LevelKey.class);
        levels.put(LevelKey.SUPPLY, supplyLevel);
        levels.put(LevelKey.ROTOR, rotorLevel);
        levels.put(LevelKey.TYPE, typeLevel);
        levels.put(LevelKey.MIN_VALUE, minimumLevel);
        levels.put(LevelKey.MAX_VALUE, maximumLevel);
        return levels;
    }

    float getSpeed() {
        return turbine.getGeneratedSpeed();
    }

    int getCurrentLevel() {
        return Mth.floor(Math.min(supplyLevel, Math.min(getRotorLevel(), typeLevel)));
    }

    CompoundTag write() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putFloat(COMPOUND_KEY_SUPPLY_LEVEL, supplyLevel);
        compoundTag.putFloat(COMPOUND_KEY_TYPE_LEVEL, typeLevel);
        return compoundTag;
    }

    void read(CompoundTag compoundTag, boolean clientPacket) {
        if (!clientPacket) {
            supplyLevel = 0;
            typeLevel = 0;
            return;
        }

        supplyLevel = readLevel(compoundTag, COMPOUND_KEY_SUPPLY_LEVEL);
        typeLevel = readLevel(compoundTag, COMPOUND_KEY_TYPE_LEVEL);
    }

    private int getRotorLevel() {
        int rotorCount = turbine.getBlockState().getValue(TeslaTurbineBlock.ROTOR);
        return Mth.clamp(rotorCount * LEVELS_PER_ROTOR, 0, MAX_LEVEL);
    }

    private boolean setSupplyLevel(float newLevel) {
        float clampedLevel = Mth.clamp(newLevel, 0, MAX_LEVEL);
        if (supplyLevel == clampedLevel) {
            return false;
        }

        supplyLevel = clampedLevel;
        return true;
    }

    private boolean setTypeLevel(float newLevel) {
        float clampedLevel = Mth.clamp(newLevel, 0, MAX_LEVEL);
        if (typeLevel == clampedLevel) {
            return false;
        }

        typeLevel = clampedLevel;
        return true;
    }

    enum LevelKey {
        SUPPLY,
        ROTOR,
        TYPE,
        MIN_VALUE,
        MAX_VALUE
    }
}

package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.turbinehandlers.AirtightTurbineHandlerUtils;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;
import java.util.Map;

import static net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineUtils.LEVELS_PER_ROTOR;
import static net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineUtils.MAX_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class TeslaTurbineLevelCalculator {
    private static final String COMPOUND_KEY_SUPPLY_LEVEL = "SupplyLevel";
    private static final String COMPOUND_KEY_TYPE_LEVEL = "TypeLevel";
    private final TeslaTurbineCore core;
    private final TeslaTurbineBlockEntity turbine;

    private int supplyLevel;
    private int typeLevel;

    TeslaTurbineLevelCalculator(TeslaTurbineCore core, TeslaTurbineBlockEntity turbine) {
        this.core = core;
        this.turbine = turbine;
    }

    private static int readLevel(CompoundTag compoundTag, String key) {
        return CCBMathUtils.clampNonNegative(CCBNbtUtils.getIntOrDefault(compoundTag, key, 0), MAX_LEVEL);
    }

    void updateSupplyLevel(int newLevel) {
        if (!setSupplyLevel(newLevel)) {
            return;
        }

        core.markForClientSync();
    }

    void loadSupplyLevel(int newLevel) {
        setSupplyLevel(newLevel);
    }

    void loadTypeLevel() {
        setTypeLevel(getGasTypeLevel());
    }

    Map<LevelKey, Integer> getLevels() {
        int rotorLevel = getRotorLevel();
        int minimumLevel = Math.min(supplyLevel, Math.min(rotorLevel, typeLevel));
        int maximumLevel = Math.max(supplyLevel, Math.max(rotorLevel, typeLevel));
        Map<LevelKey, Integer> levels = new EnumMap<>(LevelKey.class);
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
        return Math.min(supplyLevel, Math.min(getRotorLevel(), typeLevel));
    }

    CompoundTag write() {
        CompoundTag compoundTag = new CompoundTag();
        CCBNbtUtils.putInt(compoundTag, COMPOUND_KEY_SUPPLY_LEVEL, supplyLevel);
        CCBNbtUtils.putInt(compoundTag, COMPOUND_KEY_TYPE_LEVEL, typeLevel);
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

    private int getGasTypeLevel() {
        GasStack gasType = core.getFlowMeter().getGasType();
        if (gasType.isEmpty()) {
            return 0;
        }
        return AirtightTurbineHandlerUtils.of(gasType).getMaxLevel();
    }

    private int getRotorLevel() {
        int rotorCount = turbine.getBlockState().getValue(TeslaTurbineBlock.ROTOR);
        return CCBMathUtils.clampNonNegative(rotorCount * LEVELS_PER_ROTOR, MAX_LEVEL);
    }

    private boolean setSupplyLevel(int newLevel) {
        int clampedLevel = CCBMathUtils.clampNonNegative(newLevel, MAX_LEVEL);
        if (supplyLevel == clampedLevel) {
            return false;
        }

        supplyLevel = clampedLevel;
        return true;
    }

    private void setTypeLevel(int newLevel) {
        int clampedLevel = CCBMathUtils.clampNonNegative(newLevel, MAX_LEVEL);
        if (typeLevel == clampedLevel) {
            return;
        }

        typeLevel = clampedLevel;
    }

    enum LevelKey {
        SUPPLY,
        ROTOR,
        TYPE,
        MIN_VALUE,
        MAX_VALUE
    }
}

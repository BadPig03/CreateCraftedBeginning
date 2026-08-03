package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.turbinehandlers.AirtightTurbineHandlerUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;
import java.util.Map;

import static net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineUtils.LEVELS_PER_ROTOR;
import static net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineUtils.MAX_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTurbineLevelCalculator {
    private static final String COMPOUND_KEY_SUPPLY_LEVEL = "SupplyLevel";
    private static final String COMPOUND_KEY_TYPE_LEVEL = "TypeLevel";
    private final TeslaTurbineCore core;
    private final TeslaTurbineBlockEntity turbine;

    private int supplyLevel;
    private int typeLevel;

    public TeslaTurbineLevelCalculator(TeslaTurbineCore core, TeslaTurbineBlockEntity turbine) {
        this.core = core;
        this.turbine = turbine;
    }

    private static int readLevel(CompoundTag compoundTag, String key) {
        return compoundTag.contains(key) ? clampLevel(compoundTag.getInt(key)) : 0;
    }

    private static int clampLevel(int level) {
        return Mth.clamp(level, 0, MAX_LEVEL);
    }

    public void updateSupplyLevel(int newLevel) {
        if (!setSupplyLevel(newLevel)) {
            return;
        }

        core.markForClientSync();
    }

    public void loadSupplyLevel(int newLevel) {
        setSupplyLevel(newLevel);
    }

    public void loadTypeLevel() {
        setTypeLevel(getGasTypeLevel());
    }

    public Map<LevelKey, Integer> getLevels() {
        int rotorLevel = getRotorLevel();
        int minLevel = Math.min(supplyLevel, Math.min(rotorLevel, typeLevel));
        int maxLevel = Math.max(supplyLevel, Math.max(rotorLevel, typeLevel));
        Map<LevelKey, Integer> levels = new EnumMap<>(LevelKey.class);
        levels.put(LevelKey.SUPPLY, supplyLevel);
        levels.put(LevelKey.ROTOR, rotorLevel);
        levels.put(LevelKey.TYPE, typeLevel);
        levels.put(LevelKey.MIN_VALUE, minLevel);
        levels.put(LevelKey.MAX_VALUE, maxLevel);
        return levels;
    }

    public float getSpeed() {
        return turbine.getGeneratedSpeed();
    }

    public int getCurrentLevel() {
        return Math.min(supplyLevel, Math.min(getRotorLevel(), typeLevel));
    }

    public void reset() {
        boolean changed = supplyLevel != 0 || typeLevel != 0;
        supplyLevel = 0;
        typeLevel = 0;
        if (!changed) {
            return;
        }

        core.markForClientSync();
    }

    public CompoundTag write(boolean clientPacket) {
        CompoundTag compoundTag = new CompoundTag();
        if (!clientPacket) {
            return compoundTag;
        }

        compoundTag.putInt(COMPOUND_KEY_SUPPLY_LEVEL, supplyLevel);
        compoundTag.putInt(COMPOUND_KEY_TYPE_LEVEL, typeLevel);
        return compoundTag;
    }

    public void read(CompoundTag compoundTag, boolean clientPacket) {
        if (!clientPacket) {
            supplyLevel = 0;
            typeLevel = 0;
            return;
        }

        supplyLevel = readLevel(compoundTag, COMPOUND_KEY_SUPPLY_LEVEL);
        typeLevel = readLevel(compoundTag, COMPOUND_KEY_TYPE_LEVEL);
    }

    private int getGasTypeLevel() {
        GasStack gas = core.getFlowMeter().getGasType();
        return gas.isEmpty() ? 0 : AirtightTurbineHandlerUtils.of(gas).getMaxLevel();
    }

    private int getRotorLevel() {
        int rotors = turbine.getBlockState().getValue(TeslaTurbineBlock.ROTOR);
        return clampLevel(rotors * LEVELS_PER_ROTOR);
    }

    private boolean setSupplyLevel(int newLevel) {
        int clampedLevel = clampLevel(newLevel);
        if (supplyLevel == clampedLevel) {
            return false;
        }

        supplyLevel = clampedLevel;
        return true;
    }

    private void setTypeLevel(int newLevel) {
        int clampedLevel = clampLevel(newLevel);
        if (typeLevel == clampedLevel) {
            return;
        }

        typeLevel = clampedLevel;
    }

    public enum LevelKey {
        SUPPLY,
        ROTOR,
        TYPE,
        MIN_VALUE,
        MAX_VALUE
    }
}

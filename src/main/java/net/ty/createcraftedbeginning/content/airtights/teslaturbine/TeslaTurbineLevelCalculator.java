package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

import static net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineCore.MAX_LEVEL;

public class TeslaTurbineLevelCalculator {
    private static final String COMPOUND_KEY_CURRENT_LEVEL = "CurrentLevel";
    private static final String COMPOUND_KEY_MAX_LEVEL_FOR_SUPPLY = "MaxLevelForSupply";
    private static final String COMPOUND_KEY_MAX_LEVEL_FOR_ROTOR = "MaxLevelForRotor";
    private static final String COMPOUND_KEY_MAX_LEVEL_FOR_TYPE = "MaxLevelForType";
    private static final String COMPOUND_KEY_SUPPLY_LEVEL = "SupplyLevel";
    private static final String COMPOUND_KEY_ROTOR_LEVEL = "RotorLevel";
    private static final String COMPOUND_KEY_TYPE_LEVEL = "TypeLevel";
    private static final String COMPOUND_KEY_MAX_VALUE = "MaxValue";
    private static final String COMPOUND_KEY_MIN_VALUE = "MinValue";
    private static final String COMPOUND_KEY_SPEED = "Speed";

    private final TeslaTurbineCore core;
    private final TeslaTurbineBlockEntity turbine;

    private int currentLevel;
    private int maxLevelForSupply;
    private int maxLevelForRotor;
    private int maxLevelForType;
    private int maxValue;
    private int minValue;
    private int supplyLevel;
    private int rotorLevel;
    private int typeLevel;
    private float speed;

    public TeslaTurbineLevelCalculator(@NotNull TeslaTurbineCore core, TeslaTurbineBlockEntity turbine) {
        this.core = core;
        this.turbine = turbine;
    }

    public void updateSupplyLevel(int newLevel) {
        supplyLevel = newLevel;
    }

    public void updateTypeLevel() {
        GasStack gasStack = core.getFlowMeter().getGasType();
        if (gasStack.isEmpty()) {
            typeLevel = 0;
            return;
        }

        typeLevel = (int) gasStack.getGas().getTeslaEfficiency();
    }

    public Map<LevelKey, Integer> getLevels() {
        Map<LevelKey, Integer> levels = new EnumMap<>(LevelKey.class);
        levels.put(LevelKey.SUPPLY, maxLevelForSupply);
        levels.put(LevelKey.ROTOR, maxLevelForRotor);
        levels.put(LevelKey.TYPE, maxLevelForType);
        levels.put(LevelKey.MIN_VALUE, minValue);
        levels.put(LevelKey.MAX_VALUE, maxValue);
        return levels;
    }

    public float getSpeed() {
        return speed;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void reset() {
        currentLevel = 0;
        maxLevelForSupply = 0;
        maxLevelForRotor = 0;
        maxLevelForType = 0;
        maxValue = 0;
        minValue = 0;
        supplyLevel = 0;
        rotorLevel = 0;
        typeLevel = 0;
    }

    public CompoundTag write() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt(COMPOUND_KEY_CURRENT_LEVEL, currentLevel);
        compoundTag.putInt(COMPOUND_KEY_MAX_LEVEL_FOR_SUPPLY, maxLevelForSupply);
        compoundTag.putInt(COMPOUND_KEY_MAX_LEVEL_FOR_ROTOR, maxLevelForRotor);
        compoundTag.putInt(COMPOUND_KEY_MAX_LEVEL_FOR_TYPE, maxLevelForType);
        compoundTag.putInt(COMPOUND_KEY_SUPPLY_LEVEL, supplyLevel);
        compoundTag.putInt(COMPOUND_KEY_ROTOR_LEVEL, rotorLevel);
        compoundTag.putInt(COMPOUND_KEY_TYPE_LEVEL, typeLevel);
        compoundTag.putInt(COMPOUND_KEY_MAX_VALUE, maxValue);
        compoundTag.putInt(COMPOUND_KEY_MIN_VALUE, minValue);
        compoundTag.putFloat(COMPOUND_KEY_SPEED, speed);
        return compoundTag;
    }

    public void read(@NotNull CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_CURRENT_LEVEL)) {
            currentLevel = compoundTag.getInt(COMPOUND_KEY_CURRENT_LEVEL);
        }
        if (compoundTag.contains(COMPOUND_KEY_MAX_LEVEL_FOR_SUPPLY)) {
            maxLevelForSupply = compoundTag.getInt(COMPOUND_KEY_MAX_LEVEL_FOR_SUPPLY);
        }
        if (compoundTag.contains(COMPOUND_KEY_MAX_LEVEL_FOR_ROTOR)) {
            maxLevelForRotor = compoundTag.getInt(COMPOUND_KEY_MAX_LEVEL_FOR_ROTOR);
        }
        if (compoundTag.contains(COMPOUND_KEY_MAX_LEVEL_FOR_TYPE)) {
            maxLevelForType = compoundTag.getInt(COMPOUND_KEY_MAX_LEVEL_FOR_TYPE);
        }
        if (compoundTag.contains(COMPOUND_KEY_SUPPLY_LEVEL)) {
            supplyLevel = compoundTag.getInt(COMPOUND_KEY_SUPPLY_LEVEL);
        }
        if (compoundTag.contains(COMPOUND_KEY_ROTOR_LEVEL)) {
            rotorLevel = compoundTag.getInt(COMPOUND_KEY_ROTOR_LEVEL);
        }
        if (compoundTag.contains(COMPOUND_KEY_TYPE_LEVEL)) {
            typeLevel = compoundTag.getInt(COMPOUND_KEY_TYPE_LEVEL);
        }
        if (compoundTag.contains(COMPOUND_KEY_MAX_VALUE)) {
            maxValue = compoundTag.getInt(COMPOUND_KEY_MAX_VALUE);
        }
        if (compoundTag.contains(COMPOUND_KEY_MIN_VALUE)) {
            minValue = compoundTag.getInt(COMPOUND_KEY_MIN_VALUE);
        }
        if (compoundTag.contains(COMPOUND_KEY_SPEED)) {
            speed = compoundTag.getFloat(COMPOUND_KEY_SPEED);
        }
        update();
    }

    public void update() {
        updateRotorLevel();
        maxLevelForSupply = Math.min(MAX_LEVEL, supplyLevel);
        maxLevelForRotor = Math.min(MAX_LEVEL, rotorLevel);
        maxLevelForType = Math.min(MAX_LEVEL, typeLevel);
        minValue = Math.min(Math.min(maxLevelForRotor, maxLevelForSupply), maxLevelForType);
        maxValue = Math.max(Math.max(maxLevelForRotor, maxLevelForSupply), maxLevelForType);
        currentLevel = minValue;
        if (currentLevel != MAX_LEVEL) {
            return;
        }

        turbine.getAdvancementBehaviour().awardPlayer(CCBAdvancements.MIRACLE_OF_ENGINEERING);
    }

    public void updateRotorLevel() {
        rotorLevel = turbine.getBlockState().getValue(TeslaTurbineBlock.ROTOR) * 2;
        speed = turbine.getGeneratedSpeed();
    }

    public enum LevelKey {
        SUPPLY,
        ROTOR,
        TYPE,
        MIN_VALUE,
        MAX_VALUE
    }
}

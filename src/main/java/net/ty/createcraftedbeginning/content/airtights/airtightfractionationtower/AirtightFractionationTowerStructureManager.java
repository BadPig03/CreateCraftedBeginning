package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.thermoregulator.ThermoregulatorSampling;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightFractionationTowerStructureManager {
    private static final String COMPOUND_KEY_TEMPERATURE = "Temperature";

    private final AirtightFractionationTowerBlockEntity tower;
    private float temperature;

    AirtightFractionationTowerStructureManager(AirtightFractionationTowerBlockEntity tower) {
        this.tower = tower;
    }

    void tick() {
        if (!evaluate()) {
            return;
        }

        tower.notifyUpdate();
    }

    CompoundTag write() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putFloat(COMPOUND_KEY_TEMPERATURE, temperature);
        return compoundTag;
    }

    void read(CompoundTag compoundTag) {
        clear();
        float savedTemperature = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_TEMPERATURE, AirtightThermoregulatorHandler.NONE);
        if (!tower.isTowerController() || !Float.isFinite(savedTemperature)) {
            return;
        }

        temperature = savedTemperature;
    }

    float getTemperature() {
        return temperature;
    }

    AirtightFractionationTowerMode getProcessingMode() {
        return switch (TemperatureCondition.getConditionByTemperature(temperature)) {
            case HEATED, SUPERHEATED -> AirtightFractionationTowerMode.FRACTIONATION;
            case CHILLED, SUPERCHILLED -> AirtightFractionationTowerMode.CONDENSATION;
            case NONE -> AirtightFractionationTowerMode.NONE;
        };
    }

    void clear() {
        temperature = AirtightThermoregulatorHandler.NONE;
    }

    private boolean evaluate() {
        Level level = tower.getLevel();
        BlockPos origin = tower.getOrigin();
        if (level == null || level.isClientSide || origin == null || !tower.isTowerController() || tower.isRemoved()) {
            return false;
        }

        float totalTemperature = ThermoregulatorSampling.calculateTemperature(level, tower.getBlockPos(), origin.below(), AirtightFractionationTowerBlock.WIDTH);
        if (temperature == totalTemperature) {
            return false;
        }

        temperature = totalTemperature;
        return true;
    }
}

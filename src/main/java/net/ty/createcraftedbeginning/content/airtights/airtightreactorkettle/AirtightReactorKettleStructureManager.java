package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandlerUtils;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AirtightReactorKettleStructureManager {
    private static final String COMPOUND_KEY_TEMPERATURE = "Temperature";
    private static final String COMPOUND_KEY_PREVIOUS_TEMPERATURE = "PreviousTemperature";
    private static final String COMPOUND_KEY_PREVIOUS_SPEED = "PreviousSpeed";
    private static final String COMPOUND_KEY_SPEED = "Speed";
    private static final String COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED = "PreviousTheoreticalSpeed";
    private static final String COMPOUND_KEY_THEORETICAL_SPEED = "TheoreticalSpeed";
    private static final String COMPOUND_KEY_PREVIOUS_OVERSTRESSED = "PreviousOverstressed";
    private static final String COMPOUND_KEY_OVERSTRESSED = "Overstressed";

    private final AirtightReactorKettleBlockEntity kettle;
    private float temperature;
    private float previousTemperature;
    private float speed;
    private float previousSpeed;
    private float theoreticalSpeed;
    private float previousTheoreticalSpeed;
    private boolean previousOverstressed;
    private boolean overstressed;

    AirtightReactorKettleStructureManager(AirtightReactorKettleBlockEntity kettle) {
        this.kettle = kettle;
    }

    private static float calculateTemperature(BlockPos corePos, Level level) {
        float totalTemperature = 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos thermoregulatorPos = corePos.offset(x, -2, z);
                BlockState thermoregulatorState = level.getBlockState(thermoregulatorPos);
                AirtightThermoregulatorHandler thermoregulatorHandler = AirtightThermoregulatorHandlerUtils.of(thermoregulatorState.getBlock());
                totalTemperature += thermoregulatorHandler.getHeat(level, thermoregulatorPos, thermoregulatorState);
            }
        }
        return totalTemperature;
    }

    private static float getSpeed(BlockPos corePos, Level level) {
        if (!(level.getBlockEntity(corePos.above()) instanceof AirtightReactorKettleStructuralCogBlockEntity cog)) {
            return 0;
        }
        return cog.getSpeed();
    }

    private static float getTheoreticalSpeed(BlockPos corePos, Level level) {
        float maxTheoreticalSpeed = 0;
        for (Direction direction : Iterate.horizontalDirections) {
            BlockPos cogPos = corePos.above().relative(direction);
            if (!(level.getBlockEntity(cogPos) instanceof AirtightReactorKettleStructuralCogBlockEntity cog)) {
                return 0;
            }

            float candidateSpeed = Mth.abs(cog.getTheoreticalSpeed());
            if (candidateSpeed <= maxTheoreticalSpeed) {
                continue;
            }

            maxTheoreticalSpeed = candidateSpeed;
        }

        return maxTheoreticalSpeed;
    }

    private static boolean isOverstressed(BlockPos corePos, Level level) {
        return level.getBlockEntity(corePos.above()) instanceof AirtightReactorKettleStructuralCogBlockEntity cog && cog.getOverstressed();
    }

    void tick() {
        if (!evaluate()) {
            return;
        }

        kettle.scheduleUpdate();
        kettle.sendData();
    }

    private boolean evaluate() {
        Level level = kettle.getLevel();
        if (level == null) {
            return false;
        }

        BlockPos corePos = kettle.getBlockPos();
        previousTemperature = temperature;
        temperature = calculateTemperature(corePos, level);
        previousSpeed = speed;
        speed = getSpeed(corePos, level);
        previousTheoreticalSpeed = theoreticalSpeed;
        theoreticalSpeed = getTheoreticalSpeed(corePos, level);
        previousOverstressed = overstressed;
        overstressed = isOverstressed(corePos, level);
        return previousTemperature != temperature || previousSpeed != speed || previousTheoreticalSpeed != theoreticalSpeed || previousOverstressed != overstressed;
    }

    CompoundTag write() {
        CompoundTag compoundTag = new CompoundTag();
        CCBNbtUtils.putFloat(compoundTag, COMPOUND_KEY_TEMPERATURE, temperature);
        CCBNbtUtils.putFloat(compoundTag, COMPOUND_KEY_PREVIOUS_TEMPERATURE, previousTemperature);
        CCBNbtUtils.putFloat(compoundTag, COMPOUND_KEY_SPEED, speed);
        CCBNbtUtils.putFloat(compoundTag, COMPOUND_KEY_PREVIOUS_SPEED, previousSpeed);
        CCBNbtUtils.putFloat(compoundTag, COMPOUND_KEY_THEORETICAL_SPEED, theoreticalSpeed);
        CCBNbtUtils.putFloat(compoundTag, COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED, previousTheoreticalSpeed);
        CCBNbtUtils.putBoolean(compoundTag, COMPOUND_KEY_OVERSTRESSED, overstressed);
        CCBNbtUtils.putBoolean(compoundTag, COMPOUND_KEY_PREVIOUS_OVERSTRESSED, previousOverstressed);
        return compoundTag;
    }

    void read(CompoundTag compoundTag) {
        temperature = CCBNbtUtils.getFloatOrDefault(compoundTag, COMPOUND_KEY_TEMPERATURE, temperature);
        previousTemperature = CCBNbtUtils.getFloatOrDefault(compoundTag, COMPOUND_KEY_PREVIOUS_TEMPERATURE, previousTemperature);
        speed = CCBNbtUtils.getFloatOrDefault(compoundTag, COMPOUND_KEY_SPEED, speed);
        previousSpeed = CCBNbtUtils.getFloatOrDefault(compoundTag, COMPOUND_KEY_PREVIOUS_SPEED, previousSpeed);
        theoreticalSpeed = CCBNbtUtils.getFloatOrDefault(compoundTag, COMPOUND_KEY_THEORETICAL_SPEED, theoreticalSpeed);
        previousTheoreticalSpeed = CCBNbtUtils.getFloatOrDefault(compoundTag, COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED, previousTheoreticalSpeed);
        overstressed = CCBNbtUtils.getBooleanOrDefault(compoundTag, COMPOUND_KEY_OVERSTRESSED, overstressed);
        previousOverstressed = CCBNbtUtils.getBooleanOrDefault(compoundTag, COMPOUND_KEY_PREVIOUS_OVERSTRESSED, previousOverstressed);
    }

    float getTemperature() {
        return temperature;
    }

    float getSpeed() {
        return speed;
    }

    float getTheoreticalSpeed() {
        return theoreticalSpeed;
    }

    boolean getOverstressed() {
        return overstressed;
    }
}

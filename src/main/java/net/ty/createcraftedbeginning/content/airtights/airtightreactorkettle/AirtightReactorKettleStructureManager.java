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

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleStructureManager {
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

    public AirtightReactorKettleStructureManager(AirtightReactorKettleBlockEntity kettle) {
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
            if (candidateSpeed > maxTheoreticalSpeed) {
                maxTheoreticalSpeed = candidateSpeed;
            }
        }

        return maxTheoreticalSpeed;
    }

    private static boolean isOverstressed(BlockPos corePos, Level level) {
        return level.getBlockEntity(corePos.above()) instanceof AirtightReactorKettleStructuralCogBlockEntity cog && cog.getOverstressed();
    }

    public void tick() {
        if (!evaluate()) {
            return;
        }

        kettle.scheduleUpdate();
        kettle.sendData();
    }

    public boolean evaluate() {
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

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(COMPOUND_KEY_TEMPERATURE, temperature);
        tag.putFloat(COMPOUND_KEY_PREVIOUS_TEMPERATURE, previousTemperature);
        tag.putFloat(COMPOUND_KEY_SPEED, speed);
        tag.putFloat(COMPOUND_KEY_PREVIOUS_SPEED, previousSpeed);
        tag.putFloat(COMPOUND_KEY_THEORETICAL_SPEED, theoreticalSpeed);
        tag.putFloat(COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED, previousTheoreticalSpeed);
        tag.putBoolean(COMPOUND_KEY_OVERSTRESSED, overstressed);
        tag.putBoolean(COMPOUND_KEY_PREVIOUS_OVERSTRESSED, previousOverstressed);
        return tag;
    }

    public void read(CompoundTag tag) {
        if (tag.contains(COMPOUND_KEY_TEMPERATURE)) {
            temperature = tag.getFloat(COMPOUND_KEY_TEMPERATURE);
        }
        if (tag.contains(COMPOUND_KEY_PREVIOUS_TEMPERATURE)) {
            previousTemperature = tag.getFloat(COMPOUND_KEY_PREVIOUS_TEMPERATURE);
        }
        if (tag.contains(COMPOUND_KEY_SPEED)) {
            speed = tag.getFloat(COMPOUND_KEY_SPEED);
        }
        if (tag.contains(COMPOUND_KEY_PREVIOUS_SPEED)) {
            previousSpeed = tag.getFloat(COMPOUND_KEY_PREVIOUS_SPEED);
        }
        if (tag.contains(COMPOUND_KEY_THEORETICAL_SPEED)) {
            theoreticalSpeed = tag.getFloat(COMPOUND_KEY_THEORETICAL_SPEED);
        }
        if (tag.contains(COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED)) {
            previousTheoreticalSpeed = tag.getFloat(COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED);
        }
        if (tag.contains(COMPOUND_KEY_OVERSTRESSED)) {
            overstressed = tag.getBoolean(COMPOUND_KEY_OVERSTRESSED);
        }
        if (!tag.contains(COMPOUND_KEY_PREVIOUS_OVERSTRESSED)) {
            return;
        }

        previousOverstressed = tag.getBoolean(COMPOUND_KEY_PREVIOUS_OVERSTRESSED);
    }

    public float getTemperature() {
        return temperature;
    }

    public float getSpeed() {
        return speed;
    }

    public float getTheoreticalSpeed() {
        return theoreticalSpeed;
    }

    public boolean getOverstressed() {
        return overstressed;
    }
}

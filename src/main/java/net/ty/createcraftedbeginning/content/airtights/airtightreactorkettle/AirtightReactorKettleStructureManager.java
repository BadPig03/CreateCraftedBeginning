package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.reactorkettle.ReactorKettleThermoregulator;
import org.jetbrains.annotations.NotNull;

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
        float addedTemperature = 0;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos pos = corePos.offset(i, -2, j);
                BlockState state = level.getBlockState(pos);
                ReactorKettleThermoregulator thermoregulator = ReactorKettleThermoregulator.REGISTRY.get(state.getBlock());
                if (thermoregulator == null) {
                    continue;
                }

                addedTemperature += thermoregulator.getHeat(level, pos, state);
            }
        }
        return addedTemperature;
    }

    private static float getSpeed(@NotNull BlockPos corePos, @NotNull Level level) {
        if (!(level.getBlockEntity(corePos.above()) instanceof AirtightReactorKettleStructuralCogBlockEntity cogBlockEntity)) {
            return 0;
        }

        return cogBlockEntity.getSpeed();
    }

    private static float getTheoreticalSpeed(@NotNull BlockPos corePos, @NotNull Level level) {
        float speed = 0;
        for (Direction direction : Iterate.horizontalDirections) {
            if (!(level.getBlockEntity(corePos.above().relative(direction)) instanceof AirtightReactorKettleStructuralCogBlockEntity cogBlockEntity)) {
                return 0;
            }

            float theoreticalSpeed = Mth.abs(cogBlockEntity.getTheoreticalSpeed());
            if (theoreticalSpeed <= speed) {
                continue;
            }

            speed = theoreticalSpeed;
        }

        return speed;
    }

    private static boolean isOverstressed(@NotNull BlockPos corePos, @NotNull Level level) {
        return level.getBlockEntity(corePos.above()) instanceof AirtightReactorKettleStructuralCogBlockEntity cogBlockEntity && cogBlockEntity.getOverstressed();
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
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putFloat(COMPOUND_KEY_TEMPERATURE, temperature);
        compoundTag.putFloat(COMPOUND_KEY_PREVIOUS_TEMPERATURE, previousTemperature);
        compoundTag.putFloat(COMPOUND_KEY_SPEED, speed);
        compoundTag.putFloat(COMPOUND_KEY_PREVIOUS_SPEED, previousSpeed);
        compoundTag.putFloat(COMPOUND_KEY_THEORETICAL_SPEED, theoreticalSpeed);
        compoundTag.putFloat(COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED, previousTheoreticalSpeed);
        compoundTag.putBoolean(COMPOUND_KEY_OVERSTRESSED, overstressed);
        compoundTag.putBoolean(COMPOUND_KEY_PREVIOUS_OVERSTRESSED, previousOverstressed);
        return compoundTag;
    }

    public void read(@NotNull CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_TEMPERATURE)) {
            temperature = compoundTag.getFloat(COMPOUND_KEY_TEMPERATURE);
        }
        if (compoundTag.contains(COMPOUND_KEY_PREVIOUS_TEMPERATURE)) {
            previousTemperature = compoundTag.getFloat(COMPOUND_KEY_PREVIOUS_TEMPERATURE);
        }
        if (compoundTag.contains(COMPOUND_KEY_SPEED)) {
            speed = compoundTag.getFloat(COMPOUND_KEY_SPEED);
        }
        if (compoundTag.contains(COMPOUND_KEY_PREVIOUS_SPEED)) {
            previousSpeed = compoundTag.getFloat(COMPOUND_KEY_PREVIOUS_SPEED);
        }
        if (compoundTag.contains(COMPOUND_KEY_THEORETICAL_SPEED)) {
            theoreticalSpeed = compoundTag.getFloat(COMPOUND_KEY_THEORETICAL_SPEED);
        }
        if (compoundTag.contains(COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED)) {
            previousTheoreticalSpeed = compoundTag.getFloat(COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED);
        }
        if (compoundTag.contains(COMPOUND_KEY_OVERSTRESSED)) {
            overstressed = compoundTag.getBoolean(COMPOUND_KEY_OVERSTRESSED);
        }
        if (compoundTag.contains(COMPOUND_KEY_PREVIOUS_OVERSTRESSED)) {
            previousOverstressed = compoundTag.getBoolean(COMPOUND_KEY_PREVIOUS_OVERSTRESSED);
        }
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

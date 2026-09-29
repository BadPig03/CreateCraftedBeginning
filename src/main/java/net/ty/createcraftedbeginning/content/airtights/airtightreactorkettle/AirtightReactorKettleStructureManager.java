package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.handlers.thermoregulator.ThermoregulatorSampling;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
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

    AirtightReactorKettleStructureManager(AirtightReactorKettleBlockEntity kettle) {
        this.kettle = kettle;
    }

    private static float getSpeed(BlockPos corePos, Level level) {
        if (!(level.getBlockEntity(corePos.above()) instanceof AirtightReactorKettleStructuralCogBlockEntity cog)) {
            return 0;
        }

        return cog.getSpeed();
    }

    private static float getTheoreticalSpeed(BlockPos corePos, Level level) {
        AirtightReactorKettleStructuralCogBlockEntity cog = getKineticTooltipSource(corePos, level);
        if (cog == null) {
            return 0;
        }

        return Mth.abs(cog.getTheoreticalSpeed());
    }

    private static @Nullable AirtightReactorKettleStructuralCogBlockEntity getKineticTooltipSource(BlockPos corePos, Level level) {
        if (!(level.getBlockEntity(corePos.above()) instanceof AirtightReactorKettleStructuralCogBlockEntity cog)) {
            return null;
        }

        return cog;
    }

    private static boolean isOverstressed(BlockPos corePos, Level level) {
        return level.getBlockEntity(corePos.above()) instanceof AirtightReactorKettleStructuralCogBlockEntity cog && cog.getOverstressed();
    }

    @Internal
    public void tick() {
        if (!evaluate()) {
            return;
        }

        kettle.scheduleUpdate();
        kettle.sendData();
    }

    CompoundTag write() {
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

    void read(CompoundTag compoundTag) {
        temperature = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_TEMPERATURE, temperature);
        previousTemperature = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_PREVIOUS_TEMPERATURE, previousTemperature);
        speed = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_SPEED, speed);
        previousSpeed = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_PREVIOUS_SPEED, previousSpeed);
        theoreticalSpeed = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_THEORETICAL_SPEED, theoreticalSpeed);
        previousTheoreticalSpeed = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED, previousTheoreticalSpeed);
        overstressed = NbtValues.getBooleanOrDefault(compoundTag, COMPOUND_KEY_OVERSTRESSED, overstressed);
        previousOverstressed = NbtValues.getBooleanOrDefault(compoundTag, COMPOUND_KEY_PREVIOUS_OVERSTRESSED, previousOverstressed);
    }

    float getTemperature() {
        return temperature;
    }

    float getSpeed() {
        return speed;
    }

    boolean getOverstressed() {
        return overstressed;
    }

    @Nullable AirtightReactorKettleStructuralCogBlockEntity getKineticTooltipSource() {
        Level level = kettle.getLevel();
        if (level == null) {
            return null;
        }

        return getKineticTooltipSource(kettle.getBlockPos(), level);
    }

    private boolean evaluate() {
        Level level = kettle.getLevel();
        if (level == null) {
            return false;
        }

        BlockPos corePos = kettle.getBlockPos();
        previousTemperature = temperature;
        temperature = ThermoregulatorSampling.calculateTemperature(level, corePos, corePos.offset(-1, -2, -1), 3);
        previousSpeed = speed;
        speed = getSpeed(corePos, level);
        previousTheoreticalSpeed = theoreticalSpeed;
        theoreticalSpeed = getTheoreticalSpeed(corePos, level);
        previousOverstressed = overstressed;
        overstressed = isOverstressed(corePos, level);
        return previousTemperature != temperature || previousSpeed != speed || previousTheoreticalSpeed != theoreticalSpeed || previousOverstressed != overstressed;
    }
}

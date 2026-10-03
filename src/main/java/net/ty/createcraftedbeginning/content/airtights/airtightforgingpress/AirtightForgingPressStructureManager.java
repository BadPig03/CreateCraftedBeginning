package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AirtightForgingPressStructureManager {
    private static final String COMPOUND_KEY_PREVIOUS_SPEED = "PreviousSpeed";
    private static final String COMPOUND_KEY_SPEED = "Speed";
    private static final String COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED = "PreviousTheoreticalSpeed";
    private static final String COMPOUND_KEY_THEORETICAL_SPEED = "TheoreticalSpeed";
    private static final String COMPOUND_KEY_PREVIOUS_OVERSTRESSED = "PreviousOverstressed";
    private static final String COMPOUND_KEY_OVERSTRESSED = "Overstressed";

    private final AirtightForgingPressBlockEntity press;
    private float speed;
    private float previousSpeed;
    private float theoreticalSpeed;
    private float previousTheoreticalSpeed;
    private boolean previousOverstressed;
    private boolean overstressed;

    AirtightForgingPressStructureManager(AirtightForgingPressBlockEntity press) {
        this.press = press;
    }

    private static float getSpeed(BlockPos pressPos, Level level) {
        BlockPos shaftPos = pressPos.offset(AirtightForgingPressStructuralPosition.TOP_CENTER.getStructureOffset());
        if (!(level.getBlockEntity(shaftPos) instanceof AirtightForgingPressStructuralShaftBlockEntity shaft)) {
            return 0;
        }

        return shaft.getSpeed();
    }

    private static float getTheoreticalSpeed(BlockPos pressPos, Level level) {
        AirtightForgingPressStructuralShaftBlockEntity shaft = getKineticTooltipSource(pressPos, level);
        if (shaft == null) {
            return 0;
        }

        return Mth.abs(shaft.getTheoreticalSpeed());
    }

    private static @Nullable AirtightForgingPressStructuralShaftBlockEntity getKineticTooltipSource(BlockPos pressPos, Level level) {
        BlockPos shaftPos = pressPos.offset(AirtightForgingPressStructuralPosition.TOP_CENTER.getStructureOffset());
        if (!(level.getBlockEntity(shaftPos) instanceof AirtightForgingPressStructuralShaftBlockEntity shaft)) {
            return null;
        }

        return shaft;
    }

    private static boolean isOverstressed(BlockPos pressPos, Level level) {
        BlockPos shaftPos = pressPos.offset(AirtightForgingPressStructuralPosition.TOP_CENTER.getStructureOffset());
        return level.getBlockEntity(shaftPos) instanceof AirtightForgingPressStructuralShaftBlockEntity shaft && shaft.getOverstressed();
    }

    void tick() {
        if (!evaluate()) {
            return;
        }

        press.scheduleUpdate();
        press.sendData();
    }

    CompoundTag write() {
        CompoundTag structureTag = new CompoundTag();
        structureTag.putFloat(COMPOUND_KEY_SPEED, speed);
        structureTag.putFloat(COMPOUND_KEY_PREVIOUS_SPEED, previousSpeed);
        structureTag.putFloat(COMPOUND_KEY_THEORETICAL_SPEED, theoreticalSpeed);
        structureTag.putFloat(COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED, previousTheoreticalSpeed);
        structureTag.putBoolean(COMPOUND_KEY_OVERSTRESSED, overstressed);
        structureTag.putBoolean(COMPOUND_KEY_PREVIOUS_OVERSTRESSED, previousOverstressed);
        return structureTag;
    }

    void read(CompoundTag compoundTag) {
        speed = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_SPEED, speed);
        previousSpeed = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_PREVIOUS_SPEED, previousSpeed);
        theoreticalSpeed = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_THEORETICAL_SPEED, theoreticalSpeed);
        previousTheoreticalSpeed = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED, previousTheoreticalSpeed);
        overstressed = NbtValues.getBooleanOrDefault(compoundTag, COMPOUND_KEY_OVERSTRESSED, overstressed);
        previousOverstressed = NbtValues.getBooleanOrDefault(compoundTag, COMPOUND_KEY_PREVIOUS_OVERSTRESSED, previousOverstressed);
    }

    float getSpeed() {
        return speed;
    }

    float getRealSpeed() {
        Level level = press.getLevel();
        if (level == null) {
            return 0;
        }

        return getSpeed(press.getBlockPos(), level);
    }

    boolean getOverstressed() {
        return overstressed;
    }

    @Nullable AirtightForgingPressStructuralShaftBlockEntity getKineticTooltipSource() {
        Level level = press.getLevel();
        if (level == null) {
            return null;
        }

        return getKineticTooltipSource(press.getBlockPos(), level);
    }

    private boolean evaluate() {
        Level level = press.getLevel();
        if (level == null) {
            return false;
        }

        BlockPos pressPos = press.getBlockPos();
        previousSpeed = speed;
        speed = getSpeed(pressPos, level);
        previousTheoreticalSpeed = theoreticalSpeed;
        theoreticalSpeed = getTheoreticalSpeed(pressPos, level);
        previousOverstressed = overstressed;
        overstressed = isOverstressed(pressPos, level);

        boolean speedChanged = previousSpeed != speed;
        boolean theoreticalSpeedChanged = previousTheoreticalSpeed != theoreticalSpeed;
        boolean overstressedChanged = previousOverstressed != overstressed;
        return speedChanged || theoreticalSpeedChanged || overstressedChanged;
    }
}

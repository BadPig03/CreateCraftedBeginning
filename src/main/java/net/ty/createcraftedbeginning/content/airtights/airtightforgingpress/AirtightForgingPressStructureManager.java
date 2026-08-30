package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

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
        float maxTheoreticalSpeed = 0;
        for (AirtightForgingPressStructuralPosition structuralPosition : AirtightForgingPressStructuralPosition.all()) {
            if (!structuralPosition.isShaft() || structuralPosition == AirtightForgingPressStructuralPosition.TOP_CENTER) {
                continue;
            }

            BlockPos shaftPos = pressPos.offset(structuralPosition.getStructureOffset());
            if (!(level.getBlockEntity(shaftPos) instanceof AirtightForgingPressStructuralShaftBlockEntity shaft)) {
                return 0;
            }

            maxTheoreticalSpeed = Math.max(maxTheoreticalSpeed, Mth.abs(shaft.getTheoreticalSpeed()));
        }

        return maxTheoreticalSpeed;
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
        CCBNbtUtils.putFloat(structureTag, COMPOUND_KEY_SPEED, speed);
        CCBNbtUtils.putFloat(structureTag, COMPOUND_KEY_PREVIOUS_SPEED, previousSpeed);
        CCBNbtUtils.putFloat(structureTag, COMPOUND_KEY_THEORETICAL_SPEED, theoreticalSpeed);
        CCBNbtUtils.putFloat(structureTag, COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED, previousTheoreticalSpeed);
        CCBNbtUtils.putBoolean(structureTag, COMPOUND_KEY_OVERSTRESSED, overstressed);
        CCBNbtUtils.putBoolean(structureTag, COMPOUND_KEY_PREVIOUS_OVERSTRESSED, previousOverstressed);
        return structureTag;
    }

    void read(CompoundTag compoundTag) {
        speed = CCBNbtUtils.getFloatOrDefault(compoundTag, COMPOUND_KEY_SPEED, speed);
        previousSpeed = CCBNbtUtils.getFloatOrDefault(compoundTag, COMPOUND_KEY_PREVIOUS_SPEED, previousSpeed);
        theoreticalSpeed = CCBNbtUtils.getFloatOrDefault(compoundTag, COMPOUND_KEY_THEORETICAL_SPEED, theoreticalSpeed);
        previousTheoreticalSpeed = CCBNbtUtils.getFloatOrDefault(compoundTag, COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED, previousTheoreticalSpeed);
        overstressed = CCBNbtUtils.getBooleanOrDefault(compoundTag, COMPOUND_KEY_OVERSTRESSED, overstressed);
        previousOverstressed = CCBNbtUtils.getBooleanOrDefault(compoundTag, COMPOUND_KEY_PREVIOUS_OVERSTRESSED, previousOverstressed);
    }

    float getSpeed() {
        return speed;
    }

    float getTheoreticalSpeed() {
        return theoreticalSpeed;
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

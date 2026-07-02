package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressStructureManager {
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

    public AirtightForgingPressStructureManager(AirtightForgingPressBlockEntity press) {
        this.press = press;
    }

    private static float getSpeed(BlockPos corePos, Level level) {
        if (!(level.getBlockEntity(corePos.above()) instanceof AirtightForgingPressStructuralShaftBlockEntity shaftBlockEntity)) {
            return 0;
        }

        return shaftBlockEntity.getSpeed();
    }

    private static float getTheoreticalSpeed(BlockPos corePos, Level level) {
        float speed = 0;
        for (Direction direction : Iterate.horizontalDirections) {
            if (!(level.getBlockEntity(corePos.above().relative(direction)) instanceof AirtightForgingPressStructuralShaftBlockEntity shaftBlockEntity)) {
                return 0;
            }

            float theoreticalSpeed = Mth.abs(shaftBlockEntity.getTheoreticalSpeed());
            if (theoreticalSpeed <= speed) {
                continue;
            }

            speed = theoreticalSpeed;
        }

        return speed;
    }

    private static boolean isOverstressed(BlockPos corePos, Level level) {
        return level.getBlockEntity(corePos.above()) instanceof AirtightForgingPressStructuralShaftBlockEntity shaftBlockEntity && shaftBlockEntity.getOverstressed();
    }

    public void tick() {
        if (!evaluate()) {
            return;
        }

        press.scheduleUpdate();
        press.sendData();
    }

    public boolean evaluate() {
        Level level = press.getLevel();
        if (level == null) {
            return false;
        }

        BlockPos corePos = press.getBlockPos();
        previousSpeed = speed;
        speed = getSpeed(corePos, level);
        previousTheoreticalSpeed = theoreticalSpeed;
        theoreticalSpeed = getTheoreticalSpeed(corePos, level);
        previousOverstressed = overstressed;
        overstressed = isOverstressed(corePos, level);
        return previousSpeed != speed || previousTheoreticalSpeed != theoreticalSpeed || previousOverstressed != overstressed;
    }

    public CompoundTag write() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putFloat(COMPOUND_KEY_SPEED, speed);
        compoundTag.putFloat(COMPOUND_KEY_PREVIOUS_SPEED, previousSpeed);
        compoundTag.putFloat(COMPOUND_KEY_THEORETICAL_SPEED, theoreticalSpeed);
        compoundTag.putFloat(COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED, previousTheoreticalSpeed);
        compoundTag.putBoolean(COMPOUND_KEY_OVERSTRESSED, overstressed);
        compoundTag.putBoolean(COMPOUND_KEY_PREVIOUS_OVERSTRESSED, previousOverstressed);
        return compoundTag;
    }

    public void read(CompoundTag compoundTag) {
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

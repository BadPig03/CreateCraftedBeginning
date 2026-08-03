package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
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

    private static float getSpeed(BlockPos pressPos, Level level) {
        BlockPos shaftPos = pressPos.offset(AirtightForgingPressStructuralPosition.TOP_CENTER.getStructureOffset());
        if (!(level.getBlockEntity(shaftPos) instanceof AirtightForgingPressStructuralShaftBlockEntity shaft)) {
            return 0;
        }
        return shaft.getSpeed();
    }

    private static float getTheoreticalSpeed(BlockPos pressPos, Level level) {
        float maxSpeed = 0;
        for (AirtightForgingPressStructuralPosition position : AirtightForgingPressStructuralPosition.all()) {
            if (!position.isShaft() || position == AirtightForgingPressStructuralPosition.TOP_CENTER) {
                continue;
            }

            BlockPos shaftPos = pressPos.offset(position.getStructureOffset());
            if (!(level.getBlockEntity(shaftPos) instanceof AirtightForgingPressStructuralShaftBlockEntity shaft)) {
                return 0;
            }

            maxSpeed = Math.max(maxSpeed, Mth.abs(shaft.getTheoreticalSpeed()));
        }

        return maxSpeed;
    }

    private static boolean isOverstressed(BlockPos pressPos, Level level) {
        BlockPos shaftPos = pressPos.offset(AirtightForgingPressStructuralPosition.TOP_CENTER.getStructureOffset());
        return level.getBlockEntity(shaftPos) instanceof AirtightForgingPressStructuralShaftBlockEntity shaft && shaft.getOverstressed();
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

        BlockPos pressPos = press.getBlockPos();
        previousSpeed = speed;
        speed = getSpeed(pressPos, level);
        previousTheoreticalSpeed = theoreticalSpeed;
        theoreticalSpeed = getTheoreticalSpeed(pressPos, level);
        previousOverstressed = overstressed;
        overstressed = isOverstressed(pressPos, level);

        boolean speedChanged = previousSpeed != speed;
        boolean theoreticalSpeedChanged = previousTheoreticalSpeed != theoreticalSpeed;
        boolean stressChanged = previousOverstressed != overstressed;
        return speedChanged || theoreticalSpeedChanged || stressChanged;
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat(COMPOUND_KEY_SPEED, speed);
        tag.putFloat(COMPOUND_KEY_PREVIOUS_SPEED, previousSpeed);
        tag.putFloat(COMPOUND_KEY_THEORETICAL_SPEED, theoreticalSpeed);
        tag.putFloat(COMPOUND_KEY_PREVIOUS_THEORETICAL_SPEED, previousTheoreticalSpeed);
        tag.putBoolean(COMPOUND_KEY_OVERSTRESSED, overstressed);
        tag.putBoolean(COMPOUND_KEY_PREVIOUS_OVERSTRESSED, previousOverstressed);
        return tag;
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
        if (!compoundTag.contains(COMPOUND_KEY_PREVIOUS_OVERSTRESSED)) {
            return;
        }

        previousOverstressed = compoundTag.getBoolean(COMPOUND_KEY_PREVIOUS_OVERSTRESSED);
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

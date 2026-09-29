package net.ty.createcraftedbeginning.content.airtights.airvents;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock.VentState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.UnaryOperator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirVentLouverState {
    private static final String COMPOUND_KEY_LOUVER_MASK = "LouverMask";
    private static final String COMPOUND_KEY_OPENED_MASK = "OpenedMask";
    private static final int VALID_DIRECTION_MASK = (1 << Direction.values().length) - 1;

    private int louverMask;
    private int openedMask;

    static void transformNbt(CompoundTag tag, UnaryOperator<BlockPos> transform) {
        AirVentLouverState original = new AirVentLouverState();
        original.load(tag);
        AirVentLouverState transformed = new AirVentLouverState();
        BlockPos origin = transform.apply(BlockPos.ZERO);
        for (Direction direction : Iterate.directions) {
            BlockPos offset = transform.apply(BlockPos.ZERO.relative(direction)).subtract(origin);
            Direction target = Direction.getNearest(offset.getX(), offset.getY(), offset.getZ());
            transformed.setLouverState(target, original.getLouverState(direction));
        }
        transformed.save(tag);
    }

    private static int directionMask(Direction direction) {
        return 1 << direction.get3DDataValue();
    }

    void load(CompoundTag compoundTag) {
        louverMask = compoundTag.getInt(COMPOUND_KEY_LOUVER_MASK) & VALID_DIRECTION_MASK;
        openedMask = compoundTag.getInt(COMPOUND_KEY_OPENED_MASK) & louverMask;
    }

    void save(CompoundTag compoundTag) {
        compoundTag.putInt(COMPOUND_KEY_LOUVER_MASK, louverMask);
        compoundTag.putInt(COMPOUND_KEY_OPENED_MASK, openedMask);
    }

    VentState getLouverState(Direction direction) {
        if (!hasLouver(direction)) {
            return VentState.EMPTY;
        }

        if (!isLouverOpen(direction)) {
            return VentState.CLOSED;
        }

        return VentState.OPENED;
    }

    boolean hasLouver(Direction direction) {
        return (louverMask & directionMask(direction)) != 0;
    }

    boolean isLouverOpen(Direction direction) {
        int directionBit = directionMask(direction);
        return (louverMask & directionBit) != 0 && (openedMask & directionBit) != 0;
    }

    int getVisibleLouverMask(int connectionMask) {
        if (louverMask == 0) {
            return 0;
        }

        return louverMask & ~connectionMask & VALID_DIRECTION_MASK;
    }

    int getOpenedLouverMask() {
        return openedMask;
    }

    boolean toggleLouver(Direction direction) {
        if (hasLouver(direction)) {
            return setLouverState(direction, VentState.EMPTY);
        }

        return setLouverState(direction, VentState.CLOSED);
    }

    boolean toggleLouverOpen(Direction direction) {
        if (!hasLouver(direction)) {
            return false;
        }

        if (isLouverOpen(direction)) {
            return setLouverState(direction, VentState.CLOSED);
        }

        return setLouverState(direction, VentState.OPENED);
    }

    boolean setLouverState(Direction direction, VentState louverState) {
        int directionBit = directionMask(direction);
        int nextLouverMask = louverMask;
        int nextOpenedMask = openedMask;
        switch (louverState) {
            case EMPTY -> {
                nextLouverMask &= ~directionBit;
                nextOpenedMask &= ~directionBit;
            }
            case CLOSED -> {
                nextLouverMask |= directionBit;
                nextOpenedMask &= ~directionBit;
            }
            case OPENED -> {
                nextLouverMask |= directionBit;
                nextOpenedMask |= directionBit;
            }
            case CONNECTED -> {
                return false;
            }
        }

        if (nextLouverMask == louverMask && nextOpenedMask == openedMask) {
            return false;
        }

        louverMask = nextLouverMask;
        openedMask = nextOpenedMask;
        return true;
    }
}

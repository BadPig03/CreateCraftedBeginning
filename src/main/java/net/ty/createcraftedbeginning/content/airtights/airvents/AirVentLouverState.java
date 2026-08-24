package net.ty.createcraftedbeginning.content.airtights.airvents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock.VentState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirVentLouverState {
    private static final String COMPOUND_KEY_LOUVER_MASK = "LouverMask";
    private static final String COMPOUND_KEY_OPENED_MASK = "OpenedMask";
    private static final int VALID_DIRECTION_MASK = (1 << Direction.values().length) - 1;

    private int louverMask;
    private int openedMask;

    private static int directionMask(Direction direction) {
        return 1 << direction.get3DDataValue();
    }

    void load(CompoundTag tag) {
        louverMask = tag.getInt(COMPOUND_KEY_LOUVER_MASK) & VALID_DIRECTION_MASK;
        openedMask = tag.getInt(COMPOUND_KEY_OPENED_MASK) & louverMask;
    }

    void save(CompoundTag tag) {
        tag.putInt(COMPOUND_KEY_LOUVER_MASK, louverMask);
        tag.putInt(COMPOUND_KEY_OPENED_MASK, openedMask);
    }

    VentState getLouverState(Direction direction) {
        if (!hasLouver(direction)) {
            return VentState.EMPTY;
        }
        return isLouverOpen(direction) ? VentState.OPENED : VentState.CLOSED;
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
        return setLouverState(direction, hasLouver(direction) ? VentState.EMPTY : VentState.CLOSED);
    }

    boolean toggleLouverOpen(Direction direction) {
        return hasLouver(direction) && setLouverState(direction, isLouverOpen(direction) ? VentState.CLOSED : VentState.OPENED);
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

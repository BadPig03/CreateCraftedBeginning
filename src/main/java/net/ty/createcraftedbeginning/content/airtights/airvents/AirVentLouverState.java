package net.ty.createcraftedbeginning.content.airtights.airvents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock.VentState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirVentLouverState {
    private static final String COMPOUND_KEY_LOUVER_MASK = "LouverMask";
    private static final String COMPOUND_KEY_OPENED_MASK = "OpenedMask";
    private static final int VALID_DIRECTION_MASK = (1 << Direction.values().length) - 1;

    private int louverMask;
    private int openedMask;

    private static int directionMask(Direction direction) {
        return 1 << direction.get3DDataValue();
    }

    public void load(CompoundTag tag) {
        louverMask = tag.getInt(COMPOUND_KEY_LOUVER_MASK) & VALID_DIRECTION_MASK;
        openedMask = tag.getInt(COMPOUND_KEY_OPENED_MASK) & louverMask;
    }

    public void save(CompoundTag tag) {
        tag.putInt(COMPOUND_KEY_LOUVER_MASK, louverMask);
        tag.putInt(COMPOUND_KEY_OPENED_MASK, openedMask);
    }

    public VentState getLouverState(Direction direction) {
        if (!hasLouver(direction)) {
            return VentState.EMPTY;
        }
        return isLouverOpen(direction) ? VentState.OPENED : VentState.CLOSED;
    }

    public boolean hasLouver(Direction direction) {
        return (louverMask & directionMask(direction)) != 0;
    }

    public boolean isLouverOpen(Direction direction) {
        int directionBit = directionMask(direction);
        return (louverMask & directionBit) != 0 && (openedMask & directionBit) != 0;
    }

    public int getVisibleLouverMask(int connectionMask) {
        if (louverMask == 0) {
            return 0;
        }
        return louverMask & ~connectionMask & VALID_DIRECTION_MASK;
    }

    public int getOpenedLouverMask() {
        return openedMask;
    }

    public boolean toggleLouver(Direction direction) {
        return setLouverState(direction, hasLouver(direction) ? VentState.EMPTY : VentState.CLOSED);
    }

    public boolean toggleLouverOpen(Direction direction) {
        return hasLouver(direction) && setLouverState(direction, isLouverOpen(direction) ? VentState.CLOSED : VentState.OPENED);
    }

    public boolean setLouverState(Direction direction, VentState louverState) {
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

package net.ty.createcraftedbeginning.content.airtights.airvents;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentBlock.VentState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirVentBlockEntity extends SmartBlockEntity {
    private static final String COMPOUND_KEY_LOUVER_MASK = "LouverMask";
    private static final String COMPOUND_KEY_OPENED_MASK = "OpenedMask";
    private static final int VALID_DIRECTION_MASK = (1 << Direction.values().length) - 1;

    private int louverMask;
    private int openedMask;

    public AirVentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private static int directionMask(Direction direction) {
        return 1 << direction.get3DDataValue();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = getBlockState();
        BlockState connectedState = AirVentBlock.withConnections(state, level, worldPosition);
        if (connectedState == state) {
            return;
        }

        level.setBlockAndUpdate(worldPosition, connectedState);
    }

    @Override
    protected void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.write(tag, provider, clientPacket);
        tag.putInt(COMPOUND_KEY_LOUVER_MASK, louverMask);
        tag.putInt(COMPOUND_KEY_OPENED_MASK, openedMask);
    }

    @Override
    protected void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.read(tag, provider, clientPacket);
        louverMask = tag.getInt(COMPOUND_KEY_LOUVER_MASK) & VALID_DIRECTION_MASK;
        openedMask = tag.getInt(COMPOUND_KEY_OPENED_MASK) & louverMask;
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
        int mask = directionMask(direction);
        return (louverMask & mask) != 0 && (openedMask & mask) != 0;
    }

    public int getVisibleLouverMask() {
        return louverMask & ~AirVentBlock.getConnectionMask(getBlockState()) & VALID_DIRECTION_MASK;
    }

    public int getOpenedLouverMask() {
        return openedMask;
    }

    public void toggleLouver(Direction direction) {
        setLouverState(direction, hasLouver(direction) ? VentState.EMPTY : VentState.CLOSED);
    }

    public void toggleLouverOpen(Direction direction) {
        if (!hasLouver(direction)) {
            return;
        }

        setLouverState(direction, isLouverOpen(direction) ? VentState.CLOSED : VentState.OPENED);
    }

    public void setLouverState(Direction direction, VentState state) {
        int mask = directionMask(direction);
        int nextLouverMask = louverMask;
        int nextOpenedMask = openedMask;
        switch (state) {
            case EMPTY -> {
                nextLouverMask &= ~mask;
                nextOpenedMask &= ~mask;
            }
            case CLOSED -> {
                nextLouverMask |= mask;
                nextOpenedMask &= ~mask;
            }
            case OPENED -> {
                nextLouverMask |= mask;
                nextOpenedMask |= mask;
            }
            case CONNECTED -> {
                return;
            }
        }

        if (nextLouverMask == louverMask && nextOpenedMask == openedMask) {
            return;
        }

        louverMask = nextLouverMask;
        openedMask = nextOpenedMask;
        notifyUpdate();
    }
}

package net.ty.createcraftedbeginning.gas.network;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasConnectionResolver {
    private GasConnectionResolver() {
    }

    @Nullable
    public static GasTransportBehaviour getTransportBehaviour(BlockGetter level, BlockPos pos) {
        if (level instanceof Level world && !world.isLoaded(pos)) {
            return null;
        }

        return BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
    }

    public static AdjacentConnection resolveAdjacentConnection(Level level, BlockPos pos, Direction side) {
        return new AdjacentConnection(level, pos, side);
    }

    public static @Nullable Direction getChangedNeighborFace(Level level, BlockPos pos, BlockPos neighborPos) {
        if (level.isClientSide || !level.isLoaded(neighborPos)) {
            return null;
        }

        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof GasConnectable connectable && !connectable.propagatesGasNeighborUpdates()) {
            return null;
        }

        for (Direction direction : Iterate.directions) {
            if (!pos.relative(direction).equals(neighborPos)) {
                continue;
            }

            return direction;
        }

        return null;
    }

    public static final class AdjacentConnection {
        private static final byte UNKNOWN = -1;
        private static final byte FALSE = 0;
        private static final byte TRUE = 1;

        private final Level level;
        private final BlockPos pos;
        private final BlockState state;
        private final boolean loaded;
        private final Direction connectedFace;
        @Nullable
        private GasTransportBehaviour transportBehaviour;
        private boolean transportBehaviourResolved;
        private byte transportConnection = UNKNOWN;
        private byte gasHandler = UNKNOWN;

        private AdjacentConnection(Level level, BlockPos pipePos, Direction side) {
            this.level = level;
            pos = pipePos.relative(side);
            loaded = level.isLoaded(pos);
            state = loaded ? level.getBlockState(pos) : Blocks.VOID_AIR.defaultBlockState();
            connectedFace = side.getOpposite();
        }

        public BlockPos pos() {
            return pos;
        }

        @Nullable
        public GasTransportBehaviour behaviour() {
            if (!loaded) {
                return null;
            }

            if (!transportBehaviourResolved) {
                transportBehaviour = getTransportBehaviour(level, pos);
                transportBehaviourResolved = true;
            }
            return transportBehaviour;
        }

        public boolean hasTransportConnection() {
            if (transportConnection == UNKNOWN) {
                GasTransportBehaviour adjacentBehaviour = behaviour();
                transportConnection = adjacentBehaviour != null && adjacentBehaviour.canConnectOnFace(state, connectedFace) ? TRUE : FALSE;
            }
            return transportConnection == TRUE;
        }

        public boolean hasGasHandler() {
            if (!loaded) {
                return false;
            }

            if (gasHandler == UNKNOWN) {
                gasHandler = GasCapabilities.hasBlockHandler(level, pos, connectedFace) ? TRUE : FALSE;
            }
            return gasHandler == TRUE;
        }

        public boolean isAtmospheric() {
            return loaded && (!(state.getBlock() instanceof GasConnectable connectable) || !connectable.blocksAtmosphere(pos, state, connectedFace)) && !hasTransportConnection() && !hasGasHandler() && (CCBBlockTags.GAS_SOURCES.matches(state) || state.canBeReplaced() && state.getDestroySpeed(level, pos) != -1 && (!BlockHelper.hasBlockSolidSide(state, level, pos, connectedFace) || AllBlockTags.FAN_TRANSPARENT.matches(state)));
        }
    }
}

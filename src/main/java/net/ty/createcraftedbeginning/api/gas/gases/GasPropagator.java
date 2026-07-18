package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasPipeConnection.AirFlow;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBBlockTags;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPropagator {
    private static final WorldAttached<PropagationBatch> CHANGED_PIPE_BATCHES = new WorldAttached<>($ -> new PropagationBatch());

    private GasPropagator() {
    }

    /**
     * Propagates the pipe update through connected components.
     *
     * @param level     the level in which the operation is performed
     * @param pipePos   the position of the connected gas pipe
     * @param pipeState the block state of the connected gas pipe
     */
    public static void propagatePipe(Level level, BlockPos pipePos, BlockState pipeState) {
        propagatePipeInternal(level, pipePos, pipeState);
    }

    /**
     * Propagates the changed pipe update through connected components.
     *
     * @param level     the level in which the operation is performed
     * @param pipePos   the position of the connected gas pipe
     * @param pipeState the block state of the connected gas pipe
     */
    public static void propagateChangedPipe(Level level, BlockPos pipePos, BlockState pipeState) {
        long gameTime = level.getGameTime();
        PropagationBatch batch = CHANGED_PIPE_BATCHES.get(level);
        if (batch.gameTime != gameTime) {
            batch.gameTime = gameTime;
            batch.processed.clear();
        }
        if (batch.processed.contains(pipePos)) {
            return;
        }

        batch.processed.addAll(propagatePipeInternal(level, pipePos, pipeState));
    }

    private static Set<BlockPos> propagatePipeInternal(Level level, BlockPos pipePos, BlockState pipeState) {
        Deque<Pair<Integer, BlockPos>> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<Pair<AirtightPumpBlockEntity, Direction>> discoveredPumps = new HashSet<>();
        frontier.add(Pair.of(0, pipePos));
        visited.add(pipePos);
        int pumpRange = getAirtightPumpMaxRange();
        while (!frontier.isEmpty()) {
            Pair<Integer, BlockPos> pair = frontier.poll();
            BlockPos currentPos = pair.getSecond();
            GasTransportBehaviour behaviour = getBehaviour(level, currentPos);
            if (behaviour == null) {
                continue;
            }

            behaviour.wipePressure();
            BlockState currentState = currentPos.equals(pipePos) ? pipeState : level.getBlockState(currentPos);
            int distance = pair.getFirst();
            for (Direction direction : Iterate.directions) {
                if (!behaviour.canHaveFlowToward(currentState, direction)) {
                    continue;
                }

                BlockPos targetPos = currentPos.relative(direction);
                if (!level.isLoaded(targetPos) || visited.contains(targetPos)) {
                    continue;
                }

                BlockState targetState = level.getBlockState(targetPos);
                if (targetState.getBlock() instanceof AirtightPumpBlock) {
                    Axis pumpAxis = targetState.getValue(AirtightPumpBlock.FACING).getAxis();
                    if (pumpAxis == direction.getAxis() && level.getBlockEntity(targetPos) instanceof AirtightPumpBlockEntity pump) {
                        discoveredPumps.add(Pair.of(pump, direction.getOpposite()));
                    }
                    continue;
                }

                GasTransportBehaviour targetBehaviour = getBehaviour(level, targetPos);
                if (targetBehaviour == null) {
                    continue;
                }

                int newDistance = distance + 1;
                if (newDistance > pumpRange && !targetBehaviour.hasAnyPressure()) {
                    continue;
                }
                if (!targetBehaviour.canHaveFlowToward(targetState, direction.getOpposite())) {
                    continue;
                }

                visited.add(targetPos);
                frontier.add(Pair.of(newDistance, targetPos));
            }
        }

        for (Pair<AirtightPumpBlockEntity, Direction> pump : discoveredPumps) {
            visited.add(pump.getFirst().getBlockPos());
            pump.getFirst().updatePipesOnSide(pump.getSecond());
        }
        return visited;
    }

    /**
     * Returns the behaviour.
     *
     * @param level the level in which the operation is performed
     * @param pos   the target block position
     * @return the behaviour
     */
    @Nullable
    public static GasTransportBehaviour getBehaviour(BlockGetter level, BlockPos pos) {
        return BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
    }

    /**
     * Returns the airtight pump max range.
     *
     * @return the airtight pump max range
     */
    public static int getAirtightPumpMaxRange() {
        return CCBConfig.server().airtights.maxPumpRange.get();
    }

    /**
     * Resets the affected networks.
     *
     * @param level the level in which the operation is performed
     * @param start the starting position or value
     * @param side  the side from which the target is accessed
     */
    public static void resetAffectedNetworks(Level level, BlockPos start, Direction side) {
        Deque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        frontier.add(start);
        visited.add(start);
        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.poll();
            GasTransportBehaviour behaviour = getBehaviour(level, pos);
            if (behaviour == null) {
                continue;
            }

            if (pos.equals(start)) {
                resetNetworkInDirection(level, pos, behaviour, side, frontier, visited);
                continue;
            }

            for (Direction direction : Iterate.directions) {
                resetNetworkInDirection(level, pos, behaviour, direction, frontier, visited);
            }
        }
    }

    /**
     * Returns the changed neighbour side.
     *
     * @param level       the level in which the operation is performed
     * @param pos         the target block position
     * @param neighborPos the position of the neighboring block
     * @return the changed neighbour side
     */
    public static @Nullable Direction getChangedNeighbourSide(Level level, BlockPos pos, BlockPos neighborPos) {
        if (level.isClientSide) {
            return null;
        }

        Block otherBlock = level.getBlockState(neighborPos).getBlock();
        if (otherBlock instanceof AirtightPumpBlock) {
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

    /**
     * Checks whether this value is open ended.
     *
     * @param level the level in which the operation is performed
     * @param pos   the target block position
     * @param side  the side from which the target is accessed
     * @return {@code true} if this value is open ended; otherwise {@code false}
     */
    public static boolean isOpenEnded(Level level, BlockPos pos, Direction side) {
        BlockPos targetPos = pos.relative(side);
        BlockState targetState = level.getBlockState(targetPos);
        Direction oppositeDir = side.getOpposite();
        GasTransportBehaviour behaviour = getBehaviour(level, targetPos);
        boolean canFlowToward = behaviour != null && behaviour.canHaveFlowToward(targetState, oppositeDir);
        boolean isPump = targetState.getBlock() instanceof AirtightPumpBlock && targetState.getValue(AirtightPumpBlock.FACING).getAxis() == side.getAxis();
        boolean hasGasCapability = GasCapabilities.hasGasCapability(level, targetPos, oppositeDir);
        boolean isGasSource = CCBBlockTags.GAS_SOURCES.matches(targetState);
        boolean isFaceSolid = BlockHelper.hasBlockSolidSide(targetState, level, targetPos, oppositeDir) && !AllBlockTags.FAN_TRANSPARENT.matches(targetState);
        boolean canBeReplaced = targetState.canBeReplaced() && targetState.getDestroySpeed(level, targetPos) != -1;
        return !canFlowToward && !isPump && !hasGasCapability && (isGasSource || !isFaceSolid && canBeReplaced);
    }

    private static void resetNetworkInDirection(Level level, BlockPos pos, GasTransportBehaviour behaviour, Direction direction, Deque<BlockPos> frontier, Set<BlockPos> visited) {
        BlockPos targetPos = pos.relative(direction);
        if (!level.isLoaded(targetPos) || visited.contains(targetPos)) {
            return;
        }

        GasPipeConnection connection = behaviour.getConnection(direction);
        if (connection == null) {
            return;
        }

        AirFlow flow = connection.getFlow();
        if (flow == null || !flow.inbound) {
            return;
        }

        connection.resetNetwork();
        frontier.add(targetPos);
        visited.add(targetPos);
    }

    private static final class PropagationBatch {
        private final Set<BlockPos> processed = new HashSet<>();
        private long gameTime = Long.MIN_VALUE;
    }
}

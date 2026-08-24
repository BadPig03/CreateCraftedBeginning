package net.ty.createcraftedbeginning.content.airtights.gas.transport;

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
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPipeConnection.GasFlow;
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
    private static final WorldAttached<PressureTopologyRevision> PRESSURE_TOPOLOGY_REVISIONS = new WorldAttached<>($ -> new PressureTopologyRevision());

    private GasPropagator() {
    }

    public static void propagatePipe(Level level, BlockPos pipePos) {
        propagatePipeInternal(level, pipePos);
    }

    public static void propagateChangedPipe(Level level, BlockPos pipePos) {
        long gameTime = level.getGameTime();
        PropagationBatch propagationBatch = CHANGED_PIPE_BATCHES.get(level);
        if (propagationBatch.gameTime != gameTime) {
            propagationBatch.gameTime = gameTime;
            propagationBatch.processed.clear();
        }
        if (propagationBatch.processed.contains(pipePos)) {
            return;
        }

        propagationBatch.processed.addAll(propagatePipeInternal(level, pipePos));
    }

    @Nullable
    public static GasTransportBehaviour getBehaviour(BlockGetter level, BlockPos pos) {
        return BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
    }

    public static AdjacentTarget resolveAdjacentTarget(Level level, BlockPos pos, Direction side) {
        return new AdjacentTarget(level, pos, side);
    }

    public static int getAirtightPumpMaxRange() {
        return CCBConfig.server().airtights.maxPumpRange.get();
    }

    public static long getPressureTopologyRevision(Level level) {
        return PRESSURE_TOPOLOGY_REVISIONS.get(level).revision;
    }

    public static void invalidatePressureTopology(Level level) {
        PRESSURE_TOPOLOGY_REVISIONS.get(level).revision++;
    }

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

    public static @Nullable Direction getChangedNeighbourSide(Level level, BlockPos pos, BlockPos neighborPos) {
        if (level.isClientSide) {
            return null;
        }

        if (level.getBlockState(neighborPos).getBlock() instanceof AirtightPumpBlock) {
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

    private static Set<BlockPos> propagatePipeInternal(Level level, BlockPos pipePos) {
        Deque<Pair<Integer, BlockPos>> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<Pair<AirtightPumpBlockEntity, Direction>> discoveredPumps = new HashSet<>();
        frontier.add(Pair.of(0, pipePos));
        visited.add(pipePos);
        int pumpRange = getAirtightPumpMaxRange();
        while (!frontier.isEmpty()) {
            Pair<Integer, BlockPos> frontierEntry = frontier.poll();
            BlockPos currentPos = frontierEntry.getSecond();
            GasTransportBehaviour behaviour = getBehaviour(level, currentPos);
            if (behaviour == null) {
                continue;
            }

            behaviour.wipePressure();
            int distance = frontierEntry.getFirst();
            for (Direction direction : Iterate.directions) {
                if (behaviour.getConnection(direction) == null) {
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

                int targetDistance = distance + 1;
                if (targetDistance > pumpRange && !targetBehaviour.hasAnyPressureContribution()) {
                    continue;
                }

                if (targetBehaviour.getConnection(direction.getOpposite()) == null) {
                    continue;
                }

                visited.add(targetPos);
                frontier.add(Pair.of(targetDistance, targetPos));
            }
        }

        for (Pair<AirtightPumpBlockEntity, Direction> discoveredPump : discoveredPumps) {
            visited.add(discoveredPump.getFirst().getBlockPos());
            discoveredPump.getFirst().updatePipesOnSide(discoveredPump.getSecond());
        }
        return visited;
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

        GasFlow flow = connection.getFlow();
        if (flow == null || !flow.inbound) {
            return;
        }

        connection.resetNetwork();
        frontier.add(targetPos);
        visited.add(targetPos);
    }

    public static final class AdjacentTarget {
        private static final byte UNKNOWN = -1;
        private static final byte FALSE = 0;
        private static final byte TRUE = 1;

        private final Level level;
        private final Direction side;
        private final BlockPos pos;
        private final BlockState state;
        private final Direction connectedFace;
        @Nullable
        private GasTransportBehaviour behaviour;
        private boolean isBehaviourResolved;
        private byte canFlowToward = UNKNOWN;
        private byte gasCapability = UNKNOWN;

        private AdjacentTarget(Level level, BlockPos sourcePos, Direction side) {
            this.level = level;
            this.side = side;
            pos = sourcePos.relative(side);
            state = level.getBlockState(pos);
            connectedFace = side.getOpposite();
        }

        public BlockPos pos() {
            return pos;
        }

        public BlockState state() {
            return state;
        }

        public Direction connectedFace() {
            return connectedFace;
        }

        @Nullable
        public GasTransportBehaviour behaviour() {
            if (!isBehaviourResolved) {
                behaviour = getBehaviour(level, pos);
                isBehaviourResolved = true;
            }
            return behaviour;
        }

        public boolean canFlowToward() {
            if (canFlowToward == UNKNOWN) {
                GasTransportBehaviour targetBehaviour = behaviour();
                canFlowToward = targetBehaviour != null && targetBehaviour.canHaveFlowToward(state, connectedFace) ? TRUE : FALSE;
            }
            return canFlowToward == TRUE;
        }

        public boolean isAlignedPump() {
            return state.getBlock() instanceof AirtightPumpBlock && state.getValue(AirtightPumpBlock.FACING).getAxis() == side.getAxis();
        }

        public boolean hasGasCapability() {
            if (gasCapability == UNKNOWN) {
                gasCapability = GasCapabilities.hasGasCapability(level, pos, connectedFace) ? TRUE : FALSE;
            }
            return gasCapability == TRUE;
        }

        public boolean isOpenEnded() {
            return !isAlignedPump() && !canFlowToward() && !hasGasCapability() && (CCBBlockTags.GAS_SOURCES.matches(state) || state.canBeReplaced() && state.getDestroySpeed(level, pos) != -1 && (!BlockHelper.hasBlockSolidSide(state, level, pos, connectedFace) || AllBlockTags.FAN_TRANSPARENT.matches(state)));
        }
    }

    private static final class PropagationBatch {
        private final Set<BlockPos> processed = new HashSet<>();
        private long gameTime = Long.MIN_VALUE;
    }

    private static final class PressureTopologyRevision {
        private long revision;
    }
}

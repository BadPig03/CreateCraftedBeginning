package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.GasPipeConnection;
import net.ty.createcraftedbeginning.api.gas.gases.GasPropagator;
import net.ty.createcraftedbeginning.api.gas.gases.GasPropagator.AdjacentTarget;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTransporter;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachmentTypes.AttachmentTypes;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.IAirtightPipeDrain;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPumpBlockEntity extends KineticBlockEntity implements IGasTransporter {
    private static final int LAZY_TICK_RATE = 10;
    private static final int RECOVERY_INITIAL_BACKOFF = 20;
    private static final int RECOVERY_MAX_BACKOFF = 640;
    private static final float MIN_PUMP_SPEED = SpeedLevel.MEDIUM.getSpeedValue();
    private static final int DIRECTION_COUNT = Direction.values().length;

    private final Couple<MutableBoolean> sidesToUpdate;
    private final Couple<MutableBoolean> recoveryAttempts;
    private final Couple<RecoveryState> recoveryStates;
    private boolean pressureUpdate;
    private CCBAdvancementBehaviour advancementBehaviour;
    private GasPumpTransportBehaviour transportBehaviour;

    private boolean lazyStateInitialized;
    private float lastLazyAbsSpeed;
    private Direction lastLazyFacing;

    public AirtightPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        sidesToUpdate = Couple.create(MutableBoolean::new);
        recoveryAttempts = Couple.create(MutableBoolean::new);
        recoveryStates = Couple.create(RecoveryState::new);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    private static boolean hasReachedValidEndpoint(Level level, AdjacentTarget target, boolean pull) {
        if (target.isAlignedPump() && level.getBlockEntity(target.pos()) instanceof AirtightPumpBlockEntity pump) {
            return pump.isPumpRunning() && isPullingOnSide(pump.isFront(target.connectedFace())) != pull;
        }
        return !target.canFlowToward() && (target.hasGasCapability() || target.isOpenEnded());
    }

    private static boolean isPullingOnSide(boolean isFront) {
        return !isFront;
    }

    private static boolean isSideAccessible(BlockState state, Direction direction) {
        return state.getBlock() instanceof AirtightPumpBlock && state.getValue(AirtightPumpBlock.FACING).getAxis() == direction.getAxis();
    }

    private static boolean isFront(BlockState state, Direction direction) {
        return state.getBlock() instanceof AirtightPumpBlock && direction == state.getValue(AirtightPumpBlock.FACING);
    }

    private static boolean hasRequiredSpeed(float speed) {
        return Mth.abs(speed) >= MIN_PUMP_SPEED;
    }

    private static boolean markReachableStates(PressureGraph graph) {
        Deque<PressureNode> frontier = new ArrayDeque<>();
        for (PressureNode endpointParent : graph.endpointParents) {
            if (endpointParent.reachable) {
                continue;
            }

            endpointParent.reachable = true;
            frontier.addLast(endpointParent);
        }

        while (!frontier.isEmpty()) {
            PressureNode current = frontier.removeFirst();
            if (current.parents == null) {
                continue;
            }

            for (PressureNode parent : current.parents) {
                if (parent.reachable) {
                    continue;
                }

                parent.reachable = true;
                frontier.addLast(parent);
            }
        }
        return graph.start.reachable;
    }

    @Override
    public void tick() {
        if (level == null) {
            return;
        }

        if (shouldRunServerLogic() && pressureUpdate) {
            updatePressureChange();
        }
        super.tick();
        if (!shouldRunServerLogic()) {
            return;
        }

        sidesToUpdate.forEachWithContext((update, frontSide) -> {
            if (update.isFalse()) {
                return;
            }

            update.setFalse();
            boolean isRecovery = recoveryAttempts.get(frontSide).booleanValue();
            recoveryAttempts.get(frontSide).setFalse();
            Direction front = getFront();
            boolean validPath = distributePressureTo(frontSide ? front : front.getOpposite());
            RecoveryState recovery = recoveryStates.get(frontSide);
            if (isRecovery) {
                recovery.recordRecoveryResult(validPath, level.getGameTime());
                return;
            }

            recovery.recordRebuildResult(validPath);
        });
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (!shouldRunServerLogic()) {
            return;
        }

        float speed = Mth.abs(getSpeed());
        if (Mth.abs(previousSpeed) == speed) {
            return;
        }

        if (hasRequiredSpeed(speed) && advancementBehaviour != null) {
            advancementBehaviour.awardPlayer(CCBAdvancements.TAKE_A_DEEP_BREATH);
        }
        updatePressureChange();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.TAKE_A_DEEP_BREATH, CCBAdvancements.GASEOUS_VARIATIONS, CCBAdvancements.MINTY_FRESH);
        behaviours.add(advancementBehaviour);

        transportBehaviour = new GasPumpTransportBehaviour(this);
        behaviours.add(transportBehaviour);
    }

    private void updatePressureChange() {
        if (level == null) {
            return;
        }

        if (transportBehaviour != null) {
            transportBehaviour.wipePressure();
        }
        Direction front = getFront();
        Direction back = front.getOpposite();
        BlockPos frontPos = worldPosition.relative(front);
        BlockPos backPos = worldPosition.relative(back);
        GasPropagator.propagatePipe(level, frontPos);
        GasPropagator.propagatePipe(level, backPos);
        recoveryAttempts.forEach(MutableBoolean::setFalse);
        sidesToUpdate.forEach(MutableBoolean::setTrue);
        pressureUpdate = false;
    }

    private void recoverMissingPressure(boolean recoverFront, boolean recoverBack) {
        if (level == null || !recoverFront && !recoverBack) {
            return;
        }

        if (transportBehaviour != null) {
            transportBehaviour.wipePressure();
        }
        Direction front = getFront();
        if (recoverFront) {
            BlockPos frontPos = worldPosition.relative(front);
            GasPropagator.propagatePipe(level, frontPos);
            recoveryAttempts.getFirst().setTrue();
            sidesToUpdate.getFirst().setTrue();
        }
        if (!recoverBack) {
            return;
        }

        BlockPos backPos = worldPosition.relative(front.getOpposite());
        GasPropagator.propagatePipe(level, backPos);
        recoveryAttempts.getSecond().setTrue();
        sidesToUpdate.getSecond().setTrue();
    }

    private boolean shouldRunServerLogic() {
        return level != null && (!level.isClientSide || isVirtual());
    }

    private Direction getFront() {
        return getBlockState().getValue(AirtightPumpBlock.FACING);
    }

    private boolean isPumpRunning() {
        return level != null && !isRemoved() && hasRequiredSpeed(getSpeed());
    }

    private float getPumpPressure() {
        return isPumpRunning() ? Mth.abs(getSpeed()) : 0;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (!shouldRunServerLogic() || transportBehaviour == null || level == null) {
            return;
        }

        float absSpeed = Mth.abs(getSpeed());
        Direction front = getFront();
        boolean stateChanged = !lazyStateInitialized || absSpeed != lastLazyAbsSpeed || front != lastLazyFacing;
        lazyStateInitialized = true;
        lastLazyAbsSpeed = absSpeed;
        lastLazyFacing = front;
        if (!isPumpRunning()) {
            return;
        }

        if (stateChanged) {
            updatePressureChange();
            return;
        }

        long gameTime = level.getGameTime();
        BlockPos frontPos = worldPosition.relative(front);
        BlockPos backPos = worldPosition.relative(front.getOpposite());
        GasTransportBehaviour frontPipe = GasPropagator.getBehaviour(level, frontPos);
        GasTransportBehaviour backPipe = GasPropagator.getBehaviour(level, backPos);
        boolean isFrontPressureMissing = frontPipe != null && !frontPipe.hasAnyPressureContribution();
        boolean isBackPressureMissing = backPipe != null && !backPipe.hasAnyPressureContribution();
        boolean recoverFront = recoveryStates.getFirst().shouldAttempt(isFrontPressureMissing, gameTime);
        boolean recoverBack = recoveryStates.getSecond().shouldAttempt(isBackPressureMissing, gameTime);
        recoverMissingPressure(recoverFront, recoverBack);
    }

    public void updatePipesOnSide(Direction side) {
        if (!isSideAccessible(side)) {
            return;
        }

        updatePipeNetwork(isFront(side));
        if (transportBehaviour == null) {
            return;
        }

        transportBehaviour.wipePressure();
    }

    private boolean isSideAccessible(Direction direction) {
        return isSideAccessible(getBlockState(), direction);
    }

    private boolean isFront(Direction direction) {
        return isFront(getBlockState(), direction);
    }

    private void updatePipeNetwork(boolean front) {
        recoveryAttempts.get(front).setFalse();
        sidesToUpdate.get(front).setTrue();
    }

    public void markPressureUpdate() {
        pressureUpdate = true;
    }

    private boolean distributePressureTo(Direction side) {
        if (!isPumpRunning() || level == null) {
            return false;
        }

        boolean pull = isPullingOnSide(isFront(side));
        Direction entryFace = side.getOpposite();
        BlockPos startPos = worldPosition.relative(side);
        if (!level.isLoaded(startPos)) {
            return false;
        }

        if (!pull) {
            GasPropagator.resetAffectedNetworks(level, worldPosition, entryFace);
        }
        AdjacentTarget startTarget = GasPropagator.resolveAdjacentTarget(level, worldPosition, side);
        if (hasReachedValidEndpoint(level, startTarget, pull)) {
            return true;
        }

        PressureGraph graph = buildPressureGraph(startPos, entryFace, pull);
        if (graph.endpointParents.isEmpty() || !markReachableStates(graph)) {
            return false;
        }

        applyPressureGraph(graph, pull, getPumpPressure());
        return true;
    }

    private PressureGraph buildPressureGraph(BlockPos startPos, Direction entryFace, boolean pull) {
        PressureGraph graph = new PressureGraph(startPos, entryFace);
        if (level == null || !level.isLoaded(startPos)) {
            return graph;
        }

        GasTransportBehaviour startPipe = GasPropagator.getBehaviour(level, startPos);
        if (startPipe == null || startPipe instanceof GasPumpTransportBehaviour) {
            return graph;
        }

        int maxDistance = GasPropagator.getAirtightPumpMaxRange();
        Deque<PressureNode> frontier = new ArrayDeque<>();
        graph.start.searchQueued = true;
        frontier.addLast(graph.start);

        while (!frontier.isEmpty()) {
            PressureNode current = frontier.removeFirst();
            current.searchQueued = false;
            if (current.expanded || !level.isLoaded(current.pos)) {
                continue;
            }

            current.expanded = true;
            BlockState currentState = level.getBlockState(current.pos);
            GasTransportBehaviour currentPipe = GasPropagator.getBehaviour(level, current.pos);
            if (currentPipe == null || currentPipe instanceof GasPumpTransportBehaviour) {
                continue;
            }

            boolean canFlowFromEntry = pull ? currentPipe.allowsOutboundFlow(currentState, current.entryFace) : currentPipe.allowsInboundFlow(currentState, current.entryFace);
            if (!canFlowFromEntry) {
                continue;
            }

            for (Direction face : Iterate.directions) {
                if (face == current.entryFace || current.pos.equals(startPos) && face == entryFace) {
                    continue;
                }

                boolean canFlowToExit = pull ? currentPipe.allowsInboundFlow(currentState, face) : currentPipe.allowsOutboundFlow(currentState, face);
                if (!canFlowToExit) {
                    continue;
                }

                BlockPos connectedPos = current.pos.relative(face);
                if (!level.isLoaded(connectedPos)) {
                    continue;
                }

                AdjacentTarget target = GasPropagator.resolveAdjacentTarget(level, current.pos, face);
                if (hasReachedValidEndpoint(level, target, pull)) {
                    current.addEdge(new FlowEdge(face, pull, null));
                    graph.markEndpointParent(current);
                    continue;
                }

                GasTransportBehaviour connectedPipe = target.behaviour();
                if (connectedPipe == null || connectedPipe instanceof GasPumpTransportBehaviour) {
                    continue;
                }

                BlockState connectedState = target.state();
                Direction connectedFace = target.connectedFace();
                boolean canCrossBoundary = pull ? connectedPipe.allowsOutboundFlow(connectedState, connectedFace) : connectedPipe.allowsInboundFlow(connectedState, connectedFace);
                if (!canCrossBoundary) {
                    continue;
                }

                int nextDistance = current.distance + 1;
                if (nextDistance > maxDistance) {
                    continue;
                }

                PressureNode next = graph.getOrCreate(connectedPos, connectedFace, nextDistance);
                if (next.distance != nextDistance) {
                    continue;
                }

                current.addEdge(new FlowEdge(face, pull, next));
                next.addParent(current);
                if (next.expanded || next.searchQueued) {
                    continue;
                }

                next.searchQueued = true;
                frontier.addLast(next);
            }
        }

        return graph;
    }

    private void applyPressureGraph(PressureGraph graph, boolean pull, float pressure) {
        Deque<PressureNode> frontier = new ArrayDeque<>();
        graph.start.pendingPressure = pressure;
        graph.start.pressureQueued = true;
        frontier.addLast(graph.start);

        while (!frontier.isEmpty()) {
            PressureNode current = frontier.removeFirst();
            current.pressureQueued = false;
            if (!current.reachable || current.pressureApplied || current.edges == null) {
                continue;
            }

            current.pressureApplied = true;
            int validEdgeCount = 0;
            for (FlowEdge edge : current.edges) {
                if (edge.target == null || edge.target.reachable) {
                    validEdgeCount++;
                }
            }
            if (validEdgeCount == 0) {
                continue;
            }

            addPressureToPipe(current.pos, current.entryFace, !pull, current.pendingPressure);
            float branchPressure = current.pendingPressure / validEdgeCount;
            for (FlowEdge edge : current.edges) {
                if (edge.target != null && !edge.target.reachable) {
                    continue;
                }

                addPressureToPipe(current.pos, edge.face, edge.inbound, branchPressure);
                if (edge.target == null || edge.target.pressureApplied) {
                    continue;
                }

                edge.target.pendingPressure += branchPressure;
                if (edge.target.pressureQueued) {
                    continue;
                }

                edge.target.pressureQueued = true;
                frontier.addLast(edge.target);
            }
        }
    }

    private void addPressureToPipe(BlockPos pipePos, Direction pipeSide, boolean inbound, float pressure) {
        if (level == null || pipePos.equals(worldPosition) || pressure <= 0) {
            return;
        }

        GasTransportBehaviour transport = GasPropagator.getBehaviour(level, pipePos);
        if (transport == null) {
            return;
        }

        transport.addPressure(pipeSide, inbound, pressure);
    }

    @Override
    public boolean canTransport(Level level, BlockState state, BlockPos pos, Direction direction) {
        return isPumpRunning() && isSideAccessible(state, direction) && isPullingOnSide(isFront(state, direction));
    }

    @Override
    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    private static final class RecoveryState {
        private boolean hadValidPath;
        private int backoff = RECOVERY_INITIAL_BACKOFF;
        private long nextAttempt;

        private boolean shouldAttempt(boolean pressureMissing, long gameTime) {
            return hadValidPath && pressureMissing && gameTime >= nextAttempt;
        }

        private void recordRebuildResult(boolean validPath) {
            hadValidPath = validPath;
            backoff = RECOVERY_INITIAL_BACKOFF;
            nextAttempt = 0;
        }

        private void recordRecoveryResult(boolean validPath, long gameTime) {
            if (validPath) {
                hadValidPath = true;
                backoff = RECOVERY_INITIAL_BACKOFF;
                nextAttempt = gameTime + RECOVERY_INITIAL_BACKOFF;
                return;
            }

            nextAttempt = gameTime + backoff;
            backoff = Math.min(RECOVERY_MAX_BACKOFF, backoff * 2);
        }
    }

    private static final class PressureGraph {
        private final Map<BlockPos, PressureNode[]> nodesByPos = new HashMap<>();
        private final List<PressureNode> endpointParents = new ArrayList<>();
        private final PressureNode start;

        private PressureGraph(BlockPos startPos, Direction entryFace) {
            start = getOrCreate(startPos, entryFace, 1);
        }

        private PressureNode getOrCreate(BlockPos pos, Direction entryFace, int distance) {
            PressureNode[] nodes = nodesByPos.computeIfAbsent(pos, ignored -> new PressureNode[DIRECTION_COUNT]);
            int index = entryFace.ordinal();
            PressureNode node = nodes[index];
            if (node != null) {
                return node;
            }

            node = new PressureNode(pos, entryFace, distance);
            nodes[index] = node;
            return node;
        }

        private void markEndpointParent(PressureNode node) {
            if (node.endpointParent) {
                return;
            }

            node.endpointParent = true;
            endpointParents.add(node);
        }
    }

    private static final class PressureNode {
        private final BlockPos pos;
        private final Direction entryFace;
        private final int distance;
        @Nullable
        private List<FlowEdge> edges;
        @Nullable
        private List<PressureNode> parents;
        private boolean searchQueued;
        private boolean expanded;
        private boolean endpointParent;
        private boolean reachable;
        private boolean pressureQueued;
        private boolean pressureApplied;
        private float pendingPressure;

        private PressureNode(BlockPos pos, Direction entryFace, int distance) {
            this.pos = pos;
            this.entryFace = entryFace;
            this.distance = distance;
        }

        private void addEdge(FlowEdge edge) {
            if (edges == null) {
                edges = new ArrayList<>();
            }
            edges.add(edge);
        }

        private void addParent(PressureNode parent) {
            if (parents == null) {
                parents = new ArrayList<>();
            }
            parents.add(parent);
        }
    }

    private record FlowEdge(Direction face, boolean inbound, @Nullable PressureNode target) {}

    private class GasPumpTransportBehaviour extends GasTransportBehaviour {
        public GasPumpTransportBehaviour(AirtightPumpBlockEntity blockEntity) {
            super(blockEntity);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            return canHaveFlowTowardWithoutLevel(state, direction);
        }

        @Override
        public boolean canHaveFlowTowardWithoutLevel(BlockState state, Direction direction) {
            return isSideAccessible(direction);
        }

        @Override
        protected void beforeFlowUpdate(Level level, BlockPos pos, Collection<GasPipeConnection> connections) {
            if (level.isClientSide && !isVirtual() || !level.isLoaded(pos) || isRemoved()) {
                return;
            }

            float pressure = getPumpPressure();
            for (GasPipeConnection connection : connections) {
                Direction direction = connection.getSide();
                connection.setPumpPressure(isPullingOnSide(isFront(direction)), pressure);
            }
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction) {
            if (!(level.getBlockState(pos.relative(direction)).getBlock() instanceof IAirtightPipeDrain)) {
                return AttachmentTypes.NONE;
            }
            return AttachmentTypes.DRAIN;
        }
    }
}
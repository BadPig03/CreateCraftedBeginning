package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasPipeConnection;
import net.ty.createcraftedbeginning.api.gas.gases.GasPropagator;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasExtractor;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPumpBlockEntity extends KineticBlockEntity implements IGasExtractor {
    private static final int LAZY_TICK_RATE = 10;

    private final Couple<MutableBoolean> sidesToUpdate;
    private boolean pressureUpdate;
    private CCBAdvancementBehaviour advancementBehaviour;
    private GasPumpTransportBehaviour transportBehaviour;

    private boolean lazyStateInitialized;
    private boolean lastLazyFoundGas;
    private float lastLazyAbsSpeed;
    private Direction lastLazyFacing;

    public AirtightPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        sidesToUpdate = Couple.create(MutableBoolean::new);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    private static boolean hasReachedValidEndpoint(LevelAccessor level, BlockFace blockFace, boolean pull) {
        BlockPos connectedPos = blockFace.getConnectedPos();
        BlockState connectedState = level.getBlockState(connectedPos);
        BlockEntity blockEntity = level.getBlockEntity(connectedPos);
        Direction face = blockFace.getFace();
        if (AirtightPumpBlock.isPump(connectedState) && connectedState.getValue(AirtightPumpBlock.FACING).getAxis() == face.getAxis() && blockEntity instanceof AirtightPumpBlockEntity pumpBE) {
            return pumpBE.canPump() && isPullingOnSide(pumpBE.isFront(blockFace.getOppositeFace())) != pull;
        }

        GasTransportBehaviour pipe = GasPropagator.getPipe(level, connectedPos);
        if (pipe != null && pipe.canHaveFlowToward(connectedState, blockFace.getOppositeFace())) {
            return false;
        }

        if (blockEntity != null && blockEntity.getLevel() != null) {
            IGasHandler capability = blockEntity.getLevel().getCapability(GasHandler.BLOCK, blockEntity.getBlockPos(), face.getOpposite());
            if (capability != null) {
                return true;
            }
        }

        return GasPropagator.isOpenEnd(level, blockFace.getPos(), face);
    }

    private static boolean isPullingOnSide(boolean front) {
        return !front;
    }

    private static boolean isSideAccessible(BlockState state, Direction direction) {
        return state.getBlock() instanceof AirtightPumpBlock && state.getValue(AirtightPumpBlock.FACING).getAxis() == direction.getAxis();
    }

    private static boolean isFront(BlockState state, Direction direction) {
        return state.getBlock() instanceof AirtightPumpBlock && direction == state.getValue(AirtightPumpBlock.FACING);
    }

    private static Set<TraversalState> collectReachableStates(NetworkGraph graph) {
        Set<TraversalState> reachable = new HashSet<>(graph.endpointParents);
        Deque<TraversalState> frontier = new ArrayDeque<>(graph.endpointParents);
        while (!frontier.isEmpty()) {
            TraversalState current = frontier.removeFirst();
            for (TraversalState parent : graph.reverseEdges.getOrDefault(current, Collections.emptyList())) {
                if (!reachable.add(parent)) {
                    continue;
                }

                frontier.addLast(parent);
            }
        }
        return reachable;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide && !isVirtual()) {
            return;
        }

        if (pressureUpdate) {
            pressureUpdate = false;
            updatePressureChange();
        }
        sidesToUpdate.forEachWithContext((update, isFront) -> {
            if (update.isFalse()) {
                return;
            }
            update.setFalse();
            Direction front = getFront();
            distributePressureTo(isFront ? front : front.getOpposite());
        });
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (level == null || level.isClientSide && !isVirtual()) {
            return;
        }

        float speed = Mth.abs(getSpeed());
        if (Mth.abs(previousSpeed) == speed) {
            return;
        }
        if (speed >= SpeedLevel.MEDIUM.getSpeedValue() && advancementBehaviour != null) {
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

        Direction front = getFront();
        BlockPos frontPos = worldPosition.relative(front);
        BlockPos backPos = worldPosition.relative(front.getOpposite());
        GasPropagator.propagateChangedPipe(level, frontPos, level.getBlockState(frontPos));
        GasPropagator.propagateChangedPipe(level, backPos, level.getBlockState(backPos));
        if (transportBehaviour != null) {
            transportBehaviour.wipePressure();
        }
        sidesToUpdate.forEach(MutableBoolean::setTrue);
        pressureUpdate = false;
    }

    private Direction getFront() {
        return getBlockState().getValue(AirtightPumpBlock.FACING);
    }

    private boolean canPump() {
        return level != null && Mth.abs(getSpeed()) >= SpeedLevel.MEDIUM.getSpeedValue();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || transportBehaviour == null || level.isClientSide && !isVirtual()) {
            return;
        }

        float absSpeed = Mth.abs(getSpeed());
        Direction front = getFront();
        boolean foundGas = false;
        for (Entry<Direction, GasPipeConnection> entry : transportBehaviour.interfaces.entrySet()) {
            GasStack gasStack = transportBehaviour.getProvidedOutwardGas(entry.getKey());
            if (!gasStack.isEmpty()) {
                foundGas = true;
                break;
            }
        }

        boolean stateChanged = !lazyStateInitialized || foundGas != lastLazyFoundGas || absSpeed != lastLazyAbsSpeed || front != lastLazyFacing;
        lazyStateInitialized = true;
        lastLazyFoundGas = foundGas;
        lastLazyAbsSpeed = absSpeed;
        lastLazyFacing = front;
        if (absSpeed < SpeedLevel.MEDIUM.getSpeedValue()) {
            return;
        }

        GasTransportBehaviour frontPipe = GasPropagator.getPipe(level, worldPosition.relative(front));
        GasTransportBehaviour backPipe = GasPropagator.getPipe(level, worldPosition.relative(front.getOpposite()));
        boolean pressureMissing = frontPipe != null && !frontPipe.hasAnyPressure() || backPipe != null && !backPipe.hasAnyPressure();
        if (!stateChanged && (!foundGas || !pressureMissing)) {
            return;
        }

        updatePressureChange();
    }

    public void updatePipesOnSide(Direction direction) {
        if (!isSideAccessible(direction)) {
            return;
        }

        updatePipeNetwork(isFront(direction));
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
        sidesToUpdate.get(front).setTrue();
    }

    public void markPressureUpdate() {
        pressureUpdate = true;
    }

    private void distributePressureTo(Direction side) {
        if (!canPump() || level == null) {
            return;
        }

        BlockFace start = new BlockFace(worldPosition, side);
        boolean pull = isPullingOnSide(isFront(side));
        if (!pull) {
            GasPropagator.resetAffectedNetworks(level, worldPosition, side.getOpposite());
        }
        if (hasReachedValidEndpoint(level, start, pull)) {
            return;
        }

        TraversalState startState = new TraversalState(start.getConnectedPos(), side.getOpposite());
        NetworkGraph graph = buildPressureGraph(start, startState, pull);
        if (graph.edgesByState.isEmpty() || graph.endpointParents.isEmpty()) {
            return;
        }

        Set<TraversalState> reachableStates = collectReachableStates(graph);
        if (!reachableStates.contains(startState)) {
            return;
        }

        applyPressureGraph(graph, reachableStates, startState, pull, Mth.abs(getSpeed()));
    }

    private NetworkGraph buildPressureGraph(BlockFace start, TraversalState startState, boolean pull) {
        NetworkGraph graph = new NetworkGraph();
        if (level == null || !level.isLoaded(startState.pos)) {
            return graph;
        }

        GasTransportBehaviour startPipe = GasPropagator.getPipe(level, startState.pos);
        if (startPipe == null || startPipe instanceof GasPumpTransportBehaviour) {
            return graph;
        }

        int maxDistance = GasPropagator.getAirtightPumpRange();
        Deque<SearchEntry> frontier = new ArrayDeque<>();
        Set<TraversalState> visited = new HashSet<>();
        frontier.add(new SearchEntry(startState, 1));
        while (!frontier.isEmpty()) {
            SearchEntry entry = frontier.removeFirst();
            TraversalState current = entry.state;
            int distance = entry.distance;
            if (!level.isLoaded(current.pos) || !visited.add(current)) {
                continue;
            }

            BlockState currentState = level.getBlockState(current.pos);
            GasTransportBehaviour currentPipe = GasPropagator.getPipe(level, current.pos);
            if (currentPipe == null || currentPipe instanceof GasPumpTransportBehaviour) {
                continue;
            }

            for (Direction face : GasPropagator.getPipeConnections(currentState, currentPipe)) {
                if (face == current.entryFace) {
                    continue;
                }

                BlockFace blockFace = new BlockFace(current.pos, face);
                BlockPos connectedPos = blockFace.getConnectedPos();
                if (!level.isLoaded(connectedPos) || blockFace.isEquivalent(start)) {
                    continue;
                }

                if (hasReachedValidEndpoint(level, blockFace, pull)) {
                    graph.addEndpointEdge(current, new FlowEdge(face, pull, null));
                    continue;
                }

                GasTransportBehaviour connectedPipe = GasPropagator.getPipe(level, connectedPos);
                if (connectedPipe == null || connectedPipe instanceof GasPumpTransportBehaviour) {
                    continue;
                }

                BlockState connectedState = level.getBlockState(connectedPos);
                if (!connectedPipe.canHaveFlowToward(connectedState, face.getOpposite())) {
                    continue;
                }

                if (distance + 1 >= maxDistance) {
                    graph.addEndpointEdge(current, new FlowEdge(face, pull, null));
                    continue;
                }

                TraversalState next = new TraversalState(connectedPos, face.getOpposite());
                graph.addPipeEdge(current, new FlowEdge(face, pull, next));
                if (visited.contains(next)) {
                    continue;
                }

                frontier.addLast(new SearchEntry(next, distance + 1));
            }
        }

        return graph;
    }

    private void applyPressureGraph(NetworkGraph graph, Set<TraversalState> reachableStates, TraversalState startState, boolean pull, float pressure) {
        Deque<PressureEntry> frontier = new ArrayDeque<>();
        Set<TraversalState> appliedStates = new HashSet<>();
        frontier.add(new PressureEntry(startState, pressure));
        while (!frontier.isEmpty()) {
            PressureEntry entry = frontier.removeFirst();
            TraversalState current = entry.state;
            if (!reachableStates.contains(current) || !appliedStates.add(current)) {
                continue;
            }

            List<FlowEdge> validEdges = new ArrayList<>();
            for (FlowEdge edge : graph.edgesByState.getOrDefault(current, Collections.emptyList())) {
                if (edge.target != null && !reachableStates.contains(edge.target)) {
                    continue;
                }

                validEdges.add(edge);
            }
            if (validEdges.isEmpty()) {
                continue;
            }

            addPressureToPipe(current.pos, current.entryFace, !pull, entry.pressure);
            float branchPressure = entry.pressure / validEdges.size();
            for (FlowEdge edge : validEdges) {
                addPressureToPipe(current.pos, edge.face, edge.inbound, branchPressure);
                if (edge.target == null) {
                    continue;
                }

                frontier.addLast(new PressureEntry(edge.target, branchPressure));
            }
        }
    }

    private void addPressureToPipe(BlockPos pipePos, Direction pipeSide, boolean inbound, float pressure) {
        if (level == null || pipePos.equals(worldPosition) || pressure <= 0) {
            return;
        }

        GasTransportBehaviour pipeBehaviour = GasPropagator.getPipe(level, pipePos);
        if (pipeBehaviour == null) {
            return;
        }

        pipeBehaviour.addPressure(pipeSide, inbound, pressure);
    }

    @Override
    public boolean canExtract(Level level, BlockState blockState, BlockPos blockPos, Direction direction) {
        return isSpeedRequirementFulfilled() && isSideAccessible(blockState, direction) && isPullingOnSide(isFront(blockState, direction));
    }

    @Override
    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    private static class NetworkGraph {
        private final Map<TraversalState, List<FlowEdge>> edgesByState = new HashMap<>();
        private final Map<TraversalState, List<TraversalState>> reverseEdges = new HashMap<>();
        private final Set<TraversalState> endpointParents = new HashSet<>();

        private void addEndpointEdge(TraversalState from, FlowEdge edge) {
            edgesByState.computeIfAbsent(from, $ -> new ArrayList<>()).add(edge);
            endpointParents.add(from);
        }

        private void addPipeEdge(TraversalState from, FlowEdge edge) {
            edgesByState.computeIfAbsent(from, $ -> new ArrayList<>()).add(edge);
            reverseEdges.computeIfAbsent(edge.target, $ -> new ArrayList<>()).add(from);
        }
    }

    private record FlowEdge(Direction face, boolean inbound, @Nullable TraversalState target) {}

    private record TraversalState(BlockPos pos, Direction entryFace) {
        @Override
        public boolean equals(Object object) {
            return this == object || object instanceof TraversalState(BlockPos p, Direction f) && pos.equals(p) && entryFace == f;
        }
    }

    private record SearchEntry(TraversalState state, int distance) {}

    private record PressureEntry(TraversalState state, float pressure) {}

    private class GasPumpTransportBehaviour extends GasTransportBehaviour {
        public GasPumpTransportBehaviour(AirtightPumpBlockEntity be) {
            super(be);
        }

        @Override
        public boolean canHaveFlowToward(BlockState state, Direction direction) {
            return isSideAccessible(direction);
        }

        @Override
        public AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction) {
            return AttachmentTypes.NONE;
        }

        @Override
        public void tick() {
            super.tick();
            Level level = getWorld();
            if (level == null || level.isClientSide || !level.isLoaded(getPos()) || isRemoved()) {
                return;
            }

            float absSpeed = Mth.abs(getSpeed());
            if (absSpeed < SpeedLevel.MEDIUM.getSpeedValue()) {
                return;
            }

            for (Entry<Direction, GasPipeConnection> entry : interfaces.entrySet()) {
                boolean pull = isPullingOnSide(isFront(entry.getKey()));
                Couple<Float> pressure = entry.getValue().getPressure();
                pressure.set(pull, absSpeed);
                pressure.set(!pull, 0.0f);
            }
        }
    }
}
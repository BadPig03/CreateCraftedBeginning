package net.ty.createcraftedbeginning.content.airtights.gas.transport;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.flowsources.GasFlowSource;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasPipeConnection.GasFlow;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasNetworkTraversal {
    private static final int TRAVERSAL_WORK_BUDGET_PER_TICK = 256;
    private static final int QUEUED_ENTRY_WORK = 1;
    private static final int FRONTIER_ENTRY_WORK = 1 + Direction.values().length;
    private static final Map<Level, Map<TopologyKey, WeakReference<SharedTopology>>> SHARED_TOPOLOGIES = new WeakHashMap<>();

    private final Level level;
    private final BlockFace start;
    private final Deque<BlockFace> queued = new ArrayDeque<>();
    private final Deque<BlockFace> frontier = new ArrayDeque<>();
    private final Set<BlockFace> frontierMembership = new HashSet<>();
    private final Set<BlockPos> visited = new HashSet<>();
    private final Map<BlockFace, GasFlowSource> targets = new TreeMap<>(GasNetworkTraversal::compareBlockFaces);
    private final Map<BlockPos, WeakReference<GasTransportBehaviour>> cache = new HashMap<>();
    @Nullable
    private SharedTopology sharedTopology;
    private GasStack gas = GasStack.EMPTY;
    private long transferRateUnits;
    private int targetProbeCursor;

    GasNetworkTraversal(Level level, BlockFace start) {
        this.level = level;
        this.start = start;
    }

    private static int compareBlockFaces(BlockFace first, BlockFace second) {
        BlockPos firstPos = first.getPos();
        BlockPos secondPos = second.getPos();
        int xComparison = Integer.compare(firstPos.getX(), secondPos.getX());
        if (xComparison != 0) {
            return xComparison;
        }

        int yComparison = Integer.compare(firstPos.getY(), secondPos.getY());
        if (yComparison != 0) {
            return yComparison;
        }

        int zComparison = Integer.compare(firstPos.getZ(), secondPos.getZ());
        if (zComparison != 0) {
            return zComparison;
        }
        return Integer.compare(first.getFace().ordinal(), second.getFace().ordinal());
    }

    void reset() {
        clear();
        queued.addLast(start);
    }

    void clear() {
        frontier.clear();
        frontierMembership.clear();
        visited.clear();
        targets.clear();
        cache.clear();
        queued.clear();
        sharedTopology = null;
        gas = GasStack.EMPTY;
        transferRateUnits = 0;
        targetProbeCursor = 0;
    }

    void tick() {
        int remainingWork = TRAVERSAL_WORK_BUDGET_PER_TICK;
        Deque<BlockFace> deferredFrontier = new ArrayDeque<>();
        while (remainingWork > 0) {
            boolean performedWork = false;
            if (!queued.isEmpty()) {
                processQueuedEntry(queued.removeFirst());
                remainingWork -= QUEUED_ENTRY_WORK;
                performedWork = true;
            }

            if (!frontier.isEmpty() && remainingWork >= FRONTIER_ENTRY_WORK) {
                BlockFace frontierFace = frontier.removeFirst();
                remainingWork -= FRONTIER_ENTRY_WORK;
                performedWork = true;
                if (processFrontierEntry(frontierFace)) {
                    deferredFrontier.addLast(frontierFace);
                }
                else {
                    frontierMembership.remove(frontierFace);
                }
            }

            if (!performedWork) {
                break;
            }
        }

        frontier.addAll(deferredFrontier);
        promoteStableTopology();
    }

    GasStack getGas() {
        return gas;
    }

    long getTransferRateUnits() {
        return transferRateUnits;
    }

    boolean hasTransferTargets() {
        promoteStableTopology();
        if (sharedTopology == null) {
            return !targets.isEmpty();
        }
        return !sharedTopology.isEmpty();
    }

    List<BlockFace> claimTargetProbeWindow(int maxTargets) {
        promoteStableTopology();
        if (maxTargets <= 0) {
            return Collections.emptyList();
        }

        if (sharedTopology != null) {
            ProbeWindow probeWindow = sharedTopology.claim(targetProbeCursor, maxTargets);
            targetProbeCursor = probeWindow.nextCursor;
            return probeWindow.locations;
        }

        if (targets.isEmpty()) {
            return Collections.emptyList();
        }

        List<BlockFace> targetLocations = new ArrayList<>(targets.keySet());
        int targetCount = targetLocations.size();
        int claimCount = Math.min(maxTargets, targetCount);
        int startIndex = Math.floorMod(targetProbeCursor, targetCount);
        List<BlockFace> claimedTargets = new ArrayList<>(claimCount);
        for (int offset = 0; offset < claimCount; offset++) {
            claimedTargets.add(targetLocations.get((startIndex + offset) % targetCount));
        }

        targetProbeCursor = (startIndex + claimCount) % targetCount;
        return claimedTargets;
    }

    void invalidateTarget(BlockFace location) {
        if (sharedTopology != null) {
            sharedTopology.invalidate(location);
            return;
        }

        targets.remove(location);
    }

    @Nullable InventoryIdentifier getInventoryIdentifier(BlockFace inventoryFace) {
        if (!(level.getBlockEntity(inventoryFace.getPos()) instanceof IGasInventoryIdentifierProvider provider)) {
            return null;
        }
        return provider.getGasInventoryIdentifier(inventoryFace.getFace());
    }

    @Nullable GasFlowSource refreshTarget(BlockFace location) {
        if (!isPresent(location)) {
            return null;
        }

        GasPipeConnection connection = get(location);
        if (connection == null) {
            return null;
        }

        GasFlow targetFlow = connection.getFlow();
        if (targetFlow == null || targetFlow.inbound || !GasStack.isSameGasSameComponents(targetFlow.gas, gas)) {
            return null;
        }

        if (connection.getSource() == null && !connection.determineSource(level, location.getPos())) {
            return null;
        }

        GasFlowSource targetSource = connection.getSource();
        if (targetSource == null || !targetSource.isEndpoint()) {
            return null;
        }
        return targetSource;
    }

    private void processQueuedEntry(BlockFace blockFace) {
        if (!isPresent(blockFace)) {
            return;
        }

        GasPipeConnection connection = get(blockFace);
        if (connection == null) {
            return;
        }

        if (blockFace.equals(start)) {
            transferRateUnits = connection.getInboundPressureUnits();
        }
        if (!frontierMembership.add(blockFace)) {
            return;
        }

        frontier.addLast(blockFace);
    }

    private boolean processFrontierEntry(BlockFace blockFace) {
        if (!isPresent(blockFace)) {
            return false;
        }

        GasPipeConnection connection = get(blockFace);
        if (connection == null) {
            return false;
        }

        GasFlow flow = connection.getFlow();
        if (flow == null) {
            return true;
        }

        if (!gas.isEmpty() && !GasStack.isSameGasSameComponents(flow.gas, gas)) {
            return false;
        }

        if (!flow.inbound) {
            return connection.getPressureDirection() < 0;
        }

        if (gas.isEmpty()) {
            gas = flow.gas;
        }
        boolean keepInFrontier = false;
        for (Direction side : Iterate.directions) {
            if (side == blockFace.getFace()) {
                continue;
            }

            BlockFace adjacentLocation = new BlockFace(blockFace.getPos(), side);
            GasPipeConnection adjacentConnection = get(adjacentLocation);
            if (adjacentConnection == null) {
                continue;
            }

            GasFlow adjacentFlow = adjacentConnection.getFlow();
            if (adjacentFlow == null) {
                if (adjacentConnection.hasPressure() && adjacentConnection.getOutwardPressureUnits() > 0) {
                    keepInFrontier = true;
                }
                continue;
            }

            if (adjacentFlow.inbound) {
                if (adjacentConnection.getPressureDirection() > 0) {
                    keepInFrontier = true;
                }
                continue;
            }

            if (adjacentConnection.getSource() == null && !adjacentConnection.determineSource(level, blockFace.getPos())) {
                keepInFrontier = true;
                continue;
            }

            GasFlowSource targetSource = adjacentConnection.getSource();
            if (targetSource != null && targetSource.isEndpoint()) {
                targets.put(adjacentLocation, targetSource);
                continue;
            }

            if (!visited.add(adjacentLocation.getConnectedPos())) {
                continue;
            }

            queued.addLast(adjacentLocation.getOpposite());
        }
        return keepInFrontier;
    }

    private void promoteStableTopology() {
        if (sharedTopology != null || gas.isEmpty() || targets.isEmpty() || !queued.isEmpty() || !frontier.isEmpty()) {
            return;
        }

        List<BlockFace> targetLocations = List.copyOf(targets.keySet());
        TopologyKey key = new TopologyKey(gas.copyWithAmount(1), targetLocations);
        synchronized (SHARED_TOPOLOGIES) {
            Map<TopologyKey, WeakReference<SharedTopology>> levelTopologies = SHARED_TOPOLOGIES.computeIfAbsent(level, ignored -> new HashMap<>());
            levelTopologies.entrySet().removeIf(entry -> entry.getValue().get() == null);
            WeakReference<SharedTopology> reference = levelTopologies.get(key);
            SharedTopology topology = reference == null ? null : reference.get();
            if (topology == null) {
                topology = new SharedTopology(targetLocations);
                levelTopologies.put(key, new WeakReference<>(topology));
            }
            sharedTopology = topology;
        }

        int topologySize = sharedTopology.size();
        targetProbeCursor = topologySize == 0 ? 0 : Math.floorMod(31 * start.getPos().hashCode() + start.getFace().ordinal(), topologySize);
        targets.clear();
    }

    @Nullable
    private GasPipeConnection get(BlockFace location) {
        GasTransportBehaviour transport = getGasTransfer(location.getPos());
        if (transport == null) {
            return null;
        }
        return transport.getConnection(location.getFace());
    }

    @Nullable
    private GasTransportBehaviour getGasTransfer(BlockPos pos) {
        WeakReference<GasTransportBehaviour> cachedReference = cache.get(pos);
        GasTransportBehaviour cachedBehaviour = cachedReference == null ? null : cachedReference.get();
        if (cachedBehaviour != null && !cachedBehaviour.blockEntity.isRemoved()) {
            return cachedBehaviour;
        }

        GasTransportBehaviour behaviour = BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
        if (behaviour == null) {
            return null;
        }

        cache.put(pos, new WeakReference<>(behaviour));
        return behaviour;
    }

    private boolean isPresent(BlockFace location) {
        return level.isLoaded(location.getPos());
    }

    private record ProbeWindow(List<BlockFace> locations, int nextCursor) {}

    private record TopologyKey(GasStack gas, List<BlockFace> locations) {}

    private record SharedTopology(List<BlockFace> locations) {
        private SharedTopology(List<BlockFace> locations) {
            this.locations = new ArrayList<>(locations);
        }

        private boolean isEmpty() {
            return locations.isEmpty();
        }

        private int size() {
            return locations.size();
        }

        private ProbeWindow claim(int requestedCursor, int maxTargets) {
            if (locations.isEmpty() || maxTargets <= 0) {
                return new ProbeWindow(Collections.emptyList(), 0);
            }

            int targetCount = locations.size();
            int claimCount = Math.min(maxTargets, targetCount);
            int startIndex = Math.floorMod(requestedCursor, targetCount);
            List<BlockFace> claimedTargets = new ArrayList<>(claimCount);
            for (int offset = 0; offset < claimCount; offset++) {
                claimedTargets.add(locations.get((startIndex + offset) % targetCount));
            }
            return new ProbeWindow(claimedTargets, (startIndex + claimCount) % targetCount);
        }

        private void invalidate(BlockFace location) {
            locations.remove(location);
        }
    }
}

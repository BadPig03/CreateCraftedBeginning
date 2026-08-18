package net.ty.createcraftedbeginning.content.airtights.gas.transport;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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
public final class GasNetworkTraversal {
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

    public GasNetworkTraversal(Level level, BlockFace start) {
        this.level = level;
        this.start = start;
    }

    private static int compareBlockFaces(BlockFace first, BlockFace second) {
        BlockPos firstPos = first.getPos();
        BlockPos secondPos = second.getPos();
        int x = Integer.compare(firstPos.getX(), secondPos.getX());
        if (x != 0) {
            return x;
        }

        int y = Integer.compare(firstPos.getY(), secondPos.getY());
        if (y != 0) {
            return y;
        }

        int z = Integer.compare(firstPos.getZ(), secondPos.getZ());
        return z != 0 ? z : Integer.compare(first.getFace().ordinal(), second.getFace().ordinal());
    }

    public void reset() {
        clear();
        queued.addLast(start);
    }

    public void clear() {
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

    public void tick() {
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
                BlockFace blockFace = frontier.removeFirst();
                remainingWork -= FRONTIER_ENTRY_WORK;
                performedWork = true;
                if (processFrontierEntry(blockFace)) {
                    deferredFrontier.addLast(blockFace);
                }
                else {
                    frontierMembership.remove(blockFace);
                }
            }

            if (!performedWork) {
                break;
            }
        }

        frontier.addAll(deferredFrontier);
        promoteStableTopology();
    }

    public GasStack getGas() {
        return gas;
    }

    public long getTransferRateUnits() {
        return transferRateUnits;
    }

    public boolean hasTransferTargets() {
        promoteStableTopology();
        return sharedTopology == null ? !targets.isEmpty() : !sharedTopology.isEmpty();
    }

    public List<BlockFace> claimTargetProbeWindow(int maxTargets) {
        promoteStableTopology();
        if (maxTargets <= 0) {
            return Collections.emptyList();
        }

        if (sharedTopology != null) {
            ProbeWindow window = sharedTopology.claim(targetProbeCursor, maxTargets);
            targetProbeCursor = window.nextCursor;
            return window.locations;
        }

        if (targets.isEmpty()) {
            return Collections.emptyList();
        }

        List<BlockFace> locations = new ArrayList<>(targets.keySet());
        int size = locations.size();
        int count = Math.min(maxTargets, size);
        int cursor = Math.floorMod(targetProbeCursor, size);
        List<BlockFace> claimed = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            claimed.add(locations.get((cursor + i) % size));
        }

        targetProbeCursor = (cursor + count) % size;
        return claimed;
    }

    public void invalidateTarget(BlockFace location) {
        if (sharedTopology != null) {
            sharedTopology.invalidate(location);
            return;
        }

        targets.remove(location);
    }

    @Nullable public InventoryIdentifier getInventoryIdentifier(BlockFace inventoryFace) {
        BlockEntity blockEntity = level.getBlockEntity(inventoryFace.getPos());
        if (!(blockEntity instanceof IGasInventoryIdentifierProvider provider)) {
            return null;
        }
        return provider.getGasInventoryIdentifier(inventoryFace.getFace());
    }

    @Nullable public GasFlowSource refreshTarget(BlockFace location) {
        if (!isPresent(location)) {
            return null;
        }

        GasPipeConnection connection = get(location);
        if (connection == null) {
            return null;
        }

        GasFlow flow = connection.getFlow();
        if (flow == null || flow.inbound || !GasStack.isSameGasSameComponents(flow.gas, gas)) {
            return null;
        }

        if (connection.getSource() == null && !connection.determineSource(level, location.getPos())) {
            return null;
        }

        GasFlowSource source = connection.getSource();
        if (source == null || !source.isEndpoint()) {
            return null;
        }
        return source;
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
            GasPipeConnection adjacent = get(adjacentLocation);
            if (adjacent == null) {
                continue;
            }

            GasFlow outFlow = adjacent.getFlow();
            if (outFlow == null) {
                if (adjacent.hasPressure() && adjacent.getOutwardPressureUnits() > 0) {
                    keepInFrontier = true;
                }
                continue;
            }

            if (outFlow.inbound) {
                if (adjacent.getPressureDirection() > 0) {
                    keepInFrontier = true;
                }
                continue;
            }

            if (adjacent.getSource() == null && !adjacent.determineSource(level, blockFace.getPos())) {
                keepInFrontier = true;
                continue;
            }

            GasFlowSource adjacentSource = adjacent.getSource();
            if (adjacentSource != null && adjacentSource.isEndpoint()) {
                targets.put(adjacentLocation, adjacentSource);
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

        List<BlockFace> locations = List.copyOf(targets.keySet());
        TopologyKey key = new TopologyKey(gas.copyWithAmount(1), locations);
        synchronized (SHARED_TOPOLOGIES) {
            Map<TopologyKey, WeakReference<SharedTopology>> levelTopologies = SHARED_TOPOLOGIES.computeIfAbsent(level, ignored -> new HashMap<>());
            levelTopologies.entrySet().removeIf(entry -> entry.getValue().get() == null);
            WeakReference<SharedTopology> reference = levelTopologies.get(key);
            SharedTopology topology = reference == null ? null : reference.get();
            if (topology == null) {
                topology = new SharedTopology(locations);
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
        GasTransportBehaviour transfer = getGasTransfer(location.getPos());
        if (transfer == null) {
            return null;
        }
        return transfer.getConnection(location.getFace());
    }

    @Nullable
    private GasTransportBehaviour getGasTransfer(BlockPos pos) {
        WeakReference<GasTransportBehaviour> reference = cache.get(pos);
        GasTransportBehaviour cachedBehaviour = reference == null ? null : reference.get();
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

            int size = locations.size();
            int count = Math.min(maxTargets, size);
            int cursor = Math.floorMod(requestedCursor, size);
            List<BlockFace> claimed = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                claimed.add(locations.get((cursor + i) % size));
            }
            return new ProbeWindow(claimed, (cursor + count) % size);
        }

        private void invalidate(BlockFace location) {
            locations.remove(location);
        }
    }
}
